package com.videosite.backend.intelligence;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.intelligence.analysis.AnalyzeContext;
import com.videosite.backend.intelligence.analysis.IntelligenceResult;
import com.videosite.backend.intelligence.analysis.VideoIntelligenceAnalyzer;
import com.videosite.backend.intelligence.audio.AudioTranscriptService;
import com.videosite.backend.intelligence.audio.ExternalWhisperService;
import com.videosite.backend.intelligence.audio.NoOpTranscriptService;
import com.videosite.backend.intelligence.frame.KeyframeExtractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class VideoIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(VideoIntelligenceService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ExternalWhisperService externalWhisperService;
    private final NoOpTranscriptService noOpTranscriptService;
    private final KeyframeExtractService keyframeExtractService;
    private final Map<String, VideoIntelligenceAnalyzer> analyzers;
    private final IntelligenceProperties properties;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.local-root:../}")
    private String localStorageRoot;

    public VideoIntelligenceService(JdbcTemplate jdbcTemplate,
                                    ExternalWhisperService externalWhisperService,
                                    NoOpTranscriptService noOpTranscriptService,
                                    KeyframeExtractService keyframeExtractService,
                                    List<VideoIntelligenceAnalyzer> analyzerList,
                                    IntelligenceProperties properties,
                                    ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.externalWhisperService = externalWhisperService;
        this.noOpTranscriptService = noOpTranscriptService;
        this.keyframeExtractService = keyframeExtractService;
        this.properties = properties;
        this.objectMapper = objectMapper;

        Map<String, VideoIntelligenceAnalyzer> map = new java.util.HashMap<>();
        for (VideoIntelligenceAnalyzer analyzer : analyzerList) {
            map.put(analyzer.analyzerType(), analyzer);
        }
        this.analyzers = Collections.unmodifiableMap(map);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submitTask(Long videoId) {
        if (!properties.isEnabled()) {
            return null;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM video_intelligence_task WHERE video_id = ? AND task_status IN ('pending','running')",
                Integer.class, videoId
        );
        if (count != null && count > 0) {
            log.info("视频 {} 已有待处理或运行中的智能分析任务，跳过提交", videoId);
            return null;
        }

        Long taskId = IdWorker.getId();
        jdbcTemplate.update(
                "INSERT INTO video_intelligence_task (id, video_id, task_status, analyzer_type, result_json, error_message, created_at, updated_at) VALUES (?, ?, 'pending', ?, NULL, NULL, NOW(), NOW())",
                taskId, videoId, properties.getAudio().getProvider()
        );
        log.info("提交视频智能分析任务: videoId={}, taskId={}", videoId, taskId);
        return taskId;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processNextPendingTask() {
        List<TaskRow> rows = jdbcTemplate.query(
                "SELECT id, video_id, analyzer_type FROM video_intelligence_task WHERE task_status = 'pending' ORDER BY created_at ASC LIMIT 1",
                (rs, rowNum) -> new TaskRow(rs.getLong("id"), rs.getLong("video_id"), rs.getString("analyzer_type"))
        );

        if (rows.isEmpty()) {
            return false;
        }

        TaskRow task = rows.get(0);
        int changed = jdbcTemplate.update(
                "UPDATE video_intelligence_task SET task_status = 'running', updated_at = NOW() WHERE id = ? AND task_status = 'pending'",
                task.taskId
        );
        if (changed == 0) {
            return false;
        }

        try {
            doAnalyze(task);
            jdbcTemplate.update(
                    "UPDATE video_intelligence_task SET task_status = 'success', error_message = NULL, updated_at = NOW() WHERE id = ?",
                    task.taskId
            );
        } catch (Exception ex) {
            log.error("智能分析任务失败: taskId={}, videoId={}", task.taskId, task.videoId, ex);
            jdbcTemplate.update(
                    "UPDATE video_intelligence_task SET task_status = 'failed', error_message = ?, updated_at = NOW() WHERE id = ?",
                    truncateError(ex.getMessage()),
                    task.taskId
            );
        }

        return true;
    }

    private void doAnalyze(TaskRow task) throws Exception {
        VideoMeta meta = loadVideoMeta(task.videoId);
        String videoFilePath = resolveVideoFilePath(task.videoId);

        if (!StringUtils.hasText(videoFilePath) || !Files.exists(Paths.get(videoFilePath))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频文件不存在，无法分析");
        }

        // 1. 音频转录
        String audioTranscript = "";
        try {
            AudioTranscriptService audioService = resolveAudioService();
            audioTranscript = audioService.transcript(videoFilePath);
            if (StringUtils.hasText(audioTranscript)) {
                jdbcTemplate.update(
                        "UPDATE video_intelligence_task SET audio_transcript = ? WHERE id = ?",
                        audioTranscript, task.taskId
                );
            }
        } catch (Exception ex) {
            log.warn("音频转录失败，降级为仅使用标题和描述: videoId={}, error={}", task.videoId, ex.getMessage());
        }

        // 2. 截取关键帧
        List<String> keyframePaths = keyframeExtractService.extractKeyframes(videoFilePath, task.videoId);

        // 3. 调用 AI 分析
        VideoIntelligenceAnalyzer analyzer = analyzers.getOrDefault("kimi", null);
        if (analyzer == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未找到 AI 分析器");
        }

        AnalyzeContext context = new AnalyzeContext(
                task.videoId,
                meta.title,
                meta.description,
                audioTranscript,
                keyframePaths
        );

        IntelligenceResult result = analyzer.analyze(context);

        // 4. 保存结果
        saveIntelligenceResult(task.videoId, result);

        // 5. 保存 result_json 到任务表
        String resultJson = objectMapper.writeValueAsString(result);
        jdbcTemplate.update(
                "UPDATE video_intelligence_task SET result_json = ? WHERE id = ?",
                resultJson, task.taskId
        );

        log.info("视频智能分析完成: videoId={}, tags={}", task.videoId, result.getTags());
    }

    private AudioTranscriptService resolveAudioService() {
        return "whisper".equals(properties.getAudio().getProvider())
                ? externalWhisperService
                : noOpTranscriptService;
    }

    private void saveIntelligenceResult(Long videoId, IntelligenceResult result) {
        String tagsJson;
        String categoriesJson;
        try {
            tagsJson = result.getTags() == null ? "[]" : objectMapper.writeValueAsString(result.getTags());
            categoriesJson = result.getCategories() == null ? "[]" : objectMapper.writeValueAsString(result.getCategories());
        } catch (Exception ex) {
            tagsJson = "[]";
            categoriesJson = "[]";
        }

        jdbcTemplate.update(
                "DELETE FROM video_intelligence WHERE video_id = ?",
                videoId
        );

        jdbcTemplate.update(
                "INSERT INTO video_intelligence (id, video_id, summary, tags_json, categories_json, sentiment, audience, keywords, embedding_text, model_version, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                IdWorker.getId(),
                videoId,
                result.getSummary(),
                tagsJson,
                categoriesJson,
                result.getSentiment(),
                result.getAudience(),
                result.getKeywords(),
                result.getEmbeddingText(),
                "kimi-" + properties.getAudio().getProvider()
        );
    }

    private VideoMeta loadVideoMeta(Long videoId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, title, description FROM video WHERE id = ?",
                    (rs, rowNum) -> new VideoMeta(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("description")
                    ),
                    videoId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
    }

    private String resolveVideoFilePath(Long videoId) {
        try {
            String objectKey = jdbcTemplate.queryForObject(
                    "SELECT object_key FROM video_file WHERE video_id = ? ORDER BY created_at DESC LIMIT 1",
                    String.class,
                    videoId
            );
            if (!StringUtils.hasText(objectKey)) {
                return null;
            }
            String normalizedKey = objectKey.replace("\\", "/");
            while (normalizedKey.startsWith("/")) {
                normalizedKey = normalizedKey.substring(1);
            }
            Path rootPath = Paths.get(localStorageRoot).toAbsolutePath().normalize();
            Path resolved = rootPath.resolve(normalizedKey).normalize();
            if (!resolved.startsWith(rootPath)) {
                return null;
            }
            return resolved.toString();
        } catch (Exception ex) {
            log.warn("解析视频文件路径失败: videoId={}", videoId, ex);
            return null;
        }
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "intelligence analysis failed";
        }
        return message.length() <= 900 ? message : message.substring(0, 900);
    }

    private static class TaskRow {
        private final Long taskId;
        private final Long videoId;
        private final String analyzerType;

        private TaskRow(Long taskId, Long videoId, String analyzerType) {
            this.taskId = taskId;
            this.videoId = videoId;
            this.analyzerType = analyzerType;
        }
    }

    private static class VideoMeta {
        private final Long videoId;
        private final String title;
        private final String description;

        private VideoMeta(Long videoId, String title, String description) {
            this.videoId = videoId;
            this.title = title;
            this.description = description;
        }
    }
}
