package com.videosite.backend.dashboard.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceFormulaTest {

    @Test
    void calculateRatioShouldReturnZeroWhenDenominatorIsZero() {
        assertEquals(0D, DashboardService.calculateRatio(10, 0));
    }

    @Test
    void calculateRatioShouldCalculateCtrCorrectly() {
        assertEquals(0.25D, DashboardService.calculateRatio(25, 100));
    }

    @Test
    void calculateRatioShouldCalculateCompletionRateCorrectly() {
        assertEquals(0.9D, DashboardService.calculateRatio(9, 10));
    }
}
