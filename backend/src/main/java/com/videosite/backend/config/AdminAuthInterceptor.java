package com.videosite.backend.config;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object sessionUser = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(AuthConstants.ADMIN_SESSION_KEY);

        if (sessionUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "管理员未登录");
        }
        return true;
    }
}
