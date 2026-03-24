package com.videosite.backend.ab.dto;

import java.util.List;

public class AbExperimentResponse {

    private Long id;
    private String name;
    private String scene;
    private Long targetVideoId;
    private String status;
    private String metricPrimary;
    private String startAt;
    private String endAt;
    private List<AbVariantResponse> variants;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Long getTargetVideoId() {
        return targetVideoId;
    }

    public void setTargetVideoId(Long targetVideoId) {
        this.targetVideoId = targetVideoId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetricPrimary() {
        return metricPrimary;
    }

    public void setMetricPrimary(String metricPrimary) {
        this.metricPrimary = metricPrimary;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public List<AbVariantResponse> getVariants() {
        return variants;
    }

    public void setVariants(List<AbVariantResponse> variants) {
        this.variants = variants;
    }
}
