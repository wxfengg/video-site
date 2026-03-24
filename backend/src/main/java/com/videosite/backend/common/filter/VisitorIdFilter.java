package com.videosite.backend.common.filter;

import com.videosite.backend.common.auth.AuthConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class VisitorIdFilter extends OncePerRequestFilter {

    private static final int VISITOR_COOKIE_MAX_AGE_SECONDS = 180 * 24 * 60 * 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String visitorId = resolveVisitorId(request);
        if (!StringUtils.hasText(visitorId)) {
            visitorId = generateVisitorId();
        }

        request.setAttribute(AuthConstants.VISITOR_ID_ATTR, visitorId);
        response.setHeader(AuthConstants.VISITOR_ID_HEADER, visitorId);
        writeVisitorCookie(response, visitorId, request.isSecure());

        filterChain.doFilter(request, response);
    }

    private String resolveVisitorId(HttpServletRequest request) {
        String fromHeader = request.getHeader(AuthConstants.VISITOR_ID_HEADER);
        if (StringUtils.hasText(fromHeader)) {
            return fromHeader.trim();
        }

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (AuthConstants.VISITOR_ID_COOKIE.equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }

        return null;
    }

    private String generateVisitorId() {
        String random = UUID.randomUUID().toString().replace("-", "");
        return "v_" + random;
    }

    private void writeVisitorCookie(HttpServletResponse response, String visitorId, boolean secure) {
        Cookie cookie = new Cookie(AuthConstants.VISITOR_ID_COOKIE, visitorId);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setSecure(secure);
        cookie.setMaxAge(VISITOR_COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
    }
}
