package com.videosite.backend.user.dto;

import java.time.LocalDateTime;

public class UserWatchHistoryItemResponse {

    private Long id;
    private String title;
    private String coverUrl;
    private String status;
    private Integer durationSec;
    private Integer lastProgressSec;
    private Integer durationSecSnapshot;
    private Double completionRate;
    private boolean completed90;
    private LocalDateTime lastWatchedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public Integer getLastProgressSec() {
        return lastProgressSec;
    }

    public void setLastProgressSec(Integer lastProgressSec) {
        this.lastProgressSec = lastProgressSec;
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
