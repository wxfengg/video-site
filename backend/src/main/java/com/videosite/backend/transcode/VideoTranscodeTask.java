package com.videosite.backend.transcode;

public class VideoTranscodeTask {

    private Long id;
    private Long videoId;
    private Integer retryCount;

    public VideoTranscodeTask() {
    }

    public VideoTranscodeTask(Long id, Long videoId, Integer retryCount) {
        this.id = id;
        this.videoId = videoId;
        this.retryCount = retryCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
}
