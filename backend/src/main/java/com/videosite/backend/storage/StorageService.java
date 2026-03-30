package com.videosite.backend.storage;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
public class StorageService {

    private final StorageProvider localProvider;

    public StorageService(LocalStorageProvider localProvider) {
        this.localProvider = localProvider;
    }

    public String activeProviderName() {
        return "local";
    }

    public String getUploadUrl(String objectKey) {
        return localProvider.getSignedUrl(objectKey, Duration.ofMinutes(30));
    }

    public void put(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            localProvider.put(objectKey, inputStream, contentLength, contentType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "上传文件到存储失败");
        }
    }

    public boolean exists(String objectKey) {
        return localProvider.exists(objectKey);
    }

    public void delete(String objectKey) {
        localProvider.delete(objectKey);
    }
}
