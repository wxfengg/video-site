package com.videosite.backend.ab.controller;

import com.videosite.backend.ab.dto.AbAssignmentResponse;
import com.videosite.backend.ab.dto.AbCtrReportResponse;
import com.videosite.backend.ab.dto.AbExperimentResponse;
import com.videosite.backend.ab.dto.AbExperimentSaveRequest;
import com.videosite.backend.ab.service.AbExperimentService;
import com.videosite.backend.ab.service.AbReportService;
import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.auth.AuthConstants;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class AbExperimentController {

    private final AbExperimentService abExperimentService;
    private final AbReportService abReportService;

    public AbExperimentController(AbExperimentService abExperimentService,
                                  AbReportService abReportService) {
        this.abExperimentService = abExperimentService;
        this.abReportService = abReportService;
    }

    @GetMapping("/api/admin/ab/experiments")
    public ApiResponse<List<AbExperimentResponse>> listExperiments() {
        return ApiResponse.success(abExperimentService.listExperiments());
    }

    @PostMapping("/api/admin/ab/experiments")
    public ApiResponse<AbExperimentResponse> createExperiment(@Valid @RequestBody AbExperimentSaveRequest request) {
        return ApiResponse.success(abExperimentService.createExperiment(request));
    }

    @PatchMapping("/api/admin/ab/experiments/{experimentId}")
    public ApiResponse<AbExperimentResponse> updateExperiment(@PathVariable("experimentId") Long experimentId,
                                                              @Valid @RequestBody AbExperimentSaveRequest request) {
        return ApiResponse.success(abExperimentService.updateExperiment(experimentId, request));
    }

    @PostMapping("/api/admin/ab/experiments/{experimentId}/start")
    public ApiResponse<AbExperimentResponse> start(@PathVariable("experimentId") Long experimentId) {
        return ApiResponse.success(abExperimentService.startExperiment(experimentId));
    }

    @PostMapping("/api/admin/ab/experiments/{experimentId}/stop")
    public ApiResponse<AbExperimentResponse> stop(@PathVariable("experimentId") Long experimentId) {
        return ApiResponse.success(abExperimentService.stopExperiment(experimentId));
    }

    @GetMapping("/api/admin/ab/experiments/{experimentId}/ctr-report")
    public ApiResponse<AbCtrReportResponse> ctrReport(@PathVariable("experimentId") Long experimentId) {
        return ApiResponse.success(abReportService.getCtrReport(experimentId));
    }

    @GetMapping("/api/ab/assignment")
    public ApiResponse<AbAssignmentResponse> assignment(@RequestParam("scene") String scene,
                                                        @RequestParam(value = "targetVideoId", required = false) Long targetVideoId,
                                                        HttpServletRequest request) {
        Object visitorAttr = request.getAttribute(AuthConstants.VISITOR_ID_ATTR);
        String visitorId = visitorAttr == null ? "anonymous" : String.valueOf(visitorAttr);
        return ApiResponse.success(abExperimentService.assign(visitorId, scene, targetVideoId));
    }
}
