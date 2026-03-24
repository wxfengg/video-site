package com.videosite.backend.controller.dto;

public class AdminSessionResponse {

    private boolean loggedIn;
    private String username;

    public AdminSessionResponse() {
    }

    public AdminSessionResponse(boolean loggedIn, String username) {
        this.loggedIn = loggedIn;
        this.username = username;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
