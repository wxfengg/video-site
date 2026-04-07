package com.videosite.backend.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.user.dto.AdminUserCreateRequest;
import com.videosite.backend.user.dto.AdminUserListItemResponse;
import com.videosite.backend.video.dto.PageResult;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<AdminUserListItemResponse> listUsers(int page, int pageSize, String keyword) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        int offset = (safePage - 1) * safePageSize;

        StringBuilder countSql = new StringBuilder("SELECT COUNT(1) FROM app_user WHERE 1=1");
        StringBuilder querySql = new StringBuilder(
                "SELECT id, username, status, last_login_at, created_at, updated_at FROM app_user WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            countSql.append(" AND username LIKE ?");
            querySql.append(" AND username LIKE ?");
            params.add(like);
        }

        querySql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(safePageSize);
        queryParams.add(offset);

        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());
        List<AdminUserListItemResponse> records = jdbcTemplate.query(
                querySql.toString(),
                (rs, rowNum) -> {
                    AdminUserListItemResponse item = new AdminUserListItemResponse();
                    item.setId(rs.getLong("id"));
                    item.setUsername(rs.getString("username"));
                    item.setStatus((Integer) rs.getObject("status"));
                    item.setLastLoginAt(rs.getTimestamp("last_login_at") == null
                            ? null
                            : rs.getTimestamp("last_login_at").toLocalDateTime());
                    item.setCreatedAt(rs.getTimestamp("created_at") == null
                            ? null
                            : rs.getTimestamp("created_at").toLocalDateTime());
                    item.setUpdatedAt(rs.getTimestamp("updated_at") == null
                            ? null
                            : rs.getTimestamp("updated_at").toLocalDateTime());
                    return item;
                },
                queryParams.toArray()
        );

        return new PageResult<>(total == null ? 0 : total, safePage, safePageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserListItemResponse createUser(AdminUserCreateRequest request) {
        String normalizedUsername = normalizeUsername(request.getUsername());

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_user WHERE username = ?",
                Integer.class,
                normalizedUsername
        );

        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        int safeStatus = resolveStatusOrDefault(request.getStatus(), 1);

        Long userId = IdWorker.getId();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        jdbcTemplate.update(
                "INSERT INTO app_user (id, username, password_hash, status, last_login_at, created_at, updated_at) VALUES (?, ?, ?, ?, NULL, NOW(), NOW())",
                userId,
                normalizedUsername,
                passwordHash,
                safeStatus
        );

        return getUserById(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserListItemResponse updateUserStatus(Long userId, Integer status) {
        int safeStatus = resolveStatusOrDefault(status, 1);
        ensureUserExists(userId);

        jdbcTemplate.update(
                "UPDATE app_user SET status = ?, updated_at = NOW() WHERE id = ?",
                safeStatus,
                userId
        );

        return getUserById(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteUser(Long userId) {
        ensureUserExists(userId);

        jdbcTemplate.update("DELETE FROM user_favorite WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_watch_history WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM video_like WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM video_comment WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", userId);

        return "deleted";
    }

    private void ensureUserExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM app_user WHERE id = ?", Integer.class, userId);
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    private AdminUserListItemResponse getUserById(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, username, status, last_login_at, created_at, updated_at FROM app_user WHERE id = ?",
                    (rs, rowNum) -> {
                        AdminUserListItemResponse item = new AdminUserListItemResponse();
                        item.setId(rs.getLong("id"));
                        item.setUsername(rs.getString("username"));
                        item.setStatus((Integer) rs.getObject("status"));
                        item.setLastLoginAt(rs.getTimestamp("last_login_at") == null
                                ? null
                                : rs.getTimestamp("last_login_at").toLocalDateTime());
                        item.setCreatedAt(rs.getTimestamp("created_at") == null
                                ? null
                                : rs.getTimestamp("created_at").toLocalDateTime());
                        item.setUpdatedAt(rs.getTimestamp("updated_at") == null
                                ? null
                                : rs.getTimestamp("updated_at").toLocalDateTime());
                        return item;
                    },
                    userId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveStatusOrDefault(Integer status, int defaultValue) {
        int safeStatus = status == null ? defaultValue : status;
        if (safeStatus != 0 && safeStatus != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 仅支持 0 或 1");
        }
        return safeStatus;
    }
}