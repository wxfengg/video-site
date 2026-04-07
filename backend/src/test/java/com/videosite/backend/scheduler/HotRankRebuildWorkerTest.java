package com.videosite.backend.scheduler;

import com.videosite.backend.recommend.service.VideoHotRankService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void rebuildEveryFiveMinutesShouldNotThrowWhenMetricsTablesMissing() {
        VideoHotRankService hotRankService = mock(VideoHotRankService.class);
        doThrow(new DataAccessResourceFailureException("table missing"))
                .when(hotRankService)
                .rebuildLatestSnapshot(30);

        HotRankRebuildWorker worker = new HotRankRebuildWorker(hotRankService, 30);

        assertDoesNotThrow(worker::rebuildEveryFiveMinutes);
        verify(hotRankService).rebuildLatestSnapshot(30);
    }
}
