package com.videosite.backend.dashboard.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardTrafficTrendResponse {

    private String from;
    private String to;
    private long totalPlayPv;
    private long peakDau;
    private List<TrafficPoint> points = new ArrayList<>();

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

    public long getTotalPlayPv() {
        return totalPlayPv;
    }

    public void setTotalPlayPv(long totalPlayPv) {
        this.totalPlayPv = totalPlayPv;
    }

    public long getPeakDau() {
        return peakDau;
    }

    public void setPeakDau(long peakDau) {
        this.peakDau = peakDau;
    }

    public List<TrafficPoint> getPoints() {
        return points;
    }

    public void setPoints(List<TrafficPoint> points) {
        this.points = points;
    }

    public static class TrafficPoint {
        private LocalDateTime bucketTime;
        private Integer dau;
        private Integer newUsers;
        private Integer playPv;

        public LocalDateTime getBucketTime() {
            return bucketTime;
        }

        public void setBucketTime(LocalDateTime bucketTime) {
            this.bucketTime = bucketTime;
        }

        public Integer getDau() {
            return dau;
        }

        public void setDau(Integer dau) {
            this.dau = dau;
        }

        public Integer getNewUsers() {
            return newUsers;
        }

        public void setNewUsers(Integer newUsers) {
            this.newUsers = newUsers;
        }

        public Integer getPlayPv() {
            return playPv;
        }

        public void setPlayPv(Integer playPv) {
            this.playPv = playPv;
        }
    }
}
