package com.videosite.backend.video.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UploadCompleteRequest {

    @NotNull(message = "videoId 不能为空")
    private Long videoId;

    @NotBlank(message = "objectKey 不能为空")
    @Size(max = 512, message = "objectKey 长度不能超过 512")
    private String objectKey;

    @Size(max = 128, message = "mimeType 长度不能超过 128")
    private String mimeType;

    private Long fileSize;

    @Size(max = 64, message = "checksumSha256 长度不能超过 64")
    private String checksumSha256;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }
}
