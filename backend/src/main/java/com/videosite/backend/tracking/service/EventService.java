package com.videosite.backend.tracking.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.tracking.dto.TrackEventBatchRequest;
import com.videosite.backend.tracking.dto.TrackEventItemRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class EventService {

    private final JdbcTemplate jdbcTemplate;

    public EventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public int collectBatch(String visitorId, TrackEventBatchRequest request) {
        List<TrackEventItemRequest> events = request.getEvents();
        if (events == null || events.isEmpty()) {
            return 0;
        }

        int stored = 0;
        for (TrackEventItemRequest item : events) {
            if (!StringUtils.hasText(item.getEventType())) {
                throw new BusinessException(com.videosite.backend.common.api.ErrorCode.BAD_REQUEST, "eventType 不能为空");
            }

            long id = resolveEventId(visitorId, item);
            int affected = jdbcTemplate.update(
                    "INSERT IGNORE INTO event_log (id, visitor_id, video_id, event_type, event_time, session_id, page_path, ab_experiment_id, ab_variant, progress_sec, extra_json, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    id,
                    visitorId,
                    item.getVideoId(),
                    item.getEventType(),
                    Timestamp.from(Instant.ofEpochMilli(item.getEventTime())),
                    emptyToNull(item.getSessionId()),
                    emptyToNull(item.getPagePath()),
                    item.getAbExperimentId(),
                    emptyToNull(item.getAbVariant()),
                    item.getProgressSec(),
                    normalizeJson(item.getExtraJson(), item.getEventId())
            );
            if (affected > 0) {
                stored += 1;
            }
        }

        return stored;
    }

    private long resolveEventId(String visitorId, TrackEventItemRequest item) {
        if (!StringUtils.hasText(item.getEventId())) {
            return IdWorker.getId();
        }

        String raw = visitorId + "|" + item.getEventId();
        long hash = 1125899906842597L;
        for (int i = 0; i < raw.length(); i += 1) {
            hash = 31 * hash + raw.charAt(i);
        }
        return Math.abs(hash);
    }

    private String normalizeJson(String extraJson, String eventId) {
        String eventIdJson = StringUtils.hasText(eventId) ? "\"eventId\":\"" + eventId + "\"" : "";
        if (!StringUtils.hasText(extraJson)) {
            return StringUtils.hasText(eventIdJson) ? "{" + eventIdJson + "}" : null;
        }

        String trimmed = extraJson.trim();
        if (!StringUtils.hasText(eventIdJson)) {
            return trimmed;
        }

        if ("{}".equals(trimmed)) {
            return "{" + eventIdJson + "}";
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed.substring(0, trimmed.length() - 1) + "," + eventIdJson + "}";
        }
        return trimmed;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
