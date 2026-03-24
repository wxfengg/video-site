package com.videosite.backend.transcode;

import java.util.Arrays;
import java.util.List;

public class TranscodeProfile {

    private final int height;
    private final int width;
    private final int bitrateKbps;

    public TranscodeProfile(int height, int width, int bitrateKbps) {
        this.height = height;
        this.width = width;
        this.bitrateKbps = bitrateKbps;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getBitrateKbps() {
        return bitrateKbps;
    }

    public static List<TranscodeProfile> defaults() {
        return Arrays.asList(
                new TranscodeProfile(360, 640, 800),
                new TranscodeProfile(720, 1280, 2500),
                new TranscodeProfile(1080, 1920, 4500)
        );
    }
}
