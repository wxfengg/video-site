package com.videosite.backend.video.dto;

import javax.validation.constraints.Size;

public class VideoUpdateRequest {

    @Size(max = 255, message = "title 长度不能超过 255")
    private String title;

    @Size(max = 2000, message = "description 长度不能超过 2000")
    private String description;

    @Size(max = 512, message = "coverUrl 长度不能超过 512")
    private String coverUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
