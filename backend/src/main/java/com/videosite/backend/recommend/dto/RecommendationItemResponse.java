package com.videosite.backend.recommend.dto;

public class RecommendationItemResponse {

    private Long videoId;
    private Integer rankIndex;
    private Double scoreTotal;
    private Double scoreContent;
    private Double scoreCf;
    private Double scoreHot;

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
}
