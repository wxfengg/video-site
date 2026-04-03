package com.videosite.backend.scheduler;

import com.videosite.backend.recommend.service.VideoHotRankService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HotRankRebuildWorkerTest {

    @Test
    void rebuildEveryFiveMinutesShouldCallService() {
        VideoHotRankService hotRankService = mock(VideoHotRankService.class);
        HotRankRebuildWorker worker = new HotRankRebuildWorker(hotRankService, 30);

        worker.rebuildEveryFiveMinutes();

        verify(hotRankService).rebuildLatestSnapshot(30);
    }
}
