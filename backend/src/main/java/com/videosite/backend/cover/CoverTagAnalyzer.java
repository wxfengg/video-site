package com.videosite.backend.cover;

import java.util.List;

public interface CoverTagAnalyzer {

    String analyzerType();

    List<TagCandidate> analyze(CoverAnalyzeContext context);

    class CoverAnalyzeContext {
        private final Long videoId;
        private final String title;
        private final String description;
        private final String coverUrl;

        public CoverAnalyzeContext(Long videoId, String title, String description, String coverUrl) {
            this.videoId = videoId;
            this.title = title;
            this.description = description;
            this.coverUrl = coverUrl;
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

        public String getCoverUrl() {
            return coverUrl;
        }
    }

    class TagCandidate {
        private final String tagName;
        private final double confidence;

        public TagCandidate(String tagName, double confidence) {
            this.tagName = tagName;
            this.confidence = confidence;
        }

        public String getTagName() {
            return tagName;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
