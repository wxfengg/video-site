package com.videosite.backend.user.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminUserCreateRequest {

    @NotBlank(message = "username 不能为空")
    @Size(min = 3, max = 64, message = "username 长度需在 3 到 64 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username 仅支持字母、数字和下划线")
    private String username;

    @NotBlank(message = "password 不能为空")
    @Size(min = 6, max = 64, message = "password 长度需在 6 到 64 之间")
    private String password;

    @Min(value = 0, message = "status 仅支持 0 或 1")
    @Max(value = 1, message = "status 仅支持 0 或 1")
    private Integer status = 1;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}