package com.videosite.backend.video.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.storage.StorageService;
import com.videosite.backend.video.dto.UploadCompleteRequest;
import com.videosite.backend.video.dto.UploadCompleteResponse;
import com.videosite.backend.video.dto.UploadCoverResponse;
import com.videosite.backend.video.dto.UploadInitRequest;
import com.videosite.backend.video.dto.UploadInitResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
public class VideoUploadService {

    private static final long MAX_COVER_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_COVER_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;

    public VideoUploadService(JdbcTemplate jdbcTemplate, StorageService storageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadInitResponse initUpload(UploadInitRequest request) {
        Long videoId = IdWorker.getId();
        String objectKey = buildObjectKey(videoId, request.getFileName());

        jdbcTemplate.update(
                "INSERT INTO video (id, title, description, status, created_by, created_at, updated_at) VALUES (?, ?, ?, 'draft', NULL, NOW(), NOW())",
                videoId,
                request.getTitle(),
                request.getDescription()
        );

        String uploadUrl = "/api/videos/upload/local/" + videoId + "?objectKey=" + objectKey;
        return new UploadInitResponse(videoId, storageService.activeProviderName(), objectKey, uploadUrl);
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadCompleteResponse completeUpload(UploadCompleteRequest request) {
        Integer videoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM video WHERE id = ?",
                Integer.class,
                request.getVideoId()
        );

        if (videoCount == null || videoCount <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }

        if (!storageService.exists(request.getObjectKey())) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "上传文件不存在，请先完成文件上传");
        }

        Long videoFileId = IdWorker.getId();
        jdbcTemplate.update(
                "INSERT INTO video_file (id, video_id, storage_provider, object_key, file_size, mime_type, checksum_sha256, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                videoFileId,
                request.getVideoId(),
                storageService.activeProviderName(),
                request.getObjectKey(),
                request.getFileSize(),
                request.getMimeType(),
                emptyToNull(request.getChecksumSha256())
        );

        Long transcodeTaskId = IdWorker.getId();
        jdbcTemplate.update(
                "INSERT INTO video_transcode_task (id, video_id, task_status, retry_count, created_at, updated_at) VALUES (?, ?, 'pending', 0, NOW(), NOW())",
                transcodeTaskId,
                request.getVideoId()
        );

        jdbcTemplate.update(
                "UPDATE video SET status = 'transcoding', updated_at = NOW() WHERE id = ?",
                request.getVideoId()
        );

        return new UploadCompleteResponse(request.getVideoId(), videoFileId, transcodeTaskId, "transcoding");
    }

    public void uploadLocalFile(Long videoId, String objectKey, MultipartFile file) {
        if (videoId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "videoId 不能为空");
        }

        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "objectKey 不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            storageService.put(objectKey, inputStream, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "写入本地存储失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadCoverResponse uploadCover(Long videoId, MultipartFile file) {
        if (videoId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "videoId 不能为空");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面文件不能为空");
        }

        validateCoverFile(file);

        if (!existsVideo(videoId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }

        String objectKey = buildCoverObjectKey(videoId, file.getOriginalFilename(), file.getContentType());
        try (InputStream inputStream = file.getInputStream()) {
            storageService.put(objectKey, inputStream, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "封面上传失败");
        }

        String coverUrl = storageService.getUploadUrl(objectKey);
        jdbcTemplate.update(
                "UPDATE video SET cover_url = ?, updated_at = NOW() WHERE id = ?",
                coverUrl,
                videoId
        );

        return new UploadCoverResponse(objectKey, coverUrl);
    }

    private String buildObjectKey(Long videoId, String fileName) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = extractExtension(fileName);
        return "videos/raw/" + datePart + "/" + videoId + ext;
    }

    private String buildCoverObjectKey(Long videoId, String fileName, String contentType) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = extractImageExtension(fileName, contentType);
        return "images/covers/" + datePart + "/" + videoId + "_" + IdWorker.getId() + ext;
    }

    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".mp4";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".mp4";
        }
        return fileName.substring(dotIndex);
    }

    private String extractImageExtension(String fileName, String contentType) {
        if (StringUtils.hasText(contentType)) {
            if ("image/png".equalsIgnoreCase(contentType)) {
                return ".png";
            }
            if ("image/webp".equalsIgnoreCase(contentType)) {
                return ".webp";
            }
            if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
                return ".jpg";
            }
        }

        if (StringUtils.hasText(fileName)) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".png")) {
                return ".png";
            }
            if (lowerName.endsWith(".webp")) {
                return ".webp";
            }
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                return ".jpg";
            }

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                return ".jpg";
            }
        }

        return ".jpg";
    }

    private void validateCoverFile(MultipartFile file) {
        if (file.getSize() > MAX_COVER_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面图片不能超过 5MB");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            if (!ALLOWED_COVER_MIME_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式");
            }
            return;
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式");
        }

        String lowerName = originalFilename.toLowerCase();
        boolean ok = lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp");
        if (!ok) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式");
        }
    }

    private boolean existsVideo(Long videoId) {
        Integer videoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM video WHERE id = ?",
                Integer.class,
                videoId
        );
        return videoCount != null && videoCount > 0;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
