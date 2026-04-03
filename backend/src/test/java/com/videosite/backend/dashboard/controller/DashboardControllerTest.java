package com.videosite.backend.dashboard.controller;

import com.videosite.backend.dashboard.dto.DashboardOverviewResponse;
import com.videosite.backend.dashboard.dto.DashboardPlayFunnelResponse;
import com.videosite.backend.dashboard.dto.DashboardTrafficTrendResponse;
import com.videosite.backend.dashboard.dto.DashboardUserGrowthResponse;
import com.videosite.backend.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void overviewShouldReturnSummary() throws Exception {
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setPlayPv(123);
        response.setPeakDau(45);
        response.setNewUsers(6);
        response.setPublishedVideos(7);
        response.setRunningExperiments(1);

        DashboardOverviewResponse.HotVideoItem hotVideo = new DashboardOverviewResponse.HotVideoItem();
        hotVideo.setVideoId(11L);
        hotVideo.setRankIndex(1);
        hotVideo.setHotScore(98.7);
        hotVideo.setTitle("春日散步");
        response.setHotVideos(List.of(hotVideo));

        when(dashboardService.getOverview()).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playPv").value(123))
                .andExpect(jsonPath("$.data.hotVideos[0].videoId").value(11));
    }

    @Test
    void trafficTrendShouldAcceptRangeQuery() throws Exception {
        DashboardTrafficTrendResponse response = new DashboardTrafficTrendResponse();
        response.setTotalPlayPv(200);
        response.setPeakDau(40);

        DashboardTrafficTrendResponse.TrafficPoint point = new DashboardTrafficTrendResponse.TrafficPoint();
        point.setBucketTime(LocalDateTime.parse("2026-04-03T09:00:00"));
        point.setPlayPv(100);
        point.setDau(20);
        point.setNewUsers(3);
        response.setPoints(List.of(point));

        when(dashboardService.getTrafficTrend(eq("2026-04-01"), eq("2026-04-03"))).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/traffic-trend")
                        .queryParam("from", "2026-04-01")
                        .queryParam("to", "2026-04-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalPlayPv").value(200));
    }

    @Test
    void userGrowthShouldReturnDailyPoints() throws Exception {
        DashboardUserGrowthResponse response = new DashboardUserGrowthResponse();
        response.setTotalNewUsers(12);
        response.setCurrentUserTotal(34);

        DashboardUserGrowthResponse.UserGrowthPoint point = new DashboardUserGrowthResponse.UserGrowthPoint();
        point.setDay(LocalDate.parse("2026-04-03"));
        point.setNewUsers(5L);
        point.setCumulativeUsers(34L);
        response.setPoints(List.of(point));

        when(dashboardService.getUserGrowth(eq("2026-04-01"), eq("2026-04-03"))).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                        .queryParam("from", "2026-04-01")
                        .queryParam("to", "2026-04-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalNewUsers").value(12));
    }

    @Test
    void playFunnelShouldReturnRates() throws Exception {
        DashboardPlayFunnelResponse response = new DashboardPlayFunnelResponse();
        response.setVideoId(7L);
        response.setExposureUv(100);
        response.setClickUv(25);
        response.setPlayUv(20);
        response.setCompleteUv(8);
        response.setCtr(0.25);
        response.setCompletionRate(0.4);

        when(dashboardService.getPlayFunnel(eq(7L), eq("2026-04-01"), eq("2026-04-03"))).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/play-funnel")
                        .queryParam("videoId", "7")
                        .queryParam("from", "2026-04-01")
                        .queryParam("to", "2026-04-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.videoId").value(7))
                .andExpect(jsonPath("$.data.ctr").value(0.25));
    }
}
