package com.videosite.backend.scheduler;

import com.videosite.backend.recommend.service.VideoHotRankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotRankRebuildWorker {

    private static final Logger log = LoggerFactory.getLogger(HotRankRebuildWorker.class);

    private final VideoHotRankService videoHotRankService;
    private final int topN;

    public HotRankRebuildWorker(VideoHotRankService videoHotRankService,
                                @Value("${app.hot-rank.top-n:50}") int topN) {
        this.videoHotRankService = videoHotRankService;
        this.topN = topN;
    }

    @Scheduled(cron = "${app.hot-rank.worker-cron:15 0/5 * * * ?}")
    public void rebuildEveryFiveMinutes() {
        try {
            videoHotRankService.rebuildLatestSnapshot(Math.max(1, topN));
        } catch (DataAccessException ex) {
            Throwable root = ex.getMostSpecificCause();
            String rootMessage = root == null ? ex.getMessage() : root.getMessage();
            log.warn("跳过本轮热榜重建：数据库访问失败（可能未执行 V3 迁移）。原因: {}", rootMessage);
        }
    }
}
