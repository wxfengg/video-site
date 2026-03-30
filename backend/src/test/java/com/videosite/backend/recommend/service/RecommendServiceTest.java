package com.videosite.backend.recommend.service;

import com.videosite.backend.recommend.dto.RecommendationItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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
        )).thenReturn(List.of());

        when(jdbcTemplate.query(
                eq("SELECT v.id AS video_id FROM video v WHERE v.status = 'published' ORDER BY v.publish_at DESC LIMIT ?"),
                any(RowMapper.class),
                eq(12)
        )).thenReturn(List.of());

        List<RecommendationItemResponse> result = service.listHomeRecommendations("visitorA", 12);
        assertEquals(0, result.size());

        verify(jdbcTemplate).query(
                argThat((String sql) -> sql != null && sql.contains("FROM recommendation_result rr") && sql.contains("JOIN video v") && sql.contains("v.status = 'published'")),
                any(RowMapper.class),
                eq("visitorA"),
                eq("2026033015"),
                eq(12)
        );
    }
}
