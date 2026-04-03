package com.videosite.backend.user.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.user.dto.UserFavoriteItemResponse;
import com.videosite.backend.user.dto.UserWatchHistoryItemResponse;
import com.videosite.backend.user.dto.UserWatchProgressRequest;
import com.videosite.backend.user.dto.UserWatchProgressResponse;
import com.videosite.backend.user.service.UserCenterService;
import com.videosite.backend.video.dto.PageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/users/me")
public class UserCenterController {

    private final UserCenterService userCenterService;

    public UserCenterController(UserCenterService userCenterService) {
        this.userCenterService = userCenterService;
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<UserFavoriteItemResponse>> listFavorites(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(userCenterService.listFavorites(userId, page, pageSize));
    }

    @PostMapping("/favorites/{videoId}")
    public ApiResponse<String> addFavorite(@PathVariable("videoId") Long videoId,
                                           HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(userCenterService.addFavorite(userId, videoId));
    }

    @DeleteMapping("/favorites/{videoId}")
    public ApiResponse<String> removeFavorite(@PathVariable("videoId") Long videoId,
                                              HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(userCenterService.removeFavorite(userId, videoId));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<UserWatchHistoryItemResponse>> listHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(userCenterService.listHistory(userId, page, pageSize));
    }

    @GetMapping("/history/{videoId}/progress")
    public ApiResponse<UserWatchProgressResponse> getProgress(@PathVariable("videoId") Long videoId,
                                                              HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(userCenterService.getWatchProgress(userId, videoId));
    }

    @PutMapping("/history/{videoId}/progress")
    public ApiResponse<UserWatchProgressResponse> updateProgress(@PathVariable("videoId") Long videoId,
                                                                 @Valid @RequestBody UserWatchProgressRequest progressRequest,
                                                                 HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(userCenterService.updateWatchProgress(userId, videoId, progressRequest));
    }

    private Long currentUserId(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Object userIdAttr = request.getSession(false).getAttribute(AuthConstants.USER_SESSION_USER_ID_KEY);
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
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户会话无效");
            }
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
    }
}
