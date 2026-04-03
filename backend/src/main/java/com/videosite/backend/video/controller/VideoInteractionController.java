package com.videosite.backend.video.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.video.dto.PageResult;
import com.videosite.backend.video.dto.VideoCommentCreateRequest;
import com.videosite.backend.video.dto.VideoCommentItemResponse;
import com.videosite.backend.video.dto.VideoLikeSummaryResponse;
import com.videosite.backend.video.service.VideoInteractionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/videos")
public class VideoInteractionController {

    private final VideoInteractionService videoInteractionService;

    public VideoInteractionController(VideoInteractionService videoInteractionService) {
        this.videoInteractionService = videoInteractionService;
    }

    @PostMapping("/{videoId}/likes")
    public ApiResponse<String> addLike(@PathVariable("videoId") Long videoId,
                                       HttpServletRequest request) {
        Long userId = currentUserId(request, true);
        return ApiResponse.success(videoInteractionService.addLike(userId, videoId, currentVisitorId(request)));
    }

    @DeleteMapping("/{videoId}/likes")
    public ApiResponse<String> removeLike(@PathVariable("videoId") Long videoId,
                                          HttpServletRequest request) {
        Long userId = currentUserId(request, true);
        return ApiResponse.success(videoInteractionService.removeLike(userId, videoId, currentVisitorId(request)));
    }

    @GetMapping("/{videoId}/likes/summary")
    public ApiResponse<VideoLikeSummaryResponse> getLikeSummary(@PathVariable("videoId") Long videoId,
                                                                HttpServletRequest request) {
        Long userId = currentUserId(request, false);
        return ApiResponse.success(videoInteractionService.getLikeSummary(videoId, userId));
    }

    @GetMapping("/{videoId}/comments")
    public ApiResponse<PageResult<VideoCommentItemResponse>> listComments(@PathVariable("videoId") Long videoId,
                                                                           @RequestParam(value = "page", defaultValue = "1") int page,
                                                                           @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(videoInteractionService.listComments(videoId, page, pageSize));
    }

    @PostMapping("/{videoId}/comments")
    public ApiResponse<VideoCommentItemResponse> createComment(@PathVariable("videoId") Long videoId,
                                                                @Valid @RequestBody VideoCommentCreateRequest request,
                                                                HttpServletRequest httpServletRequest) {
        Long userId = currentUserId(httpServletRequest, true);
        return ApiResponse.success(videoInteractionService.createComment(userId, videoId, currentVisitorId(httpServletRequest), request));
    }

    @DeleteMapping("/{videoId}/comments/{commentId}")
    public ApiResponse<String> deleteComment(@PathVariable("videoId") Long videoId,
                                             @PathVariable("commentId") Long commentId,
                                             HttpServletRequest request) {
        Long userId = currentUserId(request, true);
        return ApiResponse.success(videoInteractionService.deleteComment(userId, videoId, commentId, currentVisitorId(request)));
    }

    private String currentVisitorId(HttpServletRequest request) {
        Object visitorAttr = request.getAttribute(AuthConstants.VISITOR_ID_ATTR);
        return visitorAttr == null ? "anonymous" : String.valueOf(visitorAttr);
    }

    private Long currentUserId(HttpServletRequest request, boolean required) {
        if (request.getSession(false) == null) {
            if (required) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
            }
            return null;
        }

        Object userIdAttr = request.getSession(false).getAttribute(AuthConstants.USER_SESSION_USER_ID_KEY);
        if (userIdAttr == null) {
            if (required) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
            }
            return null;
        }

        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        if (userIdAttr instanceof Integer) {
            return ((Integer) userIdAttr).longValue();
        }
        if (userIdAttr instanceof String) {
            try {
                return Long.parseLong((String) userIdAttr);
            } catch (NumberFormatException ex) {
                if (required) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户会话无效");
                }
                return null;
            }
        }

        if (required) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户会话无效");
        }
        return null;
    }
}
