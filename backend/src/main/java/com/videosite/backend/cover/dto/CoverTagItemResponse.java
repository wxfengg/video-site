package com.videosite.backend.cover.dto;

public class CoverTagItemResponse {

    private String tagName;
    private String tagSource;
    private Double confidence;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagSource() {
        return tagSource;
    }

    public void setTagSource(String tagSource) {
        this.tagSource = tagSource;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
