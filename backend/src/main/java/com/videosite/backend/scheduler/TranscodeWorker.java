package com.videosite.backend.scheduler;

import com.videosite.backend.transcode.TranscodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TranscodeWorker {

    private static final Logger log = LoggerFactory.getLogger(TranscodeWorker.class);

    private final TranscodeService transcodeService;

    @Value("${app.transcode.batch-size:1}")
    private int batchSize;

    public TranscodeWorker(TranscodeService transcodeService) {
        this.transcodeService = transcodeService;
    }

    @Scheduled(cron = "${app.transcode.worker-cron:0/10 * * * * ?}")
    public void run() {
        int processed = 0;
        for (int i = 0; i < batchSize; i++) {
            boolean hasMore = transcodeService.processNextPendingTask();
            if (!hasMore) {
                break;
            }
            processed++;
        }

        if (processed > 0) {
            log.info("Transcode worker processed {} task(s)", processed);
        }
    }
}
