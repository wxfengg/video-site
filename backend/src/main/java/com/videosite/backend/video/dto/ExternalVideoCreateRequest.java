package com.videosite.backend.video.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ExternalVideoCreateRequest {

    @NotBlank(message = "title 不能为空")
    @Size(max = 255, message = "title 长度不能超过255")
    private String title;

    @Size(max = 2000, message = "description 长度不能超过2000")
    private String description;

    @Size(max = 512, message = "coverUrl 长度不能超过512")
    private String coverUrl;

    @NotBlank(message = "sourceProtocol 不能为空")
    @Pattern(regexp = "^(?i)(mp4|hls)$", message = "sourceProtocol 仅支持 mp4 或 hls")
    private String sourceProtocol;

    @NotBlank(message = "sourceUrl 不能为空")
    @Size(max = 1024, message = "sourceUrl 长度不能超过1024")
    @Pattern(regexp = "^https?://.+", message = "sourceUrl 必须是 http/https 直链")
    private String sourceUrl;

    @Min(value = 1, message = "durationSec 必须大于0")
    private Integer durationSec;

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

    public String getSourceProtocol() {
        return sourceProtocol;
    }

    public void setSourceProtocol(String sourceProtocol) {
        this.sourceProtocol = sourceProtocol;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }
}
