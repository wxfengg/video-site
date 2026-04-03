package com.videosite.backend.video.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.video.dto.PageResult;
import com.videosite.backend.video.dto.ExternalVideoCreateRequest;
import com.videosite.backend.video.dto.VideoDetailResponse;
import com.videosite.backend.video.dto.VideoListItemResponse;
import com.videosite.backend.video.dto.VideoPlaySourcesResponse;
import com.videosite.backend.video.dto.VideoUpdateRequest;
import com.videosite.backend.video.service.VideoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/api/videos")
    public ApiResponse<PageResult<VideoListItemResponse>> listPublicVideos(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(videoService.listPublicVideos(page, pageSize, keyword));
    }

    @GetMapping("/api/videos/{videoId}")
    public ApiResponse<VideoDetailResponse> getPublicVideo(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(videoService.getVideoDetail(videoId, false));
    }

    @GetMapping("/api/videos/{videoId}/play-sources")
    public ApiResponse<VideoPlaySourcesResponse> getPlaySources(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(videoService.getVideoPlaySources(videoId));
    }

    @GetMapping("/api/admin/videos")
    public ApiResponse<PageResult<VideoListItemResponse>> listAdminVideos(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(videoService.listAdminVideos(page, pageSize, status, keyword));
    }

    @GetMapping("/api/admin/videos/{videoId}")
    public ApiResponse<VideoDetailResponse> getAdminVideo(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(videoService.getVideoDetail(videoId, true));
    }

    @PatchMapping("/api/admin/videos/{videoId}")
    public ApiResponse<VideoDetailResponse> updateVideo(@PathVariable("videoId") Long videoId,
                                                         @Valid @RequestBody VideoUpdateRequest request) {
        return ApiResponse.success(videoService.updateVideo(videoId, request));
    }

    @PostMapping("/api/admin/videos/{videoId}/publish")
    public ApiResponse<VideoDetailResponse> publishVideo(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(videoService.publishVideo(videoId));
    }

    @PostMapping("/api/admin/videos/{videoId}/unpublish")
    public ApiResponse<VideoDetailResponse> unpublishVideo(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(videoService.unpublishVideo(videoId));
    }

    @DeleteMapping("/api/admin/videos/{videoId}")
    public ApiResponse<String> deleteVideo(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(videoService.deleteVideo(videoId));
    }

    @PostMapping("/api/admin/videos/external")
    public ApiResponse<VideoDetailResponse> createExternalVideo(@Valid @RequestBody ExternalVideoCreateRequest request) {
        return ApiResponse.success(videoService.createExternalVideo(request));
    }
}
