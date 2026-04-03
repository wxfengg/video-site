package com.videosite.backend.user.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.user.dto.UserLoginRequest;
import com.videosite.backend.user.dto.UserRegisterRequest;
import com.videosite.backend.user.dto.UserSessionResponse;
import com.videosite.backend.user.service.UserAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/register")
    public ApiResponse<UserSessionResponse> register(@Valid @RequestBody UserRegisterRequest request,
                                                     HttpServletRequest servletRequest) {
        return ApiResponse.success(userAuthService.register(request, servletRequest));
    }

    @PostMapping("/login")
    public ApiResponse<UserSessionResponse> login(@Valid @RequestBody UserLoginRequest request,
                                                  HttpServletRequest servletRequest) {
        return ApiResponse.success(userAuthService.login(request, servletRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<UserSessionResponse> logout(HttpServletRequest request) {
        return ApiResponse.success(userAuthService.logout(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserSessionResponse> me(HttpServletRequest request) {
        return ApiResponse.success(userAuthService.me(request));
    }
}
