package com.videosite.backend.scheduler;

import com.videosite.backend.recommend.service.RecommendService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendRebuildWorker {

    private final RecommendService recommendService;

    public RecommendRebuildWorker(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @Scheduled(cron = "${app.recommend.worker-cron:0 5 * * * ?}")
    public void rebuildHourly() {
        recommendService.rebuildForCurrentHour();
    }
}
