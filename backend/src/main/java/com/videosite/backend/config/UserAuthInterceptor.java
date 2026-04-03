package com.videosite.backend.config;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object sessionUserId = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(AuthConstants.USER_SESSION_USER_ID_KEY);

        if (sessionUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        return true;
    }
}
