package com.videosite.backend.dashboard.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardPlayFunnelResponse {

    private Long videoId;
    private String from;
    private String to;
    private long exposureUv;
    private long clickUv;
    private long playUv;
    private long completeUv;
    private double ctr;
    private double playThroughRate;
    private double completionRate;
    private List<FunnelStage> stages = new ArrayList<>();

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
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

    public long getPlayUv() {
        return playUv;
    }

    public void setPlayUv(long playUv) {
        this.playUv = playUv;
    }

    public long getCompleteUv() {
        return completeUv;
    }

    public void setCompleteUv(long completeUv) {
        this.completeUv = completeUv;
    }

    public double getCtr() {
        return ctr;
    }

    public void setCtr(double ctr) {
        this.ctr = ctr;
    }

    public double getPlayThroughRate() {
        return playThroughRate;
    }

    public void setPlayThroughRate(double playThroughRate) {
        this.playThroughRate = playThroughRate;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public List<FunnelStage> getStages() {
        return stages;
    }

    public void setStages(List<FunnelStage> stages) {
        this.stages = stages;
    }

    public static class FunnelStage {
        private String stage;
        private Long uv;

        public FunnelStage() {
        }

        public FunnelStage(String stage, Long uv) {
            this.stage = stage;
            this.uv = uv;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public Long getUv() {
            return uv;
        }

        public void setUv(Long uv) {
            this.uv = uv;
        }
    }
}
