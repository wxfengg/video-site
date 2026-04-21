package com.videosite.backend.common.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 启动时自动加载项目根目录 .env 文件到 Spring Environment，
 * 使 ${KIMI_API_KEY} 等占位符可以解析。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 按优先级尝试几个候选路径（支持从 backend/ 或项目根目录启动）
        Path[] candidates = new Path[]{
                Paths.get(".env").toAbsolutePath().normalize(),
                Paths.get("../.env").toAbsolutePath().normalize(),
                Paths.get("../../.env").toAbsolutePath().normalize()
        };

        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                Map<String, Object> properties = loadDotenv(path);
                if (!properties.isEmpty()) {
                    // 插入到 propertySources 最前面，优先级高于 application.yml 默认值
                    environment.getPropertySources().addFirst(
                            new MapPropertySource(DOTENV_PROPERTY_SOURCE_NAME, properties)
                    );
                    return;
                }
            }
        }
    }

    private Map<String, Object> loadDotenv(Path path) {
        Map<String, Object> map = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // 去除可能的引号包裹
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        } catch (Exception ignored) {
        }
        return map;
    }
}
