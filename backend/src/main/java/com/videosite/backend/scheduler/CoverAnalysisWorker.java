package com.videosite.backend.scheduler;

import com.videosite.backend.cover.CoverTagService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CoverAnalysisWorker {

    private final CoverTagService coverTagService;

    public CoverAnalysisWorker(CoverTagService coverTagService) {
        this.coverTagService = coverTagService;
    }

    @Scheduled(cron = "${app.cover-analysis.worker-cron:0/20 * * * * ?}")
    public void pollAndProcess() {
        coverTagService.processNextPendingTask();
    }
}
