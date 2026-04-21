package com.videosite.backend.intelligence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.intelligence")
public class IntelligenceProperties {

    private boolean enabled = true;
    private String workerCron = "0/30 * * * * ?";
    private Audio audio = new Audio();
    private Frame frame = new Frame();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWorkerCron() {
        return workerCron;
    }

    public void setWorkerCron(String workerCron) {
        this.workerCron = workerCron;
    }

    public Audio getAudio() {
        return audio;
    }

    public void setAudio(Audio audio) {
        this.audio = audio;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public static class Audio {
        private String provider = "whisper"; // whisper | none
        private String whisperModel = "small";
        private String whisperScript = "scripts/transcribe.py";
        private int maxDurationSec = 600;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getWhisperModel() {
            return whisperModel;
        }

        public void setWhisperModel(String whisperModel) {
            this.whisperModel = whisperModel;
        }

        public String getWhisperScript() {
            return whisperScript;
        }

        public void setWhisperScript(String whisperScript) {
            this.whisperScript = whisperScript;
        }

        public int getMaxDurationSec() {
            return maxDurationSec;
        }

        public void setMaxDurationSec(int maxDurationSec) {
            this.maxDurationSec = maxDurationSec;
        }
    }

    public static class Frame {
        private boolean enabled = true;
        private int count = 3;
        private int scaleWidth = 480;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getScaleWidth() {
            return scaleWidth;
        }

        public void setScaleWidth(int scaleWidth) {
            this.scaleWidth = scaleWidth;
        }
    }
}
