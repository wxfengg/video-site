package com.videosite.backend.user.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.user.dto.AdminUserCreateRequest;
import com.videosite.backend.user.dto.AdminUserListItemResponse;
import com.videosite.backend.user.dto.AdminUserStatusUpdateRequest;
import com.videosite.backend.user.service.AdminUserService;
import com.videosite.backend.video.dto.PageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/api/admin/users")
    public ApiResponse<PageResult<AdminUserListItemResponse>> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(adminUserService.listUsers(page, pageSize, keyword));
    }

    @PostMapping("/api/admin/users")
    public ApiResponse<AdminUserListItemResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(adminUserService.createUser(request));
    }

    @PatchMapping("/api/admin/users/{userId}/status")
    public ApiResponse<AdminUserListItemResponse> updateUserStatus(@PathVariable("userId") Long userId,
                                                                   @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        return ApiResponse.success(adminUserService.updateUserStatus(userId, request.getStatus()));
    }

    @DeleteMapping("/api/admin/users/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable("userId") Long userId) {
        return ApiResponse.success(adminUserService.deleteUser(userId));
    }
}