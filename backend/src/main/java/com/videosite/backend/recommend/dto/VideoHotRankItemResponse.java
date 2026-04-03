package com.videosite.backend.recommend.dto;

import java.time.LocalDateTime;

public class VideoHotRankItemResponse {

    private String windowType;
    private LocalDateTime bucketTime;
    private Long videoId;
    private Integer rankIndex;
    private Double hotScore;
    private String title;
    private String coverUrl;
    private Integer durationSec;
    private LocalDateTime publishAt;

    public String getWindowType() {
        return windowType;
    }

    public void setWindowType(String windowType) {
        this.windowType = windowType;
    }

    public LocalDateTime getBucketTime() {
        return bucketTime;
    }

    public void setBucketTime(LocalDateTime bucketTime) {
        this.bucketTime = bucketTime;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Integer getRankIndex() {
        return rankIndex;
    }

    public void setRankIndex(Integer rankIndex) {
        this.rankIndex = rankIndex;
    }

    public Double getHotScore() {
        return hotScore;
    }

    public void setHotScore(Double hotScore) {
        this.hotScore = hotScore;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public LocalDateTime getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(LocalDateTime publishAt) {
        this.publishAt = publishAt;
    }
}
