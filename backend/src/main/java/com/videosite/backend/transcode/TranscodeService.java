package com.videosite.backend.transcode;

import com.videosite.backend.intelligence.IntelligenceProperties;
import com.videosite.backend.intelligence.VideoIntelligenceService;
import com.videosite.backend.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TranscodeService {

    private static final Logger log = LoggerFactory.getLogger(TranscodeService.class);

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final FfmpegCommandBuilder ffmpegCommandBuilder;
    private final VideoIntelligenceService videoIntelligenceService;
    private final IntelligenceProperties intelligenceProperties;

    @Value("${app.transcode.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${app.transcode.max-retry:3}")
    private int maxRetry;

    @Value("${app.storage.local-root:../}")
    private String localStorageRoot;

    @Value("${app.cover-auto.enabled:true}")
    private boolean autoCoverEnabled;

    @Value("${app.cover-auto.snapshot-second:3}")
    private int autoCoverSnapshotSecond;

    public TranscodeService(JdbcTemplate jdbcTemplate,
                            StorageService storageService,
                            FfmpegCommandBuilder ffmpegCommandBuilder,
                            VideoIntelligenceService videoIntelligenceService,
                            IntelligenceProperties intelligenceProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.ffmpegCommandBuilder = ffmpegCommandBuilder;
        this.videoIntelligenceService = videoIntelligenceService;
        this.intelligenceProperties = intelligenceProperties;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processNextPendingTask() {
        VideoTranscodeTask task = findNextPendingTask();
        if (task == null) {
            return false;
        }

        int changed = jdbcTemplate.update(
                "UPDATE video_transcode_task SET task_status = ?, started_at = NOW(), updated_at = NOW() WHERE id = ? AND task_status = ?",
                TranscodeTaskStatus.RUNNING.getDbValue(),
                task.getId(),
                TranscodeTaskStatus.PENDING.getDbValue()
        );
        if (changed == 0) {
            return false;
        }

        try {
            executeTask(task);
            markTaskSuccess(task);
            try {
                if (intelligenceProperties.isEnabled()) {
                    videoIntelligenceService.submitTask(task.getVideoId());
                }
            } catch (Exception intelEx) {
                log.warn("转码成功后提交智能分析任务失败: videoId={}, error={}", task.getVideoId(), intelEx.getMessage());
            }
        } catch (Exception ex) {
            markTaskFailure(task, ex);
        }

        return true;
    }

    private VideoTranscodeTask findNextPendingTask() {
        List<VideoTranscodeTask> tasks = jdbcTemplate.query(
                "SELECT id, video_id, retry_count FROM video_transcode_task WHERE task_status = ? ORDER BY created_at ASC LIMIT 1",
                (rs, rowNum) -> new VideoTranscodeTask(rs.getLong("id"), rs.getLong("video_id"), rs.getInt("retry_count")),
                TranscodeTaskStatus.PENDING.getDbValue()
        );
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private void executeTask(VideoTranscodeTask task) throws Exception {
        String sourceObjectKey = findSourceObjectKey(task.getVideoId());
        Path sourcePath = resolveLocalPathByObjectKey(sourceObjectKey);
        if (!Files.exists(sourcePath)) {
            throw new IllegalStateException("源视频不存在: " + sourcePath);
        }

        String baseObjectKey = "videos/transcoded/" + task.getVideoId();
        List<VariantRecord> variants = new ArrayList<>();

        for (TranscodeProfile profile : TranscodeProfile.defaults()) {
            String mp4ObjectKey = baseObjectKey + "/mp4_" + profile.getHeight() + "/video.mp4";
            String hlsObjectKey = baseObjectKey + "/hls_" + profile.getHeight() + "/index.m3u8";
            String segmentObjectPattern = baseObjectKey + "/hls_" + profile.getHeight() + "/seg_%03d.ts";

            Path mp4Path = resolveLocalPathByObjectKey(mp4ObjectKey);
            Path hlsPath = resolveLocalPathByObjectKey(hlsObjectKey);
            Path segmentPatternPath = resolveLocalPathByObjectKey(segmentObjectPattern);

            Files.createDirectories(mp4Path.getParent());
            Files.createDirectories(hlsPath.getParent());

            runCommand(ffmpegCommandBuilder.buildMp4Command(ffmpegBin, sourcePath, mp4Path, profile));
            runCommand(ffmpegCommandBuilder.buildHlsCommand(ffmpegBin, sourcePath, hlsPath, segmentPatternPath, profile));

            variants.add(new VariantRecord(profile, mp4ObjectKey, hlsObjectKey));
        }

        String masterObjectKey = baseObjectKey + "/master.m3u8";
        writeMasterPlaylist(masterObjectKey, variants);

        writeVariantAndPlaySource(task.getVideoId(), masterObjectKey, variants);
        tryGenerateAutoFrameCover(task.getVideoId(), sourcePath);
    }

    private String findSourceObjectKey(Long videoId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT object_key FROM video_file WHERE video_id = ? ORDER BY created_at DESC LIMIT 1",
                    String.class,
                    videoId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("未找到源视频文件记录，videoId=" + videoId, ex);
        }
    }

    private Path resolveLocalPathByObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey 不能为空");
        }

        String normalizedKey = objectKey.replace("\\", "/");
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }

        Path rootPath = Paths.get(localStorageRoot).toAbsolutePath().normalize();
        Path resolved = rootPath.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IllegalArgumentException("非法 objectKey: " + objectKey);
        }
        return resolved;
    }

    private void writeMasterPlaylist(String masterObjectKey, List<VariantRecord> variants) throws IOException {
        Path masterPath = resolveLocalPathByObjectKey(masterObjectKey);
        Files.createDirectories(masterPath.getParent());

        StringBuilder builder = new StringBuilder();
        builder.append("#EXTM3U\n");
        builder.append("#EXT-X-VERSION:3\n");
        for (VariantRecord variant : variants) {
            int bandwidth = variant.profile.getBitrateKbps() * 1000;
            builder.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bandwidth)
                    .append(",RESOLUTION=")
                    .append(variant.profile.getWidth())
                    .append("x")
                    .append(variant.profile.getHeight())
                    .append("\n");
            builder.append("hls_")
                    .append(variant.profile.getHeight())
                    .append("/index.m3u8\n");
        }

        Files.write(masterPath, builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(rollbackFor = Exception.class)
    protected void writeVariantAndPlaySource(Long videoId, String masterObjectKey, List<VariantRecord> variants) {
        for (VariantRecord item : variants) {
            upsertVariant(videoId, item.profile.getHeight(), item.profile.getBitrateKbps(), "mp4", item.mp4ObjectKey);
            upsertVariant(videoId, item.profile.getHeight(), item.profile.getBitrateKbps(), "hls", item.hlsObjectKey);

            upsertPlaySource(videoId, "mp4_" + item.profile.getHeight(), storageService.getUploadUrl(item.mp4ObjectKey));
        }

        upsertPlaySource(videoId, "hls_master", storageService.getUploadUrl(masterObjectKey));
    }

    private void upsertVariant(Long videoId, int resolution, int bitrateKbps, String format, String objectKey) {
        long variantId = Math.abs((videoId + "_" + resolution + "_" + format).hashCode()) + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO video_variant (id, video_id, resolution, bitrate_kbps, format, storage_provider, object_key, file_size, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE bitrate_kbps = VALUES(bitrate_kbps), storage_provider = VALUES(storage_provider), object_key = VALUES(object_key), updated_at = NOW()",
                variantId,
                videoId,
                resolution,
                bitrateKbps,
                format,
                storageService.activeProviderName(),
                objectKey
        );
    }

    private void upsertPlaySource(Long videoId, String sourceType, String playUrl) {
        long sourceId = Math.abs((videoId + "_" + sourceType).hashCode()) + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO video_play_source (id, video_id, source_type, play_url, expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NULL, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE play_url = VALUES(play_url), expires_at = VALUES(expires_at), updated_at = NOW()",
                sourceId,
                videoId,
                sourceType,
                playUrl
        );
    }

    private void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder logs = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.append(line).append('\n');
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("FFmpeg 执行失败，exitCode=" + exitCode + "，日志：" + logs);
        }
    }

    private void tryGenerateAutoFrameCover(Long videoId, Path sourcePath) {
        if (!autoCoverEnabled || sourcePath == null || !Files.exists(sourcePath) || hasCoverUrl(videoId)) {
            return;
        }

        String objectKey = buildAutoFrameObjectKey(videoId);
        Path outputPath = resolveLocalPathByObjectKey(objectKey);

        try {
            Files.createDirectories(outputPath.getParent());
            runCommand(ffmpegCommandBuilder.buildSnapshotCommand(ffmpegBin, sourcePath, outputPath, autoCoverSnapshotSecond));

            if (!Files.exists(outputPath) || Files.size(outputPath) <= 0) {
                log.warn("Auto frame cover was not generated, videoId={}, outputPath={}", videoId, outputPath);
                return;
            }

            String coverUrl = storageService.getUploadUrl(objectKey);
            int changed = jdbcTemplate.update(
                    "UPDATE video SET cover_url = ?, updated_at = NOW() WHERE id = ? AND (cover_url IS NULL OR cover_url = '')",
                    coverUrl,
                    videoId
            );

            if (changed > 0) {
                log.info("Auto frame cover generated for videoId={}, objectKey={}", videoId, objectKey);
            }
        } catch (Exception ex) {
            log.warn("Generate auto frame cover failed for videoId={}, message={}", videoId, ex.getMessage());
        }
    }

    private boolean hasCoverUrl(Long videoId) {
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT cover_url FROM video WHERE id = ? LIMIT 1",
                    (rs, rowNum) -> rs.getString("cover_url"),
                    videoId
            );

            if (rows.isEmpty()) {
                return false;
            }
            return StringUtils.hasText(rows.get(0));
        } catch (DataAccessException ex) {
            log.warn("Check cover url failed for videoId={}, message={}", videoId, ex.getMessage());
            return false;
        }
    }

    private String buildAutoFrameObjectKey(Long videoId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "images/covers/" + datePart + "/" + videoId + "_auto_frame.jpg";
    }

    @Transactional(rollbackFor = Exception.class)
    protected void markTaskSuccess(VideoTranscodeTask task) {
        jdbcTemplate.update(
                "UPDATE video_transcode_task SET task_status = ?, error_message = NULL, finished_at = NOW(), updated_at = NOW() WHERE id = ?",
                TranscodeTaskStatus.SUCCESS.getDbValue(),
                task.getId()
        );

        jdbcTemplate.update(
                "UPDATE video SET status = 'ready', updated_at = NOW() WHERE id = ?",
                task.getVideoId()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    protected void markTaskFailure(VideoTranscodeTask task, Exception ex) {
        int nextRetry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        String status = nextRetry >= maxRetry
                ? TranscodeTaskStatus.FAILED.getDbValue()
                : TranscodeTaskStatus.PENDING.getDbValue();

        if (TranscodeTaskStatus.FAILED.getDbValue().equals(status)) {
            jdbcTemplate.update(
                    "UPDATE video_transcode_task SET task_status = ?, retry_count = ?, error_message = ?, finished_at = NOW(), updated_at = NOW() WHERE id = ?",
                    status,
                    nextRetry,
                    truncateError(ex.getMessage()),
                    task.getId()
            );

            jdbcTemplate.update(
                    "UPDATE video SET status = 'offline', updated_at = NOW() WHERE id = ?",
                    task.getVideoId()
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE video_transcode_task SET task_status = ?, retry_count = ?, error_message = ?, started_at = NULL, finished_at = NULL, updated_at = NOW() WHERE id = ?",
                    status,
                    nextRetry,
                    truncateError(ex.getMessage()),
                    task.getId()
            );
        }
    }

    private String truncateError(String message) {
        if (message == null) {
            return "unknown transcode error at " + LocalDateTime.now();
        }
        if (message.length() <= 900) {
            return message;
        }
        return message.substring(0, 900);
    }

    private static class VariantRecord {
        private final TranscodeProfile profile;
        private final String mp4ObjectKey;
        private final String hlsObjectKey;

        private VariantRecord(TranscodeProfile profile, String mp4ObjectKey, String hlsObjectKey) {
            this.profile = profile;
            this.mp4ObjectKey = mp4ObjectKey;
            this.hlsObjectKey = hlsObjectKey;
        }
    }
}
