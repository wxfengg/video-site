package com.videosite.backend.tracking.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class TrackEventItemRequest {

    @NotBlank(message = "eventType 不能为空")
    @Size(max = 32, message = "eventType 长度不能超过32")
    private String eventType;

    private Long videoId;

    @NotNull(message = "eventTime 不能为空")
    private Long eventTime;

    @Size(max = 64, message = "sessionId 长度不能超过64")
    private String sessionId;

    @Size(max = 255, message = "pagePath 长度不能超过255")
    private String pagePath;

    private Long abExperimentId;

    @Size(max = 16, message = "abVariant 长度不能超过16")
    private String abVariant;

    private Integer progressSec;

    @Size(max = 128, message = "eventId 长度不能超过128")
    private String eventId;

    @Size(max = 1024, message = "extraJson 长度不能超过1024")
    private String extraJson;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public Long getAbExperimentId() {
        return abExperimentId;
    }

    public void setAbExperimentId(Long abExperimentId) {
        this.abExperimentId = abExperimentId;
    }

    public String getAbVariant() {
        return abVariant;
    }

    public void setAbVariant(String abVariant) {
        this.abVariant = abVariant;
    }

    public Integer getProgressSec() {
        return progressSec;
    }

    public void setProgressSec(Integer progressSec) {
        this.progressSec = progressSec;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
    }
}
