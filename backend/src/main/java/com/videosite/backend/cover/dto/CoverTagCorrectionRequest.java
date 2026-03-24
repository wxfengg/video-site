package com.videosite.backend.cover.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

public class CoverTagCorrectionRequest {

    @NotEmpty(message = "tags 不能为空")
    private List<@Size(max = 64, message = "标签长度不能超过64") String> tags;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
