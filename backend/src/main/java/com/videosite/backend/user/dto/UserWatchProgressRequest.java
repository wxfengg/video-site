package com.videosite.backend.user.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class UserWatchProgressRequest {

    @NotNull(message = "progressSec 不能为空")
    @Min(value = 0, message = "progressSec 不能小于0")
    private Integer progressSec;

    @Min(value = 1, message = "durationSecSnapshot 必须大于0")
    private Integer durationSecSnapshot;

    public Integer getProgressSec() {
        return progressSec;
    }

    public void setProgressSec(Integer progressSec) {
        this.progressSec = progressSec;
    }

    public Integer getDurationSecSnapshot() {
        return durationSecSnapshot;
    }

    public void setDurationSecSnapshot(Integer durationSecSnapshot) {
        this.durationSecSnapshot = durationSecSnapshot;
    }
}
