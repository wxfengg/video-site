package com.videosite.backend.video.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VideoDetailResponse {

    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private Integer durationSec;
    private String status;
    private LocalDateTime publishAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String aiSummary;
    private List<String> aiTags;
    private List<String> aiCategories;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(LocalDateTime publishAt) {
        this.publishAt = publishAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public List<String> getAiTags() {
        return aiTags;
    }

    public void setAiTags(List<String> aiTags) {
        this.aiTags = aiTags;
    }

    public List<String> getAiCategories() {
        return aiCategories;
    }

    public void setAiCategories(List<String> aiCategories) {
        this.aiCategories = aiCategories;
    }
}
