package com.videosite.backend.intelligence.audio;

import org.springframework.stereotype.Component;

@Component
public class NoOpTranscriptService implements AudioTranscriptService {

    @Override
    public String transcript(String videoFilePath) {
        return "";
    }
}
