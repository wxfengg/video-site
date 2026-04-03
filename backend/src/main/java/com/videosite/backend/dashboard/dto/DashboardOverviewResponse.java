package com.videosite.backend.dashboard.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardOverviewResponse {

    private String from;
    private String to;
    private long playPv;
    private long peakDau;
    private long newUsers;
    private long publishedVideos;
    private long runningExperiments;
    private long totalComments;
    private long totalLikes;
    private List<HotVideoItem> hotVideos = new ArrayList<>();
    private List<AbSummaryItem> abSummary = new ArrayList<>();

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

    public long getPlayPv() {
        return playPv;
    }

    public void setPlayPv(long playPv) {
        this.playPv = playPv;
    }

    public long getPeakDau() {
        return peakDau;
    }

    public void setPeakDau(long peakDau) {
        this.peakDau = peakDau;
    }

    public long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }

    public long getPublishedVideos() {
        return publishedVideos;
    }

    public void setPublishedVideos(long publishedVideos) {
        this.publishedVideos = publishedVideos;
    }

    public long getRunningExperiments() {
        return runningExperiments;
    }

    public void setRunningExperiments(long runningExperiments) {
        this.runningExperiments = runningExperiments;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public List<HotVideoItem> getHotVideos() {
        return hotVideos;
    }

    public void setHotVideos(List<HotVideoItem> hotVideos) {
        this.hotVideos = hotVideos;
    }

    public List<AbSummaryItem> getAbSummary() {
        return abSummary;
    }

    public void setAbSummary(List<AbSummaryItem> abSummary) {
        this.abSummary = abSummary;
    }

    public static class HotVideoItem {
        private Long videoId;
        private Integer rankIndex;
        private Double hotScore;
        private String title;
        private String coverUrl;

        public Long getVideoId() {
            return videoId;
        }

        public void setVideoId(Long videoId) {
            this.videoId = videoId;
        }

        public Integer getRankIndex() {
            return rankIndex;
        }

        public void setRankIndex(Integer rankIndex) {
            this.rankIndex = rankIndex;
        }

        public Double getHotScore() {
            return hotScore;
        }

        public void setHotScore(Double hotScore) {
            this.hotScore = hotScore;
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
    }

    public static class AbSummaryItem {
        private Long experimentId;
        private String experimentName;
        private String variantCode;
        private Long exposureUv;
        private Long clickUv;
        private Double ctr;

        public Long getExperimentId() {
            return experimentId;
        }

        public void setExperimentId(Long experimentId) {
            this.experimentId = experimentId;
        }

        public String getExperimentName() {
            return experimentName;
        }

        public void setExperimentName(String experimentName) {
            this.experimentName = experimentName;
        }

        public String getVariantCode() {
            return variantCode;
        }

        public void setVariantCode(String variantCode) {
            this.variantCode = variantCode;
        }

        public Long getExposureUv() {
            return exposureUv;
        }

        public void setExposureUv(Long exposureUv) {
            this.exposureUv = exposureUv;
        }

        public Long getClickUv() {
            return clickUv;
        }

        public void setClickUv(Long clickUv) {
            this.clickUv = clickUv;
        }

        public Double getCtr() {
            return ctr;
        }

        public void setCtr(Double ctr) {
            this.ctr = ctr;
        }
    }
}
