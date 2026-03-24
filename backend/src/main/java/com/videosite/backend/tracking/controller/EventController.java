package com.videosite.backend.tracking.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.tracking.dto.TrackEventBatchRequest;
import com.videosite.backend.tracking.dto.TrackEventBatchResponse;
import com.videosite.backend.tracking.service.EventService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/batch")
    public ApiResponse<TrackEventBatchResponse> batch(@Valid @RequestBody TrackEventBatchRequest request,
                                                      HttpServletRequest servletRequest) {
        Object visitorAttr = servletRequest.getAttribute(AuthConstants.VISITOR_ID_ATTR);
        String visitorId = visitorAttr == null ? "anonymous" : String.valueOf(visitorAttr);
        int stored = eventService.collectBatch(visitorId, request);
        return ApiResponse.success(new TrackEventBatchResponse(request.getEvents().size(), stored));
    }
}
