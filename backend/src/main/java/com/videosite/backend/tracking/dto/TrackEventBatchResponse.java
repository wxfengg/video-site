package com.videosite.backend.tracking.dto;

public class TrackEventBatchResponse {

    private int received;
    private int stored;

    public TrackEventBatchResponse() {
    }

    public TrackEventBatchResponse(int received, int stored) {
        this.received = received;
        this.stored = stored;
    }

    public int getReceived() {
        return received;
    }

    public void setReceived(int received) {
        this.received = received;
    }

    public int getStored() {
        return stored;
    }

    public void setStored(int stored) {
        this.stored = stored;
    }
}
