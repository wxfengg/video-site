package com.videosite.backend.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.user.dto.UserLoginRequest;
import com.videosite.backend.user.dto.UserRegisterRequest;
import com.videosite.backend.user.dto.UserSessionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;

@Service
public class UserAuthService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserAuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSessionResponse register(UserRegisterRequest request, HttpServletRequest servletRequest) {
        String normalizedUsername = normalizeUsername(request.getUsername());

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_user WHERE username = ?",
                Integer.class,
                normalizedUsername
        );

        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        Long userId = IdWorker.getId();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        jdbcTemplate.update(
                "INSERT INTO app_user (id, username, password_hash, status, last_login_at, created_at, updated_at) VALUES (?, ?, ?, 1, NOW(), NOW(), NOW())",
                userId,
                normalizedUsername,
                passwordHash
        );

        bindSession(servletRequest, userId, normalizedUsername);
        return new UserSessionResponse(true, userId, normalizedUsername);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSessionResponse login(UserLoginRequest request, HttpServletRequest servletRequest) {
        String normalizedUsername = normalizeUsername(request.getUsername());

        List<UserRow> users = jdbcTemplate.query(
                "SELECT id, username, password_hash, status FROM app_user WHERE username = ? LIMIT 1",
                (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getInt("status")
                ),
                normalizedUsername
        );

        if (users.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        UserRow user = users.get(0);
        if (user.status != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.passwordHash)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        jdbcTemplate.update("UPDATE app_user SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?", user.id);
        bindSession(servletRequest, user.id, user.username);

        return new UserSessionResponse(true, user.id, user.username);
    }

    public UserSessionResponse logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new UserSessionResponse(false, null, null);
    }

    public UserSessionResponse me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new UserSessionResponse(false, null, null);
        }

        Object userIdAttr = session.getAttribute(AuthConstants.USER_SESSION_USER_ID_KEY);
        Object usernameAttr = session.getAttribute(AuthConstants.USER_SESSION_USERNAME_KEY);
        if (!(userIdAttr instanceof Long) || usernameAttr == null) {
            return new UserSessionResponse(false, null, null);
        }

        return new UserSessionResponse(true, (Long) userIdAttr, String.valueOf(usernameAttr));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private void bindSession(HttpServletRequest servletRequest, Long userId, String username) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(AuthConstants.USER_SESSION_USER_ID_KEY, userId);
        session.setAttribute(AuthConstants.USER_SESSION_USERNAME_KEY, username);
    }

    private static class UserRow {
        private final Long id;
        private final String username;
        private final String passwordHash;
        private final int status;

        private UserRow(Long id, String username, String passwordHash, int status) {
            this.id = id;
            this.username = username;
            this.passwordHash = passwordHash;
            this.status = status;
        }
    }
}
