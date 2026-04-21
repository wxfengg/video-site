package com.videosite.backend.recommend.dto;

public class RecommendationItemResponse {

    private Long videoId;
    private Integer rankIndex;
    private Double scoreTotal;
    private Double scoreContent;
    private Double scoreCf;
    private Double scoreHot;
    private String title;
    private String coverUrl;
    private Integer durationSec;
    private String recommendReason;

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

    public Double getScoreTotal() {
        return scoreTotal;
    }

    public void setScoreTotal(Double scoreTotal) {
        this.scoreTotal = scoreTotal;
    }

    public Double getScoreContent() {
        return scoreContent;
    }

    public void setScoreContent(Double scoreContent) {
        this.scoreContent = scoreContent;
    }

    public Double getScoreCf() {
        return scoreCf;
    }

    public void setScoreCf(Double scoreCf) {
        this.scoreCf = scoreCf;
    }

    public Double getScoreHot() {
        return scoreHot;
    }

    public void setScoreHot(Double scoreHot) {
        this.scoreHot = scoreHot;
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

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public String getRecommendReason() {
        return recommendReason;
    }

    public void setRecommendReason(String recommendReason) {
        this.recommendReason = recommendReason;
    }
}
