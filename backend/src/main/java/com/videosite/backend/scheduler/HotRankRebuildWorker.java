package com.videosite.backend.scheduler;

import com.videosite.backend.recommend.service.VideoHotRankService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotRankRebuildWorker {

    private final VideoHotRankService videoHotRankService;
    private final int topN;

    public HotRankRebuildWorker(VideoHotRankService videoHotRankService,
                                @Value("${app.hot-rank.top-n:50}") int topN) {
        this.videoHotRankService = videoHotRankService;
        this.topN = topN;
    }

    @Scheduled(cron = "${app.hot-rank.worker-cron:15 0/5 * * * ?}")
    public void rebuildEveryFiveMinutes() {
        videoHotRankService.rebuildLatestSnapshot(Math.max(1, topN));
    }
}
