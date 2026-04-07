package com.videosite.backend.recommend.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.recommend.dto.VideoHotRankItemResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class VideoHotRankService {

    private static final String WINDOW_24H = "24h";
    private static final String WINDOW_7D = "7d";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private static final String HOT_SCORE_CASE =
            "CASE e.event_type " +
                    "WHEN 'play' THEN 2.0 " +
                    "WHEN 'click' THEN 1.5 " +
                    "WHEN 'exposure' THEN 0.5 " +
                    "WHEN 'complete' THEN 3.0 " +
                    "WHEN 'like' THEN 2.5 " +
                    "WHEN 'comment' THEN 3.5 " +
                    "WHEN 'unlike' THEN -1.5 " +
                    "WHEN 'comment_delete' THEN -2.0 " +
                    "ELSE 0 END";

    private final JdbcTemplate jdbcTemplate;

    public VideoHotRankService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VideoHotRankItemResponse> listLatest(String windowType, int limit) {
        String safeWindowType = normalizeWindowType(windowType);
        int safeLimit = normalizeLimit(limit);

        try {
            List<Timestamp> bucketRows = jdbcTemplate.query(
                    "SELECT bucket_time FROM video_hot_rank_5m WHERE window_type = ? ORDER BY bucket_time DESC LIMIT 1",
                    (rs, rowNum) -> rs.getTimestamp("bucket_time"),
                    safeWindowType
            );
            if (bucketRows.isEmpty()) {
                return List.of();
            }

            Timestamp bucketTime = bucketRows.get(0);
            return jdbcTemplate.query(
                    "SELECT r.window_type, r.bucket_time, r.video_id, r.rank_index, r.hot_score, " +
                            "v.title, v.cover_url, v.duration_sec, v.publish_at " +
                            "FROM video_hot_rank_5m r " +
                            "JOIN video v ON v.id = r.video_id " +
                            "WHERE r.window_type = ? AND r.bucket_time = ? AND v.status = 'published' " +
                            "ORDER BY r.rank_index ASC LIMIT ?",
                    (rs, rowNum) -> {
                        VideoHotRankItemResponse item = new VideoHotRankItemResponse();
                        item.setWindowType(rs.getString("window_type"));
                        item.setBucketTime(rs.getTimestamp("bucket_time") == null ? null : rs.getTimestamp("bucket_time").toLocalDateTime());
                        item.setVideoId(rs.getLong("video_id"));
                        item.setRankIndex(rs.getInt("rank_index"));

                        BigDecimal hotScore = rs.getBigDecimal("hot_score");
                        item.setHotScore(hotScore == null ? 0D : hotScore.doubleValue());

                        item.setTitle(rs.getString("title"));
                        item.setCoverUrl(rs.getString("cover_url"));
                        item.setDurationSec(rs.getObject("duration_sec") == null ? null : rs.getInt("duration_sec"));
                        item.setPublishAt(rs.getTimestamp("publish_at") == null ? null : rs.getTimestamp("publish_at").toLocalDateTime());
                        return item;
                    },
                    safeWindowType,
                    bucketTime,
                    safeLimit
            );
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int rebuildLatestSnapshot(int topN) {
        int safeTopN = normalizeLimit(topN);

        LocalDateTime bucketEnd = floorToFiveMinute(LocalDateTime.now());
        LocalDateTime bucketStart = bucketEnd.minusMinutes(5);

        Timestamp bucketTime = Timestamp.valueOf(bucketStart);
        Timestamp bucketEndTime = Timestamp.valueOf(bucketEnd);

        rebuildVideoMetricBucket(bucketTime, bucketEndTime);
        rebuildSiteMetricBucket(bucketTime, bucketStart, bucketEndTime);

        int stored = 0;
        stored += rebuildHotRankForWindow(bucketTime, bucketEndTime, WINDOW_24H, safeTopN, "24 HOUR");
        stored += rebuildHotRankForWindow(bucketTime, bucketEndTime, WINDOW_7D, safeTopN, "7 DAY");
        return stored;
    }

    private int rebuildHotRankForWindow(Timestamp bucketTime,
                                        Timestamp bucketEndTime,
                                        String windowType,
                                        int topN,
                                        String intervalExpression) {
        jdbcTemplate.update(
                "DELETE FROM video_hot_rank_5m WHERE bucket_time = ? AND window_type = ?",
                bucketTime,
                windowType
        );

        String sql = "SELECT e.video_id, SUM(" + HOT_SCORE_CASE + ") AS hot_score " +
                "FROM event_log e " +
                "JOIN video v ON v.id = e.video_id " +
                "WHERE e.video_id IS NOT NULL " +
                "AND e.event_time >= DATE_SUB(?, INTERVAL " + intervalExpression + ") " +
                "AND e.event_time < ? " +
                "AND v.status = 'published' " +
                "GROUP BY e.video_id " +
                "HAVING hot_score > 0 " +
                "ORDER BY hot_score DESC, e.video_id ASC LIMIT ?";

        List<HotScoreRow> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new HotScoreRow(rs.getLong("video_id"), rs.getBigDecimal("hot_score")),
                bucketEndTime,
                bucketEndTime,
                topN
        );

        int rankIndex = 1;
        for (HotScoreRow row : rows) {
            jdbcTemplate.update(
                    "INSERT INTO video_hot_rank_5m (id, bucket_time, window_type, rank_index, video_id, hot_score, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    IdWorker.getId(),
                    bucketTime,
                    windowType,
                    rankIndex,
                    row.videoId,
                    row.hotScore
            );
            rankIndex += 1;
        }

        return rows.size();
    }

    private void rebuildVideoMetricBucket(Timestamp bucketTime, Timestamp bucketEndTime) {
        jdbcTemplate.update("DELETE FROM metric_video_5m WHERE bucket_time = ?", bucketTime);

        List<VideoMetricRow> rows = jdbcTemplate.query(
                "SELECT e.video_id, " +
                        "COUNT(DISTINCT CASE WHEN e.event_type = 'exposure' THEN e.visitor_id END) AS exposure_uv, " +
                        "COUNT(DISTINCT CASE WHEN e.event_type = 'click' THEN e.visitor_id END) AS click_uv, " +
                        "COUNT(DISTINCT CASE WHEN e.event_type = 'play' THEN e.visitor_id END) AS play_uv, " +
                        "COUNT(DISTINCT CASE WHEN e.event_type = 'like' THEN e.visitor_id END) AS like_uv, " +
                        "COUNT(DISTINCT CASE WHEN e.event_type = 'complete' THEN e.visitor_id END) AS complete_uv " +
                        "FROM event_log e " +
                        "JOIN video v ON v.id = e.video_id " +
                        "WHERE e.video_id IS NOT NULL AND e.event_time >= ? AND e.event_time < ? AND v.status = 'published' " +
                        "GROUP BY e.video_id",
                (rs, rowNum) -> new VideoMetricRow(
                        rs.getLong("video_id"),
                        rs.getInt("exposure_uv"),
                        rs.getInt("click_uv"),
                        rs.getInt("play_uv"),
                        rs.getInt("like_uv"),
                        rs.getInt("complete_uv")
                ),
                bucketTime,
                bucketEndTime
        );

        for (VideoMetricRow row : rows) {
            double completionRate = row.playUv <= 0 ? 0D : Math.min(1D, (double) row.completeUv / (double) row.playUv);
            jdbcTemplate.update(
                    "INSERT INTO metric_video_5m (id, bucket_time, video_id, exposure_uv, click_uv, play_uv, like_uv, complete_uv, completion_rate_90, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "exposure_uv = VALUES(exposure_uv), " +
                            "click_uv = VALUES(click_uv), " +
                            "play_uv = VALUES(play_uv), " +
                            "like_uv = VALUES(like_uv), " +
                            "complete_uv = VALUES(complete_uv), " +
                            "completion_rate_90 = VALUES(completion_rate_90), " +
                            "updated_at = NOW()",
                    IdWorker.getId(),
                    bucketTime,
                    row.videoId,
                    row.exposureUv,
                    row.clickUv,
                    row.playUv,
                    row.likeUv,
                    row.completeUv,
                    BigDecimal.valueOf(completionRate)
            );
        }
    }

    private void rebuildSiteMetricBucket(Timestamp bucketTime, LocalDateTime bucketStart, Timestamp bucketEndTime) {
        Integer dau = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT visitor_id) FROM event_log WHERE event_time >= DATE_SUB(?, INTERVAL 1 DAY) AND event_time < ?",
                Integer.class,
                bucketEndTime,
                bucketEndTime
        );
        Integer newUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_user WHERE created_at >= ? AND created_at < ?",
                Integer.class,
                Timestamp.valueOf(bucketStart),
                bucketEndTime
        );
        Integer playPv = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM event_log WHERE event_type = 'play' AND event_time >= ? AND event_time < ?",
                Integer.class,
                bucketTime,
                bucketEndTime
        );

        jdbcTemplate.update(
                "INSERT INTO metric_site_5m (id, bucket_time, dau, new_users, play_pv, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "dau = VALUES(dau), " +
                        "new_users = VALUES(new_users), " +
                        "play_pv = VALUES(play_pv), " +
                        "updated_at = NOW()",
                IdWorker.getId(),
                bucketTime,
                dau == null ? 0 : dau,
                newUsers == null ? 0 : newUsers,
                playPv == null ? 0 : playPv
        );
    }

    private String normalizeWindowType(String windowType) {
        if (!StringUtils.hasText(windowType)) {
            return WINDOW_24H;
        }
        String normalized = windowType.trim().toLowerCase(Locale.ROOT);
        if (WINDOW_7D.equals(normalized)) {
            return WINDOW_7D;
        }
        return WINDOW_24H;
    }

    private int normalizeLimit(int limit) {
        int raw = limit <= 0 ? DEFAULT_LIMIT : limit;
        return Math.min(raw, MAX_LIMIT);
    }

    private LocalDateTime floorToFiveMinute(LocalDateTime time) {
        int flooredMinute = (time.getMinute() / 5) * 5;
        return time.withMinute(flooredMinute).withSecond(0).withNano(0);
    }

    private static class HotScoreRow {
        private final Long videoId;
        private final BigDecimal hotScore;

        private HotScoreRow(Long videoId, BigDecimal hotScore) {
            this.videoId = videoId;
            this.hotScore = hotScore == null ? BigDecimal.ZERO : hotScore;
        }
    }

    private static class VideoMetricRow {
        private final Long videoId;
        private final int exposureUv;
        private final int clickUv;
        private final int playUv;
        private final int likeUv;
        private final int completeUv;

        private VideoMetricRow(Long videoId,
                               int exposureUv,
                               int clickUv,
                               int playUv,
                               int likeUv,
                               int completeUv) {
            this.videoId = videoId;
            this.exposureUv = exposureUv;
            this.clickUv = clickUv;
            this.playUv = playUv;
            this.likeUv = likeUv;
            this.completeUv = completeUv;
        }
    }
}
