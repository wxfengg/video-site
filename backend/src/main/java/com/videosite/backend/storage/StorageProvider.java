package com.videosite.backend.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public interface StorageProvider {

    String providerName();

    void put(String objectKey, InputStream inputStream, long contentLength, String contentType) throws IOException;

    String getSignedUrl(String objectKey, Duration expireIn);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
