package com.videosite.backend.recommend.service;

import com.videosite.backend.recommend.dto.RecommendationItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendServiceTest {

    @Test
    void listHomeRecommendationsShouldFilterPublishedVideosInResultQuery() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RecommendService service = new RecommendService(jdbcTemplate);

        RecommendationItemResponse rec = new RecommendationItemResponse();
        rec.setVideoId(101L);
        rec.setRankIndex(1);
        rec.setScoreTotal(0.91d);
        rec.setScoreContent(0.8d);
        rec.setScoreCf(0.4d);
        rec.setScoreHot(0.7d);

        when(jdbcTemplate.query(
                eq("SELECT version_hour FROM recommendation_result WHERE visitor_id = ? AND scene = ? ORDER BY created_at DESC LIMIT 1"),
                any(RowMapper.class),
                eq("visitorA"),
                eq("home")
        )).thenReturn(List.of("2026033015"));

        when(jdbcTemplate.query(
                argThat((String sql) -> sql != null && sql.contains("WHERE rr.visitor_id = ?") && sql.contains("v.status = 'published'")),
                any(RowMapper.class),
                eq("visitorA"),
                eq("2026033015"),
                eq(12)
        )).thenReturn(List.of(rec));

        List<RecommendationItemResponse> result = service.listHomeRecommendations("visitorA", 12);
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getVideoId());
        assertEquals(1, result.get(0).getRankIndex());
        assertEquals(0.91d, result.get(0).getScoreTotal());
        assertEquals(0.8d, result.get(0).getScoreContent());
        assertEquals(0.4d, result.get(0).getScoreCf());
        assertEquals(0.7d, result.get(0).getScoreHot());

        verify(jdbcTemplate).query(
                argThat((String sql) -> sql != null && sql.contains("FROM recommendation_result rr") && sql.contains("JOIN video v") && sql.contains("v.status = 'published'")),
                any(RowMapper.class),
                eq("visitorA"),
                eq("2026033015"),
                eq(12)
        );
    }

    @Test
    void listHomeRecommendationsShouldFallbackToHotRankSnapshotWhenRecommendationRowsEmpty() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RecommendService service = new RecommendService(jdbcTemplate);

        Timestamp latestBucket = Timestamp.valueOf("2026-04-03 09:50:00");
        RecommendationItemResponse hot = new RecommendationItemResponse();
        hot.setVideoId(888L);
        hot.setRankIndex(1);
        hot.setScoreTotal(12.34d);
        hot.setScoreContent(0d);
        hot.setScoreCf(0d);
        hot.setScoreHot(12.34d);

        when(jdbcTemplate.query(
                eq("SELECT version_hour FROM recommendation_result WHERE visitor_id = ? AND scene = ? ORDER BY created_at DESC LIMIT 1"),
                any(RowMapper.class),
                eq("visitorB"),
                eq("home")
        )).thenReturn(List.of("2026040309"));

        when(jdbcTemplate.query(
                argThat((String sql) -> sql != null && sql.contains("FROM recommendation_result rr") && sql.contains("v.status = 'published'")),
                any(RowMapper.class),
                eq("visitorB"),
                eq("2026040309"),
                eq(10)
        )).thenReturn(List.of());

        when(jdbcTemplate.query(
                eq("SELECT bucket_time FROM video_hot_rank_5m WHERE window_type = ? ORDER BY bucket_time DESC LIMIT 1"),
                any(RowMapper.class),
                eq("24h")
        )).thenReturn(List.of(latestBucket));

        when(jdbcTemplate.query(
                argThat((String sql) -> sql != null && sql.contains("FROM video_hot_rank_5m r") && sql.contains("v.status = 'published'")),
                any(RowMapper.class),
                eq("24h"),
                eq(latestBucket),
                eq(10)
        )).thenReturn(List.of(hot));

        List<RecommendationItemResponse> result = service.listHomeRecommendations("visitorB", 10);
        assertEquals(1, result.size());
        assertEquals(888L, result.get(0).getVideoId());
        assertEquals(12.34d, result.get(0).getScoreHot());
    }

    @Test
    void listHomeRecommendationsShouldFallbackToPublishTimeWhenNoHotRankSnapshot() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RecommendService service = new RecommendService(jdbcTemplate);

        RecommendationItemResponse publishFallback = new RecommendationItemResponse();
        publishFallback.setVideoId(999L);
        publishFallback.setRankIndex(1);
        publishFallback.setScoreTotal(0d);
        publishFallback.setScoreContent(0d);
        publishFallback.setScoreCf(0d);
        publishFallback.setScoreHot(0d);

        when(jdbcTemplate.query(
                eq("SELECT version_hour FROM recommendation_result WHERE visitor_id = ? AND scene = ? ORDER BY created_at DESC LIMIT 1"),
                any(RowMapper.class),
                eq("visitorC"),
                eq("home")
        )).thenReturn(List.of());

        when(jdbcTemplate.query(
                eq("SELECT bucket_time FROM video_hot_rank_5m WHERE window_type = ? ORDER BY bucket_time DESC LIMIT 1"),
                any(RowMapper.class),
                eq("24h")
        )).thenReturn(List.of());

        when(jdbcTemplate.query(
                eq("SELECT v.id AS video_id FROM video v WHERE v.status = 'published' ORDER BY v.publish_at DESC LIMIT ?"),
                any(RowMapper.class),
                eq(6)
        )).thenReturn(List.of(publishFallback));

        List<RecommendationItemResponse> result = service.listHomeRecommendations("visitorC", 6);
        assertEquals(1, result.size());
        assertEquals(999L, result.get(0).getVideoId());
        assertEquals(0d, result.get(0).getScoreHot());
    }

        @Test
        void listHomeRecommendationsShouldKeepRankingOrderForExplainability() {
                JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
                RecommendService service = new RecommendService(jdbcTemplate);

                RecommendationItemResponse first = new RecommendationItemResponse();
                first.setVideoId(201L);
                first.setRankIndex(1);
                first.setScoreTotal(0.95d);
                first.setScoreContent(0.9d);
                first.setScoreCf(0.5d);
                first.setScoreHot(0.6d);

                RecommendationItemResponse second = new RecommendationItemResponse();
                second.setVideoId(202L);
                second.setRankIndex(2);
                second.setScoreTotal(0.80d);
                second.setScoreContent(0.7d);
                second.setScoreCf(0.4d);
                second.setScoreHot(0.5d);

                when(jdbcTemplate.query(
                                eq("SELECT version_hour FROM recommendation_result WHERE visitor_id = ? AND scene = ? ORDER BY created_at DESC LIMIT 1"),
                                any(RowMapper.class),
                                eq("visitorD"),
                                eq("home")
                )).thenReturn(List.of("2026040310"));

                when(jdbcTemplate.query(
                                argThat((String sql) -> sql != null && sql.contains("FROM recommendation_result rr") && sql.contains("ORDER BY rr.rank_index ASC")),
                                any(RowMapper.class),
                                eq("visitorD"),
                                eq("2026040310"),
                                eq(5)
                )).thenReturn(List.of(first, second));

                List<RecommendationItemResponse> result = service.listHomeRecommendations("visitorD", 5);

                assertEquals(2, result.size());
                assertEquals(201L, result.get(0).getVideoId());
                assertEquals(202L, result.get(1).getVideoId());
                assertEquals(0.95d, result.get(0).getScoreTotal());
                assertEquals(0.80d, result.get(1).getScoreTotal());
        }
}
