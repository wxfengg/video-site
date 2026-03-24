package com.videosite.backend.ab.dto;

public class AbCtrVariantReportItem {

    private String variantCode;
    private long exposureUv;
    private long clickUv;
    private double ctr;

    public String getVariantCode() {
        return variantCode;
    }

    public void setVariantCode(String variantCode) {
        this.variantCode = variantCode;
    }

    public long getExposureUv() {
        return exposureUv;
    }

    public void setExposureUv(long exposureUv) {
        this.exposureUv = exposureUv;
    }

    public long getClickUv() {
        return clickUv;
    }

    public void setClickUv(long clickUv) {
        this.clickUv = clickUv;
    }

    public double getCtr() {
        return ctr;
    }

    public void setCtr(double ctr) {
        this.ctr = ctr;
    }
}
