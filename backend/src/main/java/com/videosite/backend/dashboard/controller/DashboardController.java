package com.videosite.backend.dashboard.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.dashboard.dto.DashboardOverviewResponse;
import com.videosite.backend.dashboard.dto.DashboardPlayFunnelResponse;
import com.videosite.backend.dashboard.dto.DashboardTrafficTrendResponse;
import com.videosite.backend.dashboard.dto.DashboardUserGrowthResponse;
import com.videosite.backend.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.success(dashboardService.getOverview());
    }

    @GetMapping("/traffic-trend")
    public ApiResponse<DashboardTrafficTrendResponse> trafficTrend(@RequestParam(value = "from", required = false) String from,
                                                                   @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(dashboardService.getTrafficTrend(from, to));
    }

    @GetMapping("/user-growth")
    public ApiResponse<DashboardUserGrowthResponse> userGrowth(@RequestParam(value = "from", required = false) String from,
                                                               @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(dashboardService.getUserGrowth(from, to));
    }

    @GetMapping("/play-funnel")
    public ApiResponse<DashboardPlayFunnelResponse> playFunnel(@RequestParam(value = "videoId", required = false) Long videoId,
                                                                @RequestParam(value = "from", required = false) String from,
                                                                @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(dashboardService.getPlayFunnel(videoId, from, to));
    }
}
