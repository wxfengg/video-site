package com.videosite.backend.video.dto;

public class VideoPlaySourcesResponse {

    private Long videoId;
    private String hlsMasterUrl;
    private String mp4360Url;
    private String mp4720Url;
    private String mp41080Url;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getHlsMasterUrl() {
        return hlsMasterUrl;
    }

    public void setHlsMasterUrl(String hlsMasterUrl) {
        this.hlsMasterUrl = hlsMasterUrl;
    }

    public String getMp4360Url() {
        return mp4360Url;
    }

    public void setMp4360Url(String mp4360Url) {
        this.mp4360Url = mp4360Url;
    }

    public String getMp4720Url() {
        return mp4720Url;
    }

    public void setMp4720Url(String mp4720Url) {
        this.mp4720Url = mp4720Url;
    }

    public String getMp41080Url() {
        return mp41080Url;
    }

    public void setMp41080Url(String mp41080Url) {
        this.mp41080Url = mp41080Url;
    }
}
