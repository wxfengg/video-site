package com.videosite.backend.video.dto;

public class UploadCompleteResponse {

    private Long videoId;
    private Long videoFileId;
    private Long transcodeTaskId;
    private String status;

    public UploadCompleteResponse() {
    }

    public UploadCompleteResponse(Long videoId, Long videoFileId, Long transcodeTaskId, String status) {
        this.videoId = videoId;
        this.videoFileId = videoFileId;
        this.transcodeTaskId = transcodeTaskId;
        this.status = status;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getVideoFileId() {
        return videoFileId;
    }

    public void setVideoFileId(Long videoFileId) {
        this.videoFileId = videoFileId;
    }

    public Long getTranscodeTaskId() {
        return transcodeTaskId;
    }

    public void setTranscodeTaskId(Long transcodeTaskId) {
        this.transcodeTaskId = transcodeTaskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
