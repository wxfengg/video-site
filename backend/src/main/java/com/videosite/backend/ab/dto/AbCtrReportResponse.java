package com.videosite.backend.ab.dto;

import java.util.List;

public class AbCtrReportResponse {

    private Long experimentId;
    private String metricPrimary;
    private List<AbCtrVariantReportItem> variants;

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public String getMetricPrimary() {
        return metricPrimary;
    }

    public void setMetricPrimary(String metricPrimary) {
        this.metricPrimary = metricPrimary;
    }

    public List<AbCtrVariantReportItem> getVariants() {
        return variants;
    }

    public void setVariants(List<AbCtrVariantReportItem> variants) {
        this.variants = variants;
    }
}
