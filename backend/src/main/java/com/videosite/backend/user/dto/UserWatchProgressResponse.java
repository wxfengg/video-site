package com.videosite.backend.user.dto;

import java.time.LocalDateTime;

public class UserWatchProgressResponse {

    private Long videoId;
    private Integer progressSec;
    private Integer durationSecSnapshot;
    private Double completionRate;
    private boolean completed90;
    private LocalDateTime lastWatchedAt;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Integer getProgressSec() {
        return progressSec;
    }

    public void setProgressSec(Integer progressSec) {
        this.progressSec = progressSec;
    }

    public Integer getDurationSecSnapshot() {
        return durationSecSnapshot;
    }

    public void setDurationSecSnapshot(Integer durationSecSnapshot) {
        this.durationSecSnapshot = durationSecSnapshot;
    }

    public Double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }

    public boolean isCompleted90() {
        return completed90;
    }

    public void setCompleted90(boolean completed90) {
        this.completed90 = completed90;
    }

    public LocalDateTime getLastWatchedAt() {
        return lastWatchedAt;
    }

    public void setLastWatchedAt(LocalDateTime lastWatchedAt) {
        this.lastWatchedAt = lastWatchedAt;
    }
}
