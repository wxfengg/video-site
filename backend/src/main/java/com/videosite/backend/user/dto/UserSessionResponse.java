package com.videosite.backend.user.dto;

public class UserSessionResponse {

    private boolean loggedIn;
    private Long userId;
    private String username;

    public UserSessionResponse() {
    }

    public UserSessionResponse(boolean loggedIn, Long userId, String username) {
        this.loggedIn = loggedIn;
        this.userId = userId;
        this.username = username;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
