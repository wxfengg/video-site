package com.videosite.backend.dashboard.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardUserGrowthResponse {

    private String from;
    private String to;
    private long totalNewUsers;
    private long currentUserTotal;
    private List<UserGrowthPoint> points = new ArrayList<>();

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

    public long getTotalNewUsers() {
        return totalNewUsers;
    }

    public void setTotalNewUsers(long totalNewUsers) {
        this.totalNewUsers = totalNewUsers;
    }

    public long getCurrentUserTotal() {
        return currentUserTotal;
    }

    public void setCurrentUserTotal(long currentUserTotal) {
        this.currentUserTotal = currentUserTotal;
    }

    public List<UserGrowthPoint> getPoints() {
        return points;
    }

    public void setPoints(List<UserGrowthPoint> points) {
        this.points = points;
    }

    public static class UserGrowthPoint {
        private LocalDate day;
        private Long newUsers;
        private Long cumulativeUsers;

        public LocalDate getDay() {
            return day;
        }

        public void setDay(LocalDate day) {
            this.day = day;
        }

        public Long getNewUsers() {
            return newUsers;
        }

        public void setNewUsers(Long newUsers) {
            this.newUsers = newUsers;
        }

        public Long getCumulativeUsers() {
            return cumulativeUsers;
        }

        public void setCumulativeUsers(Long cumulativeUsers) {
            this.cumulativeUsers = cumulativeUsers;
        }
    }
}
