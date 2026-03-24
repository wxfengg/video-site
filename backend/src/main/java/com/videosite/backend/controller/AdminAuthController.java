package com.videosite.backend.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.controller.dto.AdminLoginRequest;
import com.videosite.backend.controller.dto.AdminSessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @PostMapping("/login")
    public ApiResponse<AdminSessionResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                   HttpServletRequest servletRequest) {
        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(AuthConstants.ADMIN_SESSION_KEY, request.getUsername());
        return ApiResponse.success(new AdminSessionResponse(true, request.getUsername()));
    }

    @PostMapping("/logout")
    public ApiResponse<AdminSessionResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.success(new AdminSessionResponse(false, null));
    }

    @GetMapping("/me")
    public ApiResponse<AdminSessionResponse> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ApiResponse.success(new AdminSessionResponse(false, null));
        }

        Object username = session.getAttribute(AuthConstants.ADMIN_SESSION_KEY);
        if (username == null) {
            return ApiResponse.success(new AdminSessionResponse(false, null));
        }

        return ApiResponse.success(new AdminSessionResponse(true, String.valueOf(username)));
    }
}
