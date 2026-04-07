package com.videosite.backend.user.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminUserStatusUpdateRequest {

    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 仅支持 0 或 1")
    @Max(value = 1, message = "status 仅支持 0 或 1")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}