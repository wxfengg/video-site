package com.videosite.backend.video.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.video.dto.UploadCompleteRequest;
import com.videosite.backend.video.dto.UploadCompleteResponse;
import com.videosite.backend.video.dto.UploadCoverResponse;
import com.videosite.backend.video.dto.UploadInitRequest;
import com.videosite.backend.video.dto.UploadInitResponse;
import com.videosite.backend.video.service.VideoUploadService;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Set;

@RestController
@RequestMapping("/api/videos/upload")
public class VideoUploadController {

    private static final long MAX_COVER_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_COVER_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final VideoUploadService videoUploadService;

    public VideoUploadController(VideoUploadService videoUploadService) {
        this.videoUploadService = videoUploadService;
    }

    @PostMapping("/init")
    public ApiResponse<UploadInitResponse> init(@Valid @RequestBody UploadInitRequest request) {
        return ApiResponse.success(videoUploadService.initUpload(request));
    }

    @PostMapping(value = "/local/{videoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadLocalFile(@PathVariable("videoId") Long videoId,
                                               @RequestParam("objectKey") String objectKey,
                                               @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail(com.videosite.backend.common.api.ErrorCode.BAD_REQUEST, "上传文件不能为空", null);
        }

        if (!StringUtils.hasText(objectKey)) {
            return ApiResponse.fail(com.videosite.backend.common.api.ErrorCode.BAD_REQUEST, "objectKey 不能为空", null);
        }

        videoUploadService.uploadLocalFile(videoId, objectKey, file);
        return ApiResponse.success("uploaded");
    }

    @PostMapping(value = "/cover/{videoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadCoverResponse> uploadCover(@PathVariable("videoId") Long videoId,
                                                         @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail(com.videosite.backend.common.api.ErrorCode.BAD_REQUEST, "封面文件不能为空", null);
        }

        if (file.getSize() > MAX_COVER_SIZE_BYTES) {
            return ApiResponse.fail(com.videosite.backend.common.api.ErrorCode.BAD_REQUEST, "封面图片不能超过 5MB", null);
        }

        if (!isAllowedCoverFile(file)) {
            return ApiResponse.fail(com.videosite.backend.common.api.ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式", null);
        }

        return ApiResponse.success(videoUploadService.uploadCover(videoId, file));
    }

    private boolean isAllowedCoverFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            return ALLOWED_COVER_MIME_TYPES.contains(contentType.toLowerCase());
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            return false;
        }
        String lowerName = originalFilename.toLowerCase();
        return lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp");
    }

    @PostMapping("/complete")
    public ApiResponse<UploadCompleteResponse> complete(@Valid @RequestBody UploadCompleteRequest request) {
        return ApiResponse.success(videoUploadService.completeUpload(request));
    }
}
