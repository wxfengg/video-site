package com.videosite.backend.storage;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private final String activeProviderName;
    private final Map<String, StorageProvider> providerMap;

    public StorageService(@Value("${app.storage.provider:local}") String activeProviderName,
                          List<StorageProvider> providers) {
        this.activeProviderName = activeProviderName;
        this.providerMap = providers.stream().collect(Collectors.toMap(StorageProvider::providerName, Function.identity()));
    }

    public String activeProviderName() {
        return activeProviderName;
    }

    public String getUploadUrl(String objectKey) {
        return provider().getSignedUrl(objectKey, Duration.ofMinutes(30));
    }

    public void put(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            provider().put(objectKey, inputStream, contentLength, contentType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "上传文件到存储失败");
        }
    }

    public boolean exists(String objectKey) {
        return provider().exists(objectKey);
    }

    public void delete(String objectKey) {
        provider().delete(objectKey);
    }

    private StorageProvider provider() {
        StorageProvider provider = providerMap.get(activeProviderName);
        if (provider == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未知存储提供者: " + activeProviderName);
        }
        return provider;
    }
}
