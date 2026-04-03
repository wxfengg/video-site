package com.videosite.backend.video.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class VideoCommentCreateRequest {

    @NotBlank(message = "content 不能为空")
    @Size(max = 1000, message = "content 长度不能超过1000")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
