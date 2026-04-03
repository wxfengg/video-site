package com.videosite.backend.dashboard.service;

import com.videosite.backend.dashboard.dto.DashboardOverviewResponse;
import com.videosite.backend.dashboard.dto.DashboardPlayFunnelResponse;
import com.videosite.backend.dashboard.dto.DashboardTrafficTrendResponse;
import com.videosite.backend.dashboard.dto.DashboardUserGrowthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class DashboardServiceResilienceTest {

    @Test
    void shouldFallbackToZeroWhenMetricsTablesUnavailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DashboardService service = new DashboardService(jdbcTemplate);

        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("table missing");

        doThrow(ex).when(jdbcTemplate)
                .queryForObject(anyString(), any(RowMapper.class), any(Object[].class));
        doThrow(ex).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Long.class), any(Object[].class));
        doThrow(ex).when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        DashboardOverviewResponse overview = service.getOverview();
        assertEquals(0L, overview.getPlayPv());
        assertEquals(0L, overview.getPeakDau());
        assertEquals(0L, overview.getNewUsers());
        assertNotNull(overview.getHotVideos());
        assertTrue(overview.getHotVideos().isEmpty());

        DashboardTrafficTrendResponse traffic = service.getTrafficTrend("2026-04-01", "2026-04-01");
        assertEquals(0L, traffic.getTotalPlayPv());
        assertEquals(0L, traffic.getPeakDau());
        assertNotNull(traffic.getPoints());
        assertTrue(traffic.getPoints().isEmpty());

        DashboardUserGrowthResponse growth = service.getUserGrowth("2026-04-01", "2026-04-01");
        assertEquals(0L, growth.getTotalNewUsers());
        assertEquals(0L, growth.getCurrentUserTotal());
        assertEquals(1, growth.getPoints().size());
        assertEquals(0L, growth.getPoints().get(0).getNewUsers());

        DashboardPlayFunnelResponse funnel = service.getPlayFunnel(1001L, "2026-04-01", "2026-04-01");
        assertEquals(0L, funnel.getExposureUv());
        assertEquals(0L, funnel.getClickUv());
        assertEquals(0D, funnel.getCtr());
    }
}
