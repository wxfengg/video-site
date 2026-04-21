package com.videosite.backend.intelligence;

import com.videosite.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/intelligence")
public class VideoIntelligenceController {

    private final VideoIntelligenceService videoIntelligenceService;

    public VideoIntelligenceController(VideoIntelligenceService videoIntelligenceService) {
        this.videoIntelligenceService = videoIntelligenceService;
    }

    @PostMapping("/videos/{videoId}/reanalyze")
    public ApiResponse<Map<String, Object>> reanalyze(@PathVariable Long videoId) {
        Long taskId = videoIntelligenceService.submitTask(videoId);
        return ApiResponse.success(Map.of("taskId", taskId != null ? taskId : -1, "message", "已提交重新分析任务"));
    }
}
