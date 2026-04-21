package com.videosite.backend.recommend.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.recommend.dto.RecommendFeedbackRequest;
import com.videosite.backend.recommend.dto.RecommendationItemResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendService {

    private static final double ALPHA = 0.6;
    private static final double BETA = 0.2;
    private static final double GAMMA = 0.2;
    private static final String HOT_RANK_WINDOW_FOR_RECOMMEND = "7d";
    private static final String HOT_RANK_WINDOW_FOR_FALLBACK = "24h";

    private final JdbcTemplate jdbcTemplate;
    private final RecommendReasonGenerator recommendReasonGenerator;
    private final ObjectMapper objectMapper;

    public RecommendService(JdbcTemplate jdbcTemplate,
                            RecommendReasonGenerator recommendReasonGenerator,
                            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.recommendReasonGenerator = recommendReasonGenerator;
        this.objectMapper = objectMapper;
    }

    public List<RecommendationItemResponse> listHomeRecommendations(String visitorId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        try {
            String latestVersion = latestVersionHour(visitorId, "home");
            if (!StringUtils.hasText(latestVersion)) {
                return fallbackByHot(safeLimit);
            }

            List<RecommendationItemResponse> rows = jdbcTemplate.query(
                "SELECT rr.video_id, rr.rank_index, rr.score_total, rr.score_content, rr.score_cf, rr.score_hot, " +
                    "v.title, v.cover_url, v.duration_sec, rr.recommend_reason " +
                    "FROM recommendation_result rr " +
                    "JOIN video v ON v.id = rr.video_id " +
                    "WHERE rr.visitor_id = ? AND rr.scene = 'home' AND rr.version_hour = ? AND v.status = 'published' " +
                    "ORDER BY rr.rank_index ASC LIMIT ?",
                    (rs, rowNum) -> {
                        RecommendationItemResponse item = new RecommendationItemResponse();
                        item.setVideoId(rs.getLong("video_id"));
                        item.setRankIndex(rs.getInt("rank_index"));
                        item.setScoreTotal(rs.getDouble("score_total"));
                        item.setScoreContent(rs.getObject("score_content") == null ? null : rs.getDouble("score_content"));
                        item.setScoreCf(rs.getObject("score_cf") == null ? null : rs.getDouble("score_cf"));
                        item.setScoreHot(rs.getObject("score_hot") == null ? null : rs.getDouble("score_hot"));
                        item.setTitle(rs.getString("title"));
                        item.setCoverUrl(rs.getString("cover_url"));
                        item.setDurationSec((Integer) rs.getObject("duration_sec"));
                        item.setRecommendReason(rs.getString("recommend_reason"));
                        return item;
                    },
                    visitorId,
                    latestVersion,
                    safeLimit
            );

            if (rows.isEmpty()) {
                return fallbackByHot(safeLimit);
            }
            return rows;
        } catch (DataAccessException ex) {
            return fallbackByPublished(safeLimit);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int rebuildForCurrentHour() {
        String versionHour = currentVersionHour();
        List<String> visitorIds = jdbcTemplate.query(
                "SELECT DISTINCT visitor_id FROM event_log WHERE event_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) ORDER BY event_time DESC LIMIT 100",
                (rs, rowNum) -> rs.getString("visitor_id")
        );

        if (visitorIds.isEmpty()) {
            visitorIds = List.of("anonymous");
        }

        List<VideoDoc> videoDocs = loadPublishedVideos();
        if (videoDocs.isEmpty()) {
            return 0;
        }

        Map<Long, Map<String, Double>> tfidfVectors = buildTfidfVectors(videoDocs);
        persistTfidfProfiles(versionHour, videoDocs, tfidfVectors);
        persistSimilarities(versionHour, tfidfVectors);

        int totalStored = 0;
        for (String visitorId : visitorIds) {
            totalStored += buildVisitorRecommendation(visitorId, versionHour);
        }
        return totalStored;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFeedback(HttpServletRequest request, RecommendFeedbackRequest feedback) {
        Object visitorAttr = request.getAttribute(AuthConstants.VISITOR_ID_ATTR);
        String visitorId = visitorAttr == null ? "anonymous" : String.valueOf(visitorAttr);

        String scene = StringUtils.hasText(feedback.getScene()) ? feedback.getScene() : "home";
        String safeAction = feedback.getAction().trim().toLowerCase(Locale.ROOT);
        String eventType = "recommend_feedback";
        if ("click".equals(safeAction)) {
            eventType = "click";
        } else if ("exposure".equals(safeAction)) {
            eventType = "exposure";
        }

        String extraJson = "{\"scene\":\"" + scene + "\",\"action\":\"" + safeAction + "\"}";

        jdbcTemplate.update(
                "INSERT INTO event_log (id, visitor_id, video_id, event_type, event_time, session_id, page_path, ab_experiment_id, ab_variant, progress_sec, extra_json, created_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), NULL, '/recommend/feedback', NULL, NULL, NULL, ?, NOW())",
                IdWorker.getId(),
                visitorId,
                feedback.getVideoId(),
                eventType,
                extraJson
        );
    }

    private int buildVisitorRecommendation(String visitorId, String versionHour) {
        List<Long> candidates = jdbcTemplate.query(
                "SELECT id FROM video WHERE status IN ('ready', 'published') ORDER BY created_at DESC LIMIT 200",
                (rs, rowNum) -> rs.getLong("id")
        );
        if (candidates.isEmpty()) {
            return 0;
        }

        Map<Long, Double> hotScoreMap = loadHotScores(candidates);
        Map<Long, Double> cfScoreMap = loadCfScores(candidates);
        List<Long> recentVideos = jdbcTemplate.query(
                "SELECT DISTINCT video_id FROM event_log WHERE visitor_id = ? AND video_id IS NOT NULL AND event_type IN ('play','click','complete') ORDER BY event_time DESC LIMIT 20",
                (rs, rowNum) -> rs.getLong("video_id"),
                visitorId
        );

        Map<Long, List<String>> categoryMap = loadCategories(candidates);

        List<ScoredVideo> scored = new ArrayList<>();
        for (Long videoId : candidates) {
            double content = contentScore(videoId, recentVideos, versionHour);
            double cf = cfScoreMap.getOrDefault(videoId, 0d);
            double hot = hotScoreMap.getOrDefault(videoId, 0d);
            double total = ALPHA * content + BETA * cf + GAMMA * hot;
            scored.add(new ScoredVideo(videoId, total, content, cf, hot));
        }

        scored.sort(Comparator.comparingDouble(ScoredVideo::getTotalScore).reversed());

        jdbcTemplate.update("DELETE FROM recommendation_result WHERE visitor_id = ? AND scene = 'home' AND version_hour = ?", visitorId, versionHour);

        int rank = 1;
        int stored = 0;
        for (ScoredVideo item : scored) {
            if (rank > 30) {
                break;
            }
            String reason = recommendReasonGenerator.generate(
                    item.contentScore, item.hotScore, categoryMap.get(item.videoId)
            );
            jdbcTemplate.update(
                    "INSERT INTO recommendation_result (id, visitor_id, video_id, scene, rank_index, score_total, score_content, score_cf, score_hot, recommend_reason, version_hour, created_at) " +
                            "VALUES (?, ?, ?, 'home', ?, ?, ?, ?, ?, ?, ?, NOW())",
                    IdWorker.getId(),
                    visitorId,
                    item.videoId,
                    rank,
                    item.totalScore,
                    item.contentScore,
                    item.cfScore,
                    item.hotScore,
                    reason,
                    versionHour
            );
            rank += 1;
            stored += 1;
        }

        return stored;
    }

    private Map<Long, Double> loadHotScores(List<Long> candidates) {
        Map<Long, Double> snapshotScores = loadHotScoresFromSnapshot(candidates, HOT_RANK_WINDOW_FOR_RECOMMEND);
        if (!snapshotScores.isEmpty()) {
            return snapshotScores;
        }

        return loadHotScoresFromEventLog(candidates);
    }

    private Map<Long, Double> loadHotScoresFromSnapshot(List<Long> candidates, String windowType) {
        try {
            List<Timestamp> bucketRows = jdbcTemplate.query(
                    "SELECT bucket_time FROM video_hot_rank_5m WHERE window_type = ? ORDER BY bucket_time DESC LIMIT 1",
                    (rs, rowNum) -> rs.getTimestamp("bucket_time"),
                    windowType
            );
            if (bucketRows.isEmpty()) {
                return Map.of();
            }

            Timestamp latestBucketTime = bucketRows.get(0);
            Set<Long> candidateSet = new HashSet<>(candidates);

            List<HotScoreRow> rows = jdbcTemplate.query(
                    "SELECT video_id, hot_score FROM video_hot_rank_5m WHERE window_type = ? AND bucket_time = ?",
                    (rs, rowNum) -> new HotScoreRow(rs.getLong("video_id"), rs.getBigDecimal("hot_score")),
                    windowType,
                    latestBucketTime
            );

            Map<Long, Double> raw = new HashMap<>();
            for (HotScoreRow row : rows) {
                if (candidateSet.contains(row.videoId)) {
                    raw.put(row.videoId, row.hotScore);
                }
            }

            if (raw.isEmpty()) {
                return Map.of();
            }

            return normalizeRawScores(candidates, raw);
        } catch (DataAccessException ex) {
            return Map.of();
        }
    }

    private Map<Long, Double> loadHotScoresFromEventLog(List<Long> candidates) {
        Map<Long, Double> raw = new HashMap<>();
        for (Long videoId : candidates) {
            Double score = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(CASE event_type " +
                            "WHEN 'play' THEN 2 " +
                            "WHEN 'click' THEN 1.5 " +
                            "WHEN 'exposure' THEN 0.5 " +
                            "WHEN 'complete' THEN 3 " +
                            "WHEN 'like' THEN 2.5 " +
                            "WHEN 'comment' THEN 3.5 " +
                            "WHEN 'unlike' THEN -1.5 " +
                            "WHEN 'comment_delete' THEN -2 " +
                            "ELSE 0 END), 0) " +
                            "FROM event_log WHERE video_id = ? AND event_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)",
                    Double.class,
                    videoId
            );
            double value = score == null ? 0 : score;
            raw.put(videoId, value);
        }

        return normalizeRawScores(candidates, raw);
    }

    private Map<Long, Double> normalizeRawScores(List<Long> candidates, Map<Long, Double> raw) {
        double max = 0;
        for (double value : raw.values()) {
            max = Math.max(max, value);
        }

        Map<Long, Double> normalized = new HashMap<>();
        if (max <= 0) {
            for (Long candidate : candidates) {
                normalized.put(candidate, 0d);
            }
            return normalized;
        }

        for (Long candidate : candidates) {
            normalized.put(candidate, raw.getOrDefault(candidate, 0d) / max);
        }
        return normalized;
    }

    private Map<Long, Double> loadCfScores(List<Long> candidates) {
        Map<Long, Double> raw = new HashMap<>();
        double max = 0;
        for (Long videoId : candidates) {
            Double uv = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT visitor_id) FROM event_log WHERE video_id = ? AND event_type IN ('click','play','complete') AND event_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)",
                    Double.class,
                    videoId
            );
            double value = uv == null ? 0 : uv;
            raw.put(videoId, value);
            max = Math.max(max, value);
        }

        Map<Long, Double> normalized = new HashMap<>();
        for (Map.Entry<Long, Double> entry : raw.entrySet()) {
            normalized.put(entry.getKey(), max <= 0 ? 0 : entry.getValue() / max);
        }
        return normalized;
    }

    private double contentScore(Long videoId, List<Long> historyVideoIds, String versionHour) {
        if (historyVideoIds == null || historyVideoIds.isEmpty()) {
            return 0;
        }

        double total = 0;
        int count = 0;
        for (Long history : historyVideoIds) {
            List<Double> simRows = jdbcTemplate.query(
                    "SELECT similarity_score FROM video_similarity WHERE video_id = ? AND related_video_id = ? AND version_hour = ? LIMIT 1",
                    (rs, rowNum) -> rs.getDouble("similarity_score"),
                    history,
                    videoId,
                    versionHour
            );
            Double sim = simRows.isEmpty() ? null : simRows.get(0);
            if (sim != null) {
                total += sim;
                count += 1;
            }
        }

        if (count == 0) {
            return 0;
        }
        return total / count;
    }

    private String latestVersionHour(String visitorId, String scene) {
        List<String> list = jdbcTemplate.query(
                "SELECT version_hour FROM recommendation_result WHERE visitor_id = ? AND scene = ? ORDER BY created_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("version_hour"),
                visitorId,
                scene
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private List<RecommendationItemResponse> fallbackByHot(int limit) {
        try {
            List<Timestamp> bucketRows = jdbcTemplate.query(
                    "SELECT bucket_time FROM video_hot_rank_5m WHERE window_type = ? ORDER BY bucket_time DESC LIMIT 1",
                    (rs, rowNum) -> rs.getTimestamp("bucket_time"),
                    HOT_RANK_WINDOW_FOR_FALLBACK
            );

            if (!bucketRows.isEmpty()) {
                Timestamp latestBucketTime = bucketRows.get(0);
                List<RecommendationItemResponse> hotRows = jdbcTemplate.query(
                        "SELECT r.video_id, r.rank_index, r.hot_score " +
                                "FROM video_hot_rank_5m r " +
                                "JOIN video v ON v.id = r.video_id " +
                                "WHERE r.window_type = ? AND r.bucket_time = ? AND v.status = 'published' " +
                                "ORDER BY r.rank_index ASC LIMIT ?",
                        (rs, rowNum) -> {
                            RecommendationItemResponse item = new RecommendationItemResponse();
                            item.setVideoId(rs.getLong("video_id"));
                            item.setRankIndex(rs.getInt("rank_index"));

                            BigDecimal hotScore = rs.getBigDecimal("hot_score");
                            double safeHotScore = hotScore == null ? 0d : hotScore.doubleValue();

                            item.setScoreTotal(safeHotScore);
                            item.setScoreContent(0d);
                            item.setScoreCf(0d);
                            item.setScoreHot(safeHotScore);
                            return item;
                        },
                        HOT_RANK_WINDOW_FOR_FALLBACK,
                        latestBucketTime,
                        limit
                );

                if (!hotRows.isEmpty()) {
                    return hotRows;
                }
            }
        } catch (DataAccessException ex) {
            return fallbackByPublished(limit);
        }

        return fallbackByPublished(limit);
    }

    private List<RecommendationItemResponse> fallbackByPublished(int limit) {
        return jdbcTemplate.query(
                "SELECT v.id AS video_id FROM video v WHERE v.status = 'published' ORDER BY v.publish_at DESC LIMIT ?",
                (rs, rowNum) -> {
                    RecommendationItemResponse item = new RecommendationItemResponse();
                    item.setVideoId(rs.getLong("video_id"));
                    item.setRankIndex(rowNum + 1);
                    item.setScoreTotal(0d);
                    item.setScoreContent(0d);
                    item.setScoreCf(0d);
                    item.setScoreHot(0d);
                    return item;
                },
                limit
        );
    }

    private List<VideoDoc> loadPublishedVideos() {
        return jdbcTemplate.query(
                "SELECT v.id, v.title, v.description, vi.embedding_text FROM video v LEFT JOIN video_intelligence vi ON vi.video_id = v.id WHERE v.status IN ('ready', 'published') ORDER BY v.updated_at DESC LIMIT 500",
                (rs, rowNum) -> {
                    String embeddingText = rs.getString("embedding_text");
                    String text;
                    if (StringUtils.hasText(embeddingText)) {
                        text = embeddingText;
                    } else {
                        text = safe(rs.getString("title")) + " " + safe(rs.getString("description"));
                    }
                    return new VideoDoc(rs.getLong("id"), text);
                }
        );
    }

    private Map<Long, Map<String, Double>> buildTfidfVectors(List<VideoDoc> docs) {
        Map<Long, List<String>> tokensMap = new HashMap<>();
        Map<String, Integer> df = new HashMap<>();

        for (VideoDoc doc : docs) {
            List<String> tokens = tokenize(doc.text);
            tokensMap.put(doc.videoId, tokens);

            Set<String> unique = new HashSet<>(tokens);
            for (String token : unique) {
                df.put(token, df.getOrDefault(token, 0) + 1);
            }
        }

        int n = docs.size();
        Map<Long, Map<String, Double>> vectors = new HashMap<>();
        for (VideoDoc doc : docs) {
            List<String> tokens = tokensMap.get(doc.videoId);
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) {
                tf.put(token, tf.getOrDefault(token, 0) + 1);
            }

            Map<String, Double> vector = new HashMap<>();
            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                String token = entry.getKey();
                double termTf = (double) entry.getValue() / Math.max(1, tokens.size());
                double idf = Math.log((double) (n + 1) / (double) (df.getOrDefault(token, 0) + 1)) + 1;
                vector.put(token, termTf * idf);
            }
            vectors.put(doc.videoId, vector);
        }

        return vectors;
    }

    private void persistTfidfProfiles(String versionHour, List<VideoDoc> docs, Map<Long, Map<String, Double>> vectors) {
        for (VideoDoc doc : docs) {
            Map<String, Double> vector = vectors.getOrDefault(doc.videoId, Map.of());
            String vectorJson = toJson(vector);
            String tokenJson = toJson(tokenize(doc.text));

            jdbcTemplate.update(
                    "INSERT INTO video_tfidf_profile (id, video_id, tokens_json, tfidf_vector_json, version_hour, created_at) VALUES (?, ?, ?, ?, ?, NOW()) " +
                            "ON DUPLICATE KEY UPDATE tokens_json = VALUES(tokens_json), tfidf_vector_json = VALUES(tfidf_vector_json), created_at = NOW()",
                    IdWorker.getId(),
                    doc.videoId,
                    tokenJson,
                    vectorJson,
                    versionHour
            );
        }
    }

    private void persistSimilarities(String versionHour, Map<Long, Map<String, Double>> vectors) {
        jdbcTemplate.update("DELETE FROM video_similarity WHERE version_hour = ?", versionHour);

        List<Long> ids = new ArrayList<>(vectors.keySet());
        for (Long source : ids) {
            for (Long target : ids) {
                if (source.equals(target)) {
                    continue;
                }
                double sim = cosine(vectors.get(source), vectors.get(target));
                if (sim <= 0) {
                    continue;
                }
                jdbcTemplate.update(
                        "INSERT INTO video_similarity (id, video_id, related_video_id, similarity_score, version_hour, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                        IdWorker.getId(),
                        source,
                        target,
                        sim,
                        versionHour
                );
            }
        }
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0;
        }

        double dot = 0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            dot += entry.getValue() * b.getOrDefault(entry.getKey(), 0d);
        }

        double normA = 0;
        for (double v : a.values()) {
            normA += v * v;
        }

        double normB = 0;
        for (double v : b.values()) {
            normB += v * v;
        }

        if (normA <= 0 || normB <= 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        String[] items = normalized.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String item : items) {
            if (item.length() < 2) {
                continue;
            }
            tokens.add(item);
        }
        return tokens;
    }

    private String toJson(Map<String, Double> map) {
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(entry.getKey().replace("\"", "")).append('"')
                    .append(':')
                    .append(String.format(Locale.US, "%.8f", entry.getValue()));
            index += 1;
        }
        builder.append('}');
        return builder.toString();
    }

    private String toJson(List<String> list) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < list.size(); i += 1) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(list.get(i).replace("\"", "")).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    private String currentVersionHour() {
        return DateTimeFormatter.ofPattern("yyyyMMddHH").format(LocalDateTime.now());
    }

    private String safe(String text) {
        return StringUtils.hasText(text) ? text : "";
    }

    private Map<Long, List<String>> loadCategories(List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return new HashMap<>();
        }
        String placeholders = String.join(",", Collections.nCopies(videoIds.size(), "?"));
        String sql = "SELECT video_id, categories_json FROM video_intelligence WHERE video_id IN (" + placeholders + ")";

        Map<Long, List<String>> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long vid = rs.getLong("video_id");
            String json = rs.getString("categories_json");
            result.put(vid, parseStringList(json));
        }, videoIds.toArray());
        return result;
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private static class VideoDoc {
        private final Long videoId;
        private final String text;

        private VideoDoc(Long videoId, String text) {
            this.videoId = videoId;
            this.text = text;
        }
    }

    private static class ScoredVideo {
        private final Long videoId;
        private final double totalScore;
        private final double contentScore;
        private final double cfScore;
        private final double hotScore;

        private ScoredVideo(Long videoId, double totalScore, double contentScore, double cfScore, double hotScore) {
            this.videoId = videoId;
            this.totalScore = totalScore;
            this.contentScore = contentScore;
            this.cfScore = cfScore;
            this.hotScore = hotScore;
        }

        public double getTotalScore() {
            return totalScore;
        }
    }

    private static class HotScoreRow {
        private final Long videoId;
        private final double hotScore;

        private HotScoreRow(Long videoId, BigDecimal hotScore) {
            this.videoId = videoId;
            this.hotScore = hotScore == null ? 0d : hotScore.doubleValue();
        }
    }
}
