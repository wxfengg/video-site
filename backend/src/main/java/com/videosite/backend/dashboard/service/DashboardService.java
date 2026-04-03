package com.videosite.backend.dashboard.service;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.dashboard.dto.DashboardOverviewResponse;
import com.videosite.backend.dashboard.dto.DashboardPlayFunnelResponse;
import com.videosite.backend.dashboard.dto.DashboardTrafficTrendResponse;
import com.videosite.backend.dashboard.dto.DashboardUserGrowthResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardOverviewResponse getOverview() {
        TimeRange range = resolveRange(null, null, 7);

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setFrom(range.from.toLocalDate().toString());
        response.setTo(range.to.toLocalDate().toString());

        SiteAggRow siteAgg = safeQueryForObject(
                "SELECT COALESCE(SUM(play_pv), 0) AS play_pv, COALESCE(MAX(dau), 0) AS peak_dau, COALESCE(SUM(new_users), 0) AS new_users " +
                        "FROM metric_site_5m WHERE bucket_time >= ? AND bucket_time <= ?",
                (rs, rowNum) -> new SiteAggRow(rs.getLong("play_pv"), rs.getLong("peak_dau"), rs.getLong("new_users")),
                range.fromTs,
                range.toTs
        );

        response.setPlayPv(siteAgg == null ? 0 : siteAgg.playPv);
        response.setPeakDau(siteAgg == null ? 0 : siteAgg.peakDau);
        response.setNewUsers(siteAgg == null ? 0 : siteAgg.newUsers);
        response.setPublishedVideos(safeCount("SELECT COUNT(1) FROM video WHERE status = 'published'"));
        response.setRunningExperiments(safeCount("SELECT COUNT(1) FROM ab_experiment WHERE status = 'running'"));
        response.setTotalComments(safeCount("SELECT COUNT(1) FROM video_comment WHERE status = 'normal'"));
        response.setTotalLikes(safeCount("SELECT COUNT(1) FROM video_like"));

        response.setHotVideos(loadHotVideos(10));
        response.setAbSummary(loadAbSummary(range, 10));
        return response;
    }

    public DashboardTrafficTrendResponse getTrafficTrend(String from, String to) {
        TimeRange range = resolveRange(from, to, 7);

        List<DashboardTrafficTrendResponse.TrafficPoint> points = safeQueryList(
                "SELECT bucket_time, dau, new_users, play_pv FROM metric_site_5m WHERE bucket_time >= ? AND bucket_time <= ? ORDER BY bucket_time ASC",
                (rs, rowNum) -> {
                    DashboardTrafficTrendResponse.TrafficPoint point = new DashboardTrafficTrendResponse.TrafficPoint();
                    point.setBucketTime(rs.getTimestamp("bucket_time").toLocalDateTime());
                    point.setDau(rs.getInt("dau"));
                    point.setNewUsers(rs.getInt("new_users"));
                    point.setPlayPv(rs.getInt("play_pv"));
                    return point;
                },
                range.fromTs,
                range.toTs
        );

        long totalPlayPv = 0;
        long peakDau = 0;
        for (DashboardTrafficTrendResponse.TrafficPoint point : points) {
            totalPlayPv += safeInt(point.getPlayPv());
            peakDau = Math.max(peakDau, safeInt(point.getDau()));
        }

        DashboardTrafficTrendResponse response = new DashboardTrafficTrendResponse();
        response.setFrom(range.from.toString());
        response.setTo(range.to.toString());
        response.setPoints(points);
        response.setTotalPlayPv(totalPlayPv);
        response.setPeakDau(peakDau);
        return response;
    }

    public DashboardUserGrowthResponse getUserGrowth(String from, String to) {
        TimeRange range = resolveRange(from, to, 14);

        List<DailyUserRow> rows = safeQueryList(
                "SELECT DATE(created_at) AS day, COUNT(1) AS new_users FROM app_user WHERE created_at >= ? AND created_at <= ? GROUP BY DATE(created_at) ORDER BY day ASC",
                (rs, rowNum) -> new DailyUserRow(rs.getDate("day").toLocalDate(), rs.getLong("new_users")),
                range.fromTs,
                range.toTs
        );

        Map<LocalDate, Long> byDay = new HashMap<>();
        for (DailyUserRow row : rows) {
            byDay.put(row.day, row.newUsers);
        }

        long userBase = safeCount(
                "SELECT COUNT(1) FROM app_user WHERE created_at < ?",
                range.fromTs
        );

        DashboardUserGrowthResponse response = new DashboardUserGrowthResponse();
        response.setFrom(range.from.toLocalDate().toString());
        response.setTo(range.to.toLocalDate().toString());

        List<DashboardUserGrowthResponse.UserGrowthPoint> points = new ArrayList<>();
        long cumulative = userBase;
        long totalNewUsers = 0;
        for (LocalDate day = range.from.toLocalDate(); !day.isAfter(range.to.toLocalDate()); day = day.plusDays(1)) {
            long dayNewUsers = byDay.getOrDefault(day, 0L);
            cumulative += dayNewUsers;
            totalNewUsers += dayNewUsers;

            DashboardUserGrowthResponse.UserGrowthPoint point = new DashboardUserGrowthResponse.UserGrowthPoint();
            point.setDay(day);
            point.setNewUsers(dayNewUsers);
            point.setCumulativeUsers(cumulative);
            points.add(point);
        }

        response.setPoints(points);
        response.setTotalNewUsers(totalNewUsers);
        response.setCurrentUserTotal(cumulative);
        return response;
    }

    public DashboardPlayFunnelResponse getPlayFunnel(Long videoId, String from, String to) {
        TimeRange range = resolveRange(from, to, 7);

        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("COUNT(DISTINCT CASE WHEN event_type = 'exposure' THEN visitor_id END) AS exposure_uv, ")
                .append("COUNT(DISTINCT CASE WHEN event_type = 'click' THEN visitor_id END) AS click_uv, ")
                .append("COUNT(DISTINCT CASE WHEN event_type = 'play' THEN visitor_id END) AS play_uv, ")
                .append("COUNT(DISTINCT CASE WHEN event_type = 'complete' THEN visitor_id END) AS complete_uv ")
                .append("FROM event_log WHERE event_time >= ? AND event_time <= ? ");
        args.add(range.fromTs);
        args.add(range.toTs);

        if (videoId != null) {
            sql.append("AND video_id = ? ");
            args.add(videoId);
        }

        FunnelCountRow row = safeQueryForObject(
                sql.toString(),
                (rs, rowNum) -> new FunnelCountRow(
                        rs.getLong("exposure_uv"),
                        rs.getLong("click_uv"),
                        rs.getLong("play_uv"),
                        rs.getLong("complete_uv")
                ),
                args.toArray()
        );

        long exposureUv = row == null ? 0 : row.exposureUv;
        long clickUv = row == null ? 0 : row.clickUv;
        long playUv = row == null ? 0 : row.playUv;
        long completeUv = row == null ? 0 : row.completeUv;

        DashboardPlayFunnelResponse response = new DashboardPlayFunnelResponse();
        response.setVideoId(videoId);
        response.setFrom(range.from.toString());
        response.setTo(range.to.toString());
        response.setExposureUv(exposureUv);
        response.setClickUv(clickUv);
        response.setPlayUv(playUv);
        response.setCompleteUv(completeUv);
        response.setCtr(calculateRatio(clickUv, exposureUv));
        response.setPlayThroughRate(calculateRatio(playUv, clickUv));
        response.setCompletionRate(calculateRatio(completeUv, playUv));
        response.setStages(List.of(
                new DashboardPlayFunnelResponse.FunnelStage("曝光", exposureUv),
                new DashboardPlayFunnelResponse.FunnelStage("点击", clickUv),
                new DashboardPlayFunnelResponse.FunnelStage("播放", playUv),
                new DashboardPlayFunnelResponse.FunnelStage("完播", completeUv)
        ));

        return response;
    }

    private List<DashboardOverviewResponse.HotVideoItem> loadHotVideos(int limit) {
        return safeQueryList(
                "SELECT r.video_id, r.rank_index, r.hot_score, v.title, v.cover_url " +
                        "FROM video_hot_rank_5m r " +
                        "JOIN video v ON v.id = r.video_id " +
                        "WHERE r.window_type = '24h' " +
                        "AND r.bucket_time = (SELECT MAX(bucket_time) FROM video_hot_rank_5m WHERE window_type = '24h') " +
                        "ORDER BY r.rank_index ASC LIMIT ?",
                (rs, rowNum) -> {
                    DashboardOverviewResponse.HotVideoItem item = new DashboardOverviewResponse.HotVideoItem();
                    item.setVideoId(rs.getLong("video_id"));
                    item.setRankIndex(rs.getInt("rank_index"));
                    item.setHotScore(rs.getBigDecimal("hot_score") == null ? 0D : rs.getBigDecimal("hot_score").doubleValue());
                    item.setTitle(rs.getString("title"));
                    item.setCoverUrl(rs.getString("cover_url"));
                    return item;
                },
                limit
        );
    }

    private List<DashboardOverviewResponse.AbSummaryItem> loadAbSummary(TimeRange range, int limit) {
        List<DashboardOverviewResponse.AbSummaryItem> rows = safeQueryList(
                "SELECT e.id AS experiment_id, e.name AS experiment_name, l.ab_variant AS variant_code, " +
                        "COUNT(DISTINCT CASE WHEN l.event_type = 'exposure' THEN l.visitor_id END) AS exposure_uv, " +
                        "COUNT(DISTINCT CASE WHEN l.event_type = 'click' THEN l.visitor_id END) AS click_uv " +
                        "FROM ab_experiment e " +
                        "LEFT JOIN event_log l ON l.ab_experiment_id = e.id AND l.event_time >= ? AND l.event_time <= ? " +
                        "WHERE e.status = 'running' " +
                        "GROUP BY e.id, e.name, l.ab_variant",
                (rs, rowNum) -> {
                    DashboardOverviewResponse.AbSummaryItem item = new DashboardOverviewResponse.AbSummaryItem();
                    item.setExperimentId(rs.getLong("experiment_id"));
                    item.setExperimentName(rs.getString("experiment_name"));
                    item.setVariantCode(rs.getString("variant_code"));
                    item.setExposureUv(rs.getLong("exposure_uv"));
                    item.setClickUv(rs.getLong("click_uv"));
                    return item;
                },
                range.fromTs,
                range.toTs
        );

        List<DashboardOverviewResponse.AbSummaryItem> filtered = new ArrayList<>();
        for (DashboardOverviewResponse.AbSummaryItem row : rows) {
            if (!StringUtils.hasText(row.getVariantCode())) {
                continue;
            }
            long exposureUv = row.getExposureUv() == null ? 0 : row.getExposureUv();
            long clickUv = row.getClickUv() == null ? 0 : row.getClickUv();
            row.setCtr(calculateRatio(clickUv, exposureUv));
            filtered.add(row);
        }

        filtered.sort(Comparator
                .comparing(DashboardOverviewResponse.AbSummaryItem::getCtr, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DashboardOverviewResponse.AbSummaryItem::getExposureUv, Comparator.nullsLast(Comparator.reverseOrder())));

        if (filtered.size() <= limit) {
            return filtered;
        }
        return filtered.subList(0, limit);
    }

    private <T> T safeQueryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, args);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private <T> List<T> safeQueryList(String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return jdbcTemplate.query(sql, rowMapper, args);
        } catch (DataAccessException ex) {
            return Collections.emptyList();
        }
    }

    private long safeCount(String sql, Object... args) {
        try {
            return defaultLong(jdbcTemplate.queryForObject(sql, Long.class, args));
        } catch (DataAccessException ex) {
            return 0L;
        }
    }

    private TimeRange resolveRange(String from, String to, int defaultDays) {
        LocalDateTime toTime = parseToTime(to, false);
        LocalDateTime fromTime = parseToTime(from, true);

        if (toTime == null) {
            toTime = LocalDateTime.now();
        }
        if (fromTime == null) {
            fromTime = toTime.minusDays(defaultDays);
        }

        if (fromTime.isAfter(toTime)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "from 不能晚于 to");
        }

        return new TimeRange(fromTime, toTime);
    }

    private LocalDateTime parseToTime(String raw, boolean startOfDayWhenDateOnly) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String input = raw.trim();
        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DATETIME_FORMAT
        );

        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            LocalDate date = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
            return startOfDayWhenDateOnly ? date.atStartOfDay() : date.atTime(LocalTime.of(23, 59, 59));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时间参数格式错误，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private long safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0 : value;
    }

    static double calculateRatio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        double value = (double) numerator / (double) denominator;
        return Math.max(0D, Math.min(1D, value));
    }

    private static class TimeRange {
        private final LocalDateTime from;
        private final LocalDateTime to;
        private final Timestamp fromTs;
        private final Timestamp toTs;

        private TimeRange(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
            this.fromTs = Timestamp.valueOf(from);
            this.toTs = Timestamp.valueOf(to);
        }
    }

    private static class SiteAggRow {
        private final long playPv;
        private final long peakDau;
        private final long newUsers;

        private SiteAggRow(long playPv, long peakDau, long newUsers) {
            this.playPv = playPv;
            this.peakDau = peakDau;
            this.newUsers = newUsers;
        }
    }

    private static class DailyUserRow {
        private final LocalDate day;
        private final long newUsers;

        private DailyUserRow(LocalDate day, long newUsers) {
            this.day = day;
            this.newUsers = newUsers;
        }
    }

    private static class FunnelCountRow {
        private final long exposureUv;
        private final long clickUv;
        private final long playUv;
        private final long completeUv;

        private FunnelCountRow(long exposureUv, long clickUv, long playUv, long completeUv) {
            this.exposureUv = exposureUv;
            this.clickUv = clickUv;
            this.playUv = playUv;
            this.completeUv = completeUv;
        }
    }
}
