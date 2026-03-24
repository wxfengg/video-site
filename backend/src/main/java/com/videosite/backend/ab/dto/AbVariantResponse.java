package com.videosite.backend.ab.dto;

public class AbVariantResponse {

    private Long id;
    private String variantCode;
    private String coverUrl;
    private Integer trafficRatio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public void setVariantCode(String variantCode) {
        this.variantCode = variantCode;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getTrafficRatio() {
        return trafficRatio;
    }

    public void setTrafficRatio(Integer trafficRatio) {
        this.trafficRatio = trafficRatio;
    }
}
