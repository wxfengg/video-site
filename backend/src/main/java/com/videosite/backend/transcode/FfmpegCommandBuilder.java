package com.videosite.backend.transcode;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class FfmpegCommandBuilder {

    public List<String> buildMp4Command(String ffmpegBin,
                                        Path inputPath,
                                        Path outputPath,
                                        TranscodeProfile profile) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegBin);
        command.add("-y");
        command.add("-i");
        command.add(inputPath.toString());
        command.add("-vf");
        command.add("scale=" + profile.getWidth() + ":" + profile.getHeight());
        command.add("-c:v");
        command.add("libx264");
        command.add("-b:v");
        command.add(profile.getBitrateKbps() + "k");
        command.add("-preset");
        command.add("veryfast");
        command.add("-c:a");
        command.add("aac");
        command.add("-movflags");
        command.add("+faststart");
        command.add(outputPath.toString());
        return command;
    }

    public List<String> buildHlsCommand(String ffmpegBin,
                                        Path inputPath,
                                        Path playlistPath,
                                        Path segmentPattern,
                                        TranscodeProfile profile) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegBin);
        command.add("-y");
        command.add("-i");
        command.add(inputPath.toString());
        command.add("-vf");
        command.add("scale=" + profile.getWidth() + ":" + profile.getHeight());
        command.add("-c:v");
        command.add("libx264");
        command.add("-b:v");
        command.add(profile.getBitrateKbps() + "k");
        command.add("-c:a");
        command.add("aac");
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("6");
        command.add("-hls_list_size");
        command.add("0");
        command.add("-hls_segment_filename");
        command.add(segmentPattern.toString());
        command.add(playlistPath.toString());
        return command;
    }
}
