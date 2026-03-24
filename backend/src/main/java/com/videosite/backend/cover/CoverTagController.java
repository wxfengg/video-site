package com.videosite.backend.cover;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.cover.dto.CoverTagCorrectionRequest;
import com.videosite.backend.cover.dto.CoverTagItemResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/admin/cover-analysis")
public class CoverTagController {

    private final CoverTagService coverTagService;

    public CoverTagController(CoverTagService coverTagService) {
        this.coverTagService = coverTagService;
    }

    @PostMapping("/videos/{videoId}/tasks")
    public ApiResponse<Map<String, Object>> submitTask(@PathVariable("videoId") Long videoId,
                                                        @RequestParam(value = "analyzerType", required = false) String analyzerType) {
        Long taskId = coverTagService.submitTask(videoId, analyzerType);
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    @PostMapping("/tasks/process-next")
    public ApiResponse<Map<String, Object>> processNext() {
        boolean processed = coverTagService.processNextPendingTask();
        return ApiResponse.success(Map.of("processed", processed));
    }

    @GetMapping("/videos/{videoId}/tags")
    public ApiResponse<List<CoverTagItemResponse>> listTags(@PathVariable("videoId") Long videoId) {
        return ApiResponse.success(coverTagService.listVideoTags(videoId));
    }

    @PatchMapping("/videos/{videoId}/tags")
    public ApiResponse<String> correctTags(@PathVariable("videoId") Long videoId,
                                           @Valid @RequestBody CoverTagCorrectionRequest request) {
        coverTagService.manualCorrectTags(videoId, request.getTags());
        return ApiResponse.success("ok");
    }
}
