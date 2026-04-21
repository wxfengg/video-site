package com.videosite.backend.intelligence.analysis;

import java.util.List;

public class AnalyzeContext {

    private final Long videoId;
    private final String title;
    private final String description;
    private final String audioTranscript;
    private final List<String> keyframePaths;

    public AnalyzeContext(Long videoId, String title, String description,
                          String audioTranscript, List<String> keyframePaths) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.audioTranscript = audioTranscript;
        this.keyframePaths = keyframePaths;
    }

    public Long getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAudioTranscript() {
        return audioTranscript;
    }

    public List<String> getKeyframePaths() {
        return keyframePaths;
    }
}
