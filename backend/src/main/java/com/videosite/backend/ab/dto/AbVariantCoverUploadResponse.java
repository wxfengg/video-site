package com.videosite.backend.ab.dto;

public class AbVariantCoverUploadResponse {

    private String objectKey;
    private String coverUrl;

    public AbVariantCoverUploadResponse() {
    }

    public AbVariantCoverUploadResponse(String objectKey, String coverUrl) {
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
