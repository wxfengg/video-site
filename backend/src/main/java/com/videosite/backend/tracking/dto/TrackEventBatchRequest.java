package com.videosite.backend.tracking.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class TrackEventBatchRequest {

    @Valid
    @NotEmpty(message = "events 不能为空")
    private List<TrackEventItemRequest> events;

    public List<TrackEventItemRequest> getEvents() {
        return events;
    }

    public void setEvents(List<TrackEventItemRequest> events) {
        this.events = events;
    }
}
