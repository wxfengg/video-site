package com.videosite.backend.recommend.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class RecommendFeedbackRequest {

    @NotNull(message = "videoId 不能为空")
    private Long videoId;

    @NotBlank(message = "action 不能为空")
    @Size(max = 32, message = "action 长度不能超过32")
    private String action;

    @Size(max = 32, message = "scene 长度不能超过32")
    private String scene;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }
}
