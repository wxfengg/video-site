package com.videosite.backend.storage;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Component
public class OssStorageProvider implements StorageProvider {

    @Override
    public String providerName() {
        return "oss";
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long contentLength, String contentType) throws IOException {
        throw new UnsupportedOperationException("OSS 存储暂未启用，请将 STORAGE_PROVIDER 设置为 local");
    }

    @Override
    public String getSignedUrl(String objectKey, Duration expireIn) {
        throw new UnsupportedOperationException("OSS 存储暂未启用，请将 STORAGE_PROVIDER 设置为 local");
    }

    @Override
    public void delete(String objectKey) {
        throw new UnsupportedOperationException("OSS 存储暂未启用，请将 STORAGE_PROVIDER 设置为 local");
    }

    @Override
    public boolean exists(String objectKey) {
        throw new UnsupportedOperationException("OSS 存储暂未启用，请将 STORAGE_PROVIDER 设置为 local");
    }
}
