package com.videosite.backend.ab.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AbVariantRequest {

    @NotBlank(message = "variantCode 不能为空")
    @Size(max = 16, message = "variantCode 长度不能超过16")
    private String variantCode;

    @Size(max = 512, message = "coverUrl 长度不能超过512")
    private String coverUrl;

    @Min(value = 1, message = "trafficRatio 最小为1")
    @Max(value = 100, message = "trafficRatio 最大为100")
    private Integer trafficRatio;

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
