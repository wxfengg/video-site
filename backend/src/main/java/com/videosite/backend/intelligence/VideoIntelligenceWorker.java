package com.videosite.backend.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VideoIntelligenceWorker {

    private static final Logger log = LoggerFactory.getLogger(VideoIntelligenceWorker.class);

    private final VideoIntelligenceService videoIntelligenceService;

    public VideoIntelligenceWorker(VideoIntelligenceService videoIntelligenceService) {
        this.videoIntelligenceService = videoIntelligenceService;
    }

    @Scheduled(cron = "${app.intelligence.worker-cron:0/30 * * * * ?}")
    public void run() {
        int processed = 0;
        while (videoIntelligenceService.processNextPendingTask()) {
            processed++;
            if (processed >= 5) {
                break;
            }
        }
        if (processed > 0) {
            log.info("视频智能分析 Worker 本轮处理 {} 个任务", processed);
        }
    }
}
