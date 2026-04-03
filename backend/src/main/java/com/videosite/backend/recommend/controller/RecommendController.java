package com.videosite.backend.recommend.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.recommend.dto.RecommendFeedbackRequest;
import com.videosite.backend.recommend.dto.RecommendationItemResponse;
import com.videosite.backend.recommend.service.RecommendService;
import com.videosite.backend.recommend.dto.VideoHotRankItemResponse;
import com.videosite.backend.recommend.service.VideoHotRankService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;
    private final VideoHotRankService videoHotRankService;

    public RecommendController(RecommendService recommendService,
                               VideoHotRankService videoHotRankService) {
        this.recommendService = recommendService;
        this.videoHotRankService = videoHotRankService;
    }

    @GetMapping("/home")
    public ApiResponse<List<RecommendationItemResponse>> home(@RequestParam(value = "limit", defaultValue = "12") int limit,
                                                              HttpServletRequest request) {
        Object visitorAttr = request.getAttribute(AuthConstants.VISITOR_ID_ATTR);
        String visitorId = visitorAttr == null ? "anonymous" : String.valueOf(visitorAttr);
        return ApiResponse.success(recommendService.listHomeRecommendations(visitorId, Math.max(1, Math.min(limit, 50))));
    }

    @GetMapping("/hot")
    public ApiResponse<List<VideoHotRankItemResponse>> hot(@RequestParam(value = "windowType", defaultValue = "24h") String windowType,
                                                           @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.success(videoHotRankService.listLatest(windowType, limit));
    }

    @PostMapping("/feedback")
    public ApiResponse<String> feedback(@Valid @RequestBody RecommendFeedbackRequest request,
                                        HttpServletRequest servletRequest) {
        recommendService.saveFeedback(servletRequest, request);
        return ApiResponse.success("ok");
    }
}
