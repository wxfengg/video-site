package com.videosite.backend.ab.service;

import com.videosite.backend.ab.dto.AbCtrReportResponse;
import com.videosite.backend.ab.dto.AbCtrVariantReportItem;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbReportServiceTest {

    @Test
    void getCtrReportShouldCalculateCtrByVariant() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AbReportService service = new AbReportService(jdbcTemplate);

        when(jdbcTemplate.query(
                eq("SELECT metric_primary FROM ab_experiment WHERE id = ? LIMIT 1"),
                any(RowMapper.class),
                eq(1001L)
        )).thenReturn(List.of("ctr"));

        AbCtrVariantReportItem variantA = new AbCtrVariantReportItem();
        variantA.setVariantCode("A");
        variantA.setExposureUv(0);
        variantA.setClickUv(0);
        variantA.setCtr(0);

        AbCtrVariantReportItem variantB = new AbCtrVariantReportItem();
        variantB.setVariantCode("B");
        variantB.setExposureUv(0);
        variantB.setClickUv(0);
        variantB.setCtr(0);

        when(jdbcTemplate.query(
                eq("SELECT variant_code FROM ab_variant WHERE experiment_id = ? ORDER BY variant_code ASC"),
                any(RowMapper.class),
                eq(1001L)
        )).thenReturn(List.of(variantA, variantB));

        AbCtrVariantReportItem aggA = new AbCtrVariantReportItem();
        aggA.setVariantCode("A");
        aggA.setExposureUv(100);
        aggA.setClickUv(30);

        when(jdbcTemplate.query(
                argThat((String sql) -> sql != null && sql.contains("FROM event_log") && sql.contains("ab_experiment_id = ?")),
                any(RowMapper.class),
                eq(1001L)
        )).thenReturn(List.of(aggA));

        AbCtrReportResponse response = service.getCtrReport(1001L);

        assertEquals(1001L, response.getExperimentId());
        assertEquals("ctr", response.getMetricPrimary());
        assertEquals(2, response.getVariants().size());
        assertEquals(0.3D, response.getVariants().get(0).getCtr());
        assertEquals(0D, response.getVariants().get(1).getCtr());
    }
}
