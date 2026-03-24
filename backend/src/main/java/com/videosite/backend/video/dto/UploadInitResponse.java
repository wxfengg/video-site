package com.videosite.backend.video.dto;

public class UploadInitResponse {

    private Long videoId;
    private String storageProvider;
    private String objectKey;
    private String uploadUrl;

    public UploadInitResponse() {
    }

    public UploadInitResponse(Long videoId, String storageProvider, String objectKey, String uploadUrl) {
        this.videoId = videoId;
        this.storageProvider = storageProvider;
        this.objectKey = objectKey;
        this.uploadUrl = uploadUrl;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
}
