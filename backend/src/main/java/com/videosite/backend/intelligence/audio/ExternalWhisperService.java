package com.videosite.backend.intelligence.audio;

import com.videosite.backend.intelligence.IntelligenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ExternalWhisperService implements AudioTranscriptService {

    private static final Logger log = LoggerFactory.getLogger(ExternalWhisperService.class);

    private final IntelligenceProperties intelligenceProperties;

    @Value("${app.transcode.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${app.storage.local-root:../}")
    private String localStorageRoot;

    public ExternalWhisperService(IntelligenceProperties intelligenceProperties) {
        this.intelligenceProperties = intelligenceProperties;
    }

    @Override
    public String transcript(String videoFilePath) {
        Path wavPath = null;
        try {
            wavPath = extractAudio(videoFilePath);
            if (wavPath == null || !Files.exists(wavPath)) {
                log.warn("音频提取失败: {}", videoFilePath);
                return "";
            }
            return runWhisper(wavPath);
        } catch (Exception ex) {
            log.error("Whisper 转录失败: {}", videoFilePath, ex);
            return "";
        } finally {
            if (wavPath != null) {
                try {
                    Files.deleteIfExists(wavPath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Path extractAudio(String videoFilePath) throws Exception {
        Path inputPath = Paths.get(videoFilePath);
        if (!Files.exists(inputPath)) {
            return null;
        }

        Path tempDir = Paths.get(localStorageRoot).toAbsolutePath().normalize()
                .resolve("temp/audio");
        Files.createDirectories(tempDir);

        String fileName = UUID.randomUUID() + "_audio.wav";
        Path outputPath = tempDir.resolve(fileName);

        int maxDuration = intelligenceProperties.getAudio().getMaxDurationSec();

        List<String> command = new ArrayList<>();
        command.add(ffmpegBin);
        command.add("-y");
        command.add("-i");
        command.add(videoFilePath);
        command.add("-vn");
        command.add("-ar");
        command.add("16000");
        command.add("-ac");
        command.add("1");
        command.add("-c:a");
        command.add("pcm_s16le");
        command.add("-t");
        command.add(String.valueOf(maxDuration));
        command.add(outputPath.toString());

        runCommand(command);
        return outputPath;
    }

    private String runWhisper(Path wavPath) throws Exception {
        String scriptPath = resolveScriptPath(intelligenceProperties.getAudio().getWhisperScript());
        String model = intelligenceProperties.getAudio().getWhisperModel();

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(scriptPath);
        command.add("--audio");
        command.add(wavPath.toString());
        command.add("--model");
        command.add(model);
        command.add("--language");
        command.add("zh");

        StringBuilder output = new StringBuilder();
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("Whisper 脚本退出码非零: {}, output={}", exitCode, output);
        }

        return output.toString().trim();
    }

    private String resolveScriptPath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return configuredPath;
        }
        Path path = Paths.get(configuredPath);
        if (path.isAbsolute() || Files.exists(path)) {
            return configuredPath;
        }
        // 尝试基于 localStorageRoot（项目根目录）解析相对路径
        try {
            Path root = Paths.get(localStorageRoot).toAbsolutePath().normalize();
            Path resolved = root.resolve(configuredPath).normalize();
            if (Files.exists(resolved)) {
                return resolved.toString();
            }
        } catch (Exception ignored) {
        }
        return configuredPath;
    }

    private void runCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                // discard ffmpeg logs
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("FFmpeg 音频提取失败，exitCode=" + exitCode);
        }
    }
}
