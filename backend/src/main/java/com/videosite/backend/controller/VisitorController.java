package com.videosite.backend.controller;

import com.videosite.backend.common.api.ApiResponse;
import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.controller.dto.VisitorInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/visitor")
public class VisitorController {

    @GetMapping("/me")
    public ApiResponse<VisitorInfoResponse> me(HttpServletRequest request) {
        Object visitorId = request.getAttribute(AuthConstants.VISITOR_ID_ATTR);
        return ApiResponse.success(new VisitorInfoResponse(visitorId == null ? null : String.valueOf(visitorId)));
    }
}
