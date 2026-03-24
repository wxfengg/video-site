package com.videosite.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Component
public class LocalStorageProvider implements StorageProvider {

    private final Path rootPath;

    public LocalStorageProvider(@Value("${app.storage.local-root:./data/storage}") String localRoot) {
        this.rootPath = Paths.get(localRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化本地存储目录: " + this.rootPath, e);
        }
    }

    @Override
    public String providerName() {
        return "local";
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long contentLength, String contentType) throws IOException {
        Path target = resolvePath(objectKey);
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public String getSignedUrl(String objectKey, Duration expireIn) {
        String normalizedKey = normalizeObjectKey(objectKey);
        return "/api/storage/" + normalizedKey;
    }

    @Override
    public void delete(String objectKey) {
        Path target = resolvePath(objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("删除文件失败: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path target = resolvePath(objectKey);
        return Files.exists(target);
    }

    private Path resolvePath(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        Path resolved = rootPath.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IllegalArgumentException("非法 objectKey: " + objectKey);
        }
        return resolved;
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("objectKey 不能为空");
        }

        String normalizedKey = objectKey.replace("\\", "/");
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }
        return normalizedKey;
    }
}
