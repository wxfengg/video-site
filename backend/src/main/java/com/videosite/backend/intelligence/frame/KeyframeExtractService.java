package com.videosite.backend.intelligence.frame;

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

@Component
public class KeyframeExtractService {

    private static final Logger log = LoggerFactory.getLogger(KeyframeExtractService.class);

    private final IntelligenceProperties intelligenceProperties;

    @Value("${app.transcode.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${app.storage.local-root:../}")
    private String localStorageRoot;

    public KeyframeExtractService(IntelligenceProperties intelligenceProperties) {
        this.intelligenceProperties = intelligenceProperties;
    }

    public List<String> extractKeyframes(String videoFilePath, Long videoId) {
        if (!intelligenceProperties.getFrame().isEnabled()) {
            return new ArrayList<>();
        }

        try {
            Path inputPath = Paths.get(videoFilePath);
            if (!Files.exists(inputPath)) {
                log.warn("视频文件不存在: {}", videoFilePath);
                return new ArrayList<>();
            }

            double duration = getVideoDuration(videoFilePath);
            if (duration <= 0) {
                log.warn("无法获取视频时长: {}", videoFilePath);
                return new ArrayList<>();
            }

            Path outputDir = Paths.get(localStorageRoot).toAbsolutePath().normalize()
                    .resolve("temp/keyframes/" + videoId);
            Files.createDirectories(outputDir);

            int count = intelligenceProperties.getFrame().getCount();
            int scaleWidth = intelligenceProperties.getFrame().getScaleWidth();
            List<String> result = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                double ratio = (i + 1.0) / (count + 1.0);
                int second = (int) (duration * ratio);
                String outputName = "keyframe_" + (i + 1) + ".jpg";
                Path outputPath = outputDir.resolve(outputName);

                List<String> command = new ArrayList<>();
                command.add(ffmpegBin);
                command.add("-y");
                command.add("-ss");
                command.add(String.valueOf(second));
                command.add("-i");
                command.add(videoFilePath);
                command.add("-frames:v");
                command.add("1");
                command.add("-vf");
                command.add("scale=" + scaleWidth + ":-1");
                command.add("-q:v");
                command.add("3");
                command.add(outputPath.toString());

                runCommand(command);

                if (Files.exists(outputPath)) {
                    result.add(outputPath.toString());
                }
            }

            return result;
        } catch (Exception ex) {
            log.error("截取关键帧失败: videoId={}, path={}", videoId, videoFilePath, ex);
            return new ArrayList<>();
        }
    }

    private double getVideoDuration(String videoFilePath) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegBin.replace("ffmpeg", "ffprobe"));
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration");
        command.add("-of");
        command.add("csv=p=0");
        command.add(videoFilePath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null) {
                    return Double.parseDouble(line.trim());
                }
            }

            process.waitFor();
        } catch (Exception ex) {
            log.warn("获取视频时长失败: {}", videoFilePath);
        }

        return 0;
    }

    private void runCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                // discard
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("FFmpeg 截图失败，exitCode=" + exitCode);
        }
    }
}
