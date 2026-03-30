package com.videosite.backend.video.dto;

public class UploadCoverResponse {

    private String objectKey;
    private String coverUrl;

    public UploadCoverResponse() {
    }

    public UploadCoverResponse(String objectKey, String coverUrl) {
        this.objectKey = objectKey;
        this.coverUrl = coverUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
