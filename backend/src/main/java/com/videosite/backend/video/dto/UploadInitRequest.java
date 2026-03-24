package com.videosite.backend.video.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UploadInitRequest {

    @NotBlank(message = "title 不能为空")
    @Size(max = 255, message = "title 长度不能超过 255")
    private String title;

    @Size(max = 2000, message = "description 长度不能超过 2000")
    private String description;

    @NotBlank(message = "fileName 不能为空")
    @Size(max = 255, message = "fileName 长度不能超过 255")
    private String fileName;

    @NotBlank(message = "mimeType 不能为空")
    @Size(max = 128, message = "mimeType 长度不能超过 128")
    private String mimeType;

    @Min(value = 1, message = "fileSize 必须大于 0")
    private long fileSize;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
