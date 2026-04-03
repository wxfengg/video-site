package com.videosite.backend.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.user.dto.UserFavoriteItemResponse;
import com.videosite.backend.user.dto.UserWatchHistoryItemResponse;
import com.videosite.backend.user.dto.UserWatchProgressRequest;
import com.videosite.backend.user.dto.UserWatchProgressResponse;
import com.videosite.backend.video.dto.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserCenterService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final double COMPLETION_THRESHOLD = 0.9d;

    private final JdbcTemplate jdbcTemplate;

    public UserCenterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<UserFavoriteItemResponse> listFavorites(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        int offset = (safePage - 1) * safePageSize;

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_favorite uf JOIN video v ON v.id = uf.video_id WHERE uf.user_id = ? AND v.status IN ('ready', 'published')",
                Long.class,
                userId
        );

        List<UserFavoriteItemResponse> records = jdbcTemplate.query(
                "SELECT v.id, v.title, v.cover_url, v.status, v.duration_sec, v.publish_at, uf.created_at AS favorited_at " +
                        "FROM user_favorite uf " +
                        "JOIN video v ON v.id = uf.video_id " +
                        "WHERE uf.user_id = ? AND v.status IN ('ready', 'published') " +
                        "ORDER BY uf.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    UserFavoriteItemResponse item = new UserFavoriteItemResponse();
                    item.setId(rs.getLong("id"));
                    item.setTitle(rs.getString("title"));
                    item.setCoverUrl(rs.getString("cover_url"));
                    item.setStatus(rs.getString("status"));
                    item.setDurationSec((Integer) rs.getObject("duration_sec"));
                    item.setPublishAt(rs.getTimestamp("publish_at") == null ? null : rs.getTimestamp("publish_at").toLocalDateTime());
                    item.setFavoritedAt(rs.getTimestamp("favorited_at") == null ? null : rs.getTimestamp("favorited_at").toLocalDateTime());
                    return item;
                },
                userId,
                safePageSize,
                offset
        );

        return new PageResult<>(total == null ? 0 : total, safePage, safePageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public String addFavorite(Long userId, Long videoId) {
        ensurePublicVideo(videoId);
        jdbcTemplate.update(
                "INSERT IGNORE INTO user_favorite (id, user_id, video_id, created_at) VALUES (?, ?, ?, NOW())",
                IdWorker.getId(),
                userId,
                videoId
        );
        return "ok";
    }

    @Transactional(rollbackFor = Exception.class)
    public String removeFavorite(Long userId, Long videoId) {
        jdbcTemplate.update("DELETE FROM user_favorite WHERE user_id = ? AND video_id = ?", userId, videoId);
        return "ok";
    }

    public PageResult<UserWatchHistoryItemResponse> listHistory(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        int offset = (safePage - 1) * safePageSize;

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_watch_history h JOIN video v ON v.id = h.video_id WHERE h.user_id = ? AND v.status IN ('ready', 'published')",
                Long.class,
                userId
        );

        List<UserWatchHistoryItemResponse> records = jdbcTemplate.query(
                "SELECT v.id, v.title, v.cover_url, v.status, v.duration_sec, " +
                        "h.last_progress_sec, h.duration_sec_snapshot, h.completion_rate, h.is_completed_90, h.last_watched_at " +
                        "FROM user_watch_history h " +
                        "JOIN video v ON v.id = h.video_id " +
                        "WHERE h.user_id = ? AND v.status IN ('ready', 'published') " +
                        "ORDER BY h.last_watched_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    UserWatchHistoryItemResponse item = new UserWatchHistoryItemResponse();
                    item.setId(rs.getLong("id"));
                    item.setTitle(rs.getString("title"));
                    item.setCoverUrl(rs.getString("cover_url"));
                    item.setStatus(rs.getString("status"));
                    item.setDurationSec((Integer) rs.getObject("duration_sec"));
                    item.setLastProgressSec((Integer) rs.getObject("last_progress_sec"));
                    item.setDurationSecSnapshot((Integer) rs.getObject("duration_sec_snapshot"));
                    item.setCompletionRate(rs.getObject("completion_rate") == null ? 0d : rs.getDouble("completion_rate"));
                    item.setCompleted90(rs.getInt("is_completed_90") == 1);
                    item.setLastWatchedAt(rs.getTimestamp("last_watched_at") == null ? null : rs.getTimestamp("last_watched_at").toLocalDateTime());
                    return item;
                },
                userId,
                safePageSize,
                offset
        );

        return new PageResult<>(total == null ? 0 : total, safePage, safePageSize, records);
    }

    public UserWatchProgressResponse getWatchProgress(Long userId, Long videoId) {
        List<UserWatchProgressResponse> rows = jdbcTemplate.query(
                "SELECT video_id, last_progress_sec, duration_sec_snapshot, completion_rate, is_completed_90, last_watched_at " +
                        "FROM user_watch_history WHERE user_id = ? AND video_id = ? LIMIT 1",
                (rs, rowNum) -> {
                    UserWatchProgressResponse response = new UserWatchProgressResponse();
                    response.setVideoId(rs.getLong("video_id"));
                    response.setProgressSec((Integer) rs.getObject("last_progress_sec"));
                    response.setDurationSecSnapshot((Integer) rs.getObject("duration_sec_snapshot"));
                    response.setCompletionRate(rs.getObject("completion_rate") == null ? 0d : rs.getDouble("completion_rate"));
                    response.setCompleted90(rs.getInt("is_completed_90") == 1);
                    response.setLastWatchedAt(rs.getTimestamp("last_watched_at") == null ? null : rs.getTimestamp("last_watched_at").toLocalDateTime());
                    return response;
                },
                userId,
                videoId
        );

        if (rows.isEmpty()) {
            UserWatchProgressResponse response = new UserWatchProgressResponse();
            response.setVideoId(videoId);
            response.setProgressSec(0);
            response.setDurationSecSnapshot(null);
            response.setCompletionRate(0d);
            response.setCompleted90(false);
            response.setLastWatchedAt(null);
            return response;
        }

        return rows.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserWatchProgressResponse updateWatchProgress(Long userId, Long videoId, UserWatchProgressRequest request) {
        VideoDurationRow video = loadPublicVideo(videoId);

        int safeProgressSec = Math.max(request.getProgressSec(), 0);
        Integer durationSnapshot = request.getDurationSecSnapshot() != null
                ? request.getDurationSecSnapshot()
                : video.durationSec;

        double completionRate = calculateCompletionRate(safeProgressSec, durationSnapshot);
        boolean completed90 = completionRate >= COMPLETION_THRESHOLD;
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                "INSERT INTO user_watch_history (id, user_id, video_id, last_progress_sec, duration_sec_snapshot, completion_rate, is_completed_90, play_count, last_watched_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "last_progress_sec = VALUES(last_progress_sec), " +
                        "duration_sec_snapshot = COALESCE(VALUES(duration_sec_snapshot), duration_sec_snapshot), " +
                        "completion_rate = VALUES(completion_rate), " +
                        "is_completed_90 = VALUES(is_completed_90), " +
                        "last_watched_at = VALUES(last_watched_at), " +
                        "updated_at = NOW()",
                IdWorker.getId(),
                userId,
                videoId,
                safeProgressSec,
                durationSnapshot,
                completionRate,
                completed90 ? 1 : 0,
                Timestamp.valueOf(now)
        );

        return getWatchProgress(userId, videoId);
    }

    private void ensurePublicVideo(Long videoId) {
        loadPublicVideo(videoId);
    }

    private VideoDurationRow loadPublicVideo(Long videoId) {
        List<VideoDurationRow> videos = jdbcTemplate.query(
                "SELECT id, duration_sec FROM video WHERE id = ? AND status IN ('ready', 'published') LIMIT 1",
                (rs, rowNum) -> new VideoDurationRow(
                        rs.getLong("id"),
                        (Integer) rs.getObject("duration_sec")
                ),
                videoId
        );

        if (videos.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在或不可见");
        }

        return videos.get(0);
    }

    private double calculateCompletionRate(int progressSec, Integer durationSec) {
        if (durationSec == null || durationSec <= 0) {
            return 0d;
        }
        return Math.min(1d, (double) progressSec / (double) durationSec);
    }

    private static class VideoDurationRow {
        private final Long id;
        private final Integer durationSec;

        private VideoDurationRow(Long id, Integer durationSec) {
            this.id = id;
            this.durationSec = durationSec;
        }
    }
}
