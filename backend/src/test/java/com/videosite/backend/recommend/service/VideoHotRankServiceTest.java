package com.videosite.backend.recommend.service;

import com.videosite.backend.recommend.dto.VideoHotRankItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class VideoHotRankServiceTest {

    @Test
    void listLatestShouldReturnEmptyWhenMetricTablesUnavailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        VideoHotRankService service = new VideoHotRankService(jdbcTemplate);

        doThrow(new DataAccessResourceFailureException("table missing"))
                .when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        List<VideoHotRankItemResponse> result = service.listLatest("24h", 10);
        assertTrue(result.isEmpty());
    }
}
