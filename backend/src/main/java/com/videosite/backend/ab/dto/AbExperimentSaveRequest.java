package com.videosite.backend.ab.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class AbExperimentSaveRequest {

    @NotBlank(message = "name 不能为空")
    @Size(max = 128, message = "name 长度不能超过128")
    private String name;

    @NotBlank(message = "scene 不能为空")
    @Size(max = 64, message = "scene 长度不能超过64")
    private String scene;

    @NotNull(message = "targetVideoId 不能为空")
    private Long targetVideoId;

    @NotBlank(message = "metricPrimary 不能为空")
    @Size(max = 32, message = "metricPrimary 长度不能超过32")
    private String metricPrimary;

    private String startAt;

    private String endAt;

    @Valid
    @NotEmpty(message = "variants 不能为空")
    private List<AbVariantRequest> variants;

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

    public List<AbVariantRequest> getVariants() {
        return variants;
    }

    public void setVariants(List<AbVariantRequest> variants) {
        this.variants = variants;
    }
}
