package com.videosite.backend.controller.dto;

public class VisitorInfoResponse {

    private String visitorId;

    public VisitorInfoResponse() {
    }

    public VisitorInfoResponse(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }
}
