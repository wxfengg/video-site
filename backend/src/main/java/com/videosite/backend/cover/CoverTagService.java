package com.videosite.backend.cover;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.cover.dto.CoverTagItemResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CoverTagService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, CoverTagAnalyzer> analyzers;

    public CoverTagService(JdbcTemplate jdbcTemplate,
                           List<CoverTagAnalyzer> analyzerList) {
        this.jdbcTemplate = jdbcTemplate;
        Map<String, CoverTagAnalyzer> map = new HashMap<>();
        for (CoverTagAnalyzer analyzer : analyzerList) {
            map.put(analyzer.analyzerType(), analyzer);
        }
        this.analyzers = Collections.unmodifiableMap(map);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submitTask(Long videoId, String analyzerType) {
        ensureVideoExists(videoId);
        String useType = StringUtils.hasText(analyzerType) ? analyzerType : "rule";
        if (!analyzers.containsKey(useType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的分析器类型");
        }

        Long taskId = IdWorker.getId();
        jdbcTemplate.update(
                "INSERT INTO cover_analysis_task (id, video_id, task_status, analyzer_type, result_json, error_message, created_at, updated_at) VALUES (?, ?, 'pending', ?, NULL, NULL, NOW(), NOW())",
                taskId,
                videoId,
                useType
        );
        return taskId;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processNextPendingTask() {
        List<TaskRow> rows = jdbcTemplate.query(
                "SELECT id, video_id, analyzer_type FROM cover_analysis_task WHERE task_status = 'pending' ORDER BY created_at ASC LIMIT 1",
                (rs, rowNum) -> new TaskRow(rs.getLong("id"), rs.getLong("video_id"), rs.getString("analyzer_type"))
        );

        if (rows.isEmpty()) {
            return false;
        }

        TaskRow task = rows.get(0);
        int changed = jdbcTemplate.update(
                "UPDATE cover_analysis_task SET task_status = 'running', updated_at = NOW() WHERE id = ? AND task_status = 'pending'",
                task.taskId
        );
        if (changed == 0) {
            return false;
        }

        try {
            doAnalyze(task);
            jdbcTemplate.update(
                    "UPDATE cover_analysis_task SET task_status = 'success', error_message = NULL, updated_at = NOW() WHERE id = ?",
                    task.taskId
            );
        } catch (Exception ex) {
            jdbcTemplate.update(
                    "UPDATE cover_analysis_task SET task_status = 'failed', error_message = ?, updated_at = NOW() WHERE id = ?",
                    truncateError(ex.getMessage()),
                    task.taskId
            );
        }

        return true;
    }

    public List<CoverTagItemResponse> listVideoTags(Long videoId) {
        return jdbcTemplate.query(
                "SELECT tag_name, tag_source, confidence FROM video_tag WHERE video_id = ? ORDER BY confidence DESC, updated_at DESC",
                (rs, rowNum) -> {
                    CoverTagItemResponse item = new CoverTagItemResponse();
                    item.setTagName(rs.getString("tag_name"));
                    item.setTagSource(rs.getString("tag_source"));
                    item.setConfidence(rs.getObject("confidence") == null ? null : rs.getDouble("confidence"));
                    return item;
                },
                videoId
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualCorrectTags(Long videoId, List<String> tags) {
        ensureVideoExists(videoId);
        jdbcTemplate.update("DELETE FROM video_tag WHERE video_id = ? AND tag_source = 'manual'", videoId);

        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO video_tag (id, video_id, tag_name, tag_source, confidence, created_at, updated_at) VALUES (?, ?, ?, 'manual', 1.0, NOW(), NOW())",
                    IdWorker.getId(),
                    videoId,
                    tag.trim()
            );
        }
    }

    private void doAnalyze(TaskRow task) {
        VideoMeta meta = loadVideoMeta(task.videoId);
        CoverTagAnalyzer analyzer = analyzers.get(task.analyzerType);
        if (analyzer == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到分析器");
        }

        List<CoverTagAnalyzer.TagCandidate> tags = analyzer.analyze(
                new CoverTagAnalyzer.CoverAnalyzeContext(meta.videoId, meta.title, meta.description, meta.coverUrl)
        );

        jdbcTemplate.update("DELETE FROM video_tag WHERE video_id = ? AND tag_source IN ('rule', 'ai')", meta.videoId);

        for (CoverTagAnalyzer.TagCandidate tag : tags) {
            jdbcTemplate.update(
                    "INSERT INTO video_tag (id, video_id, tag_name, tag_source, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                    IdWorker.getId(),
                    meta.videoId,
                    tag.getTagName(),
                    "rule".equals(task.analyzerType) ? "rule" : "ai",
                    tag.getConfidence()
            );
        }

        String resultJson = buildResultJson(tags);
        jdbcTemplate.update(
                "UPDATE cover_analysis_task SET result_json = ? WHERE id = ?",
                resultJson,
                task.taskId
        );
    }

    private VideoMeta loadVideoMeta(Long videoId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, title, description, cover_url FROM video WHERE id = ?",
                    (rs, rowNum) -> new VideoMeta(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("cover_url")
                    ),
                    videoId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
    }

    private void ensureVideoExists(Long videoId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM video WHERE id = ?", Integer.class, videoId);
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
    }

    private String buildResultJson(List<CoverTagAnalyzer.TagCandidate> tags) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < tags.size(); i += 1) {
            CoverTagAnalyzer.TagCandidate tag = tags.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append("{\"tag\":\"")
                    .append(tag.getTagName().replace("\"", ""))
                    .append("\",\"confidence\":")
                    .append(String.format(java.util.Locale.US, "%.4f", tag.getConfidence()))
                    .append('}');
        }
        builder.append(']');
        return builder.toString();
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "cover analysis failed";
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
        private final String coverUrl;

        private VideoMeta(Long videoId, String title, String description, String coverUrl) {
            this.videoId = videoId;
            this.title = title;
            this.description = description;
            this.coverUrl = coverUrl;
        }
    }
}
