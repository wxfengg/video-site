package com.videosite.backend.ab.service;

import com.videosite.backend.ab.dto.AbExperimentSaveRequest;
import com.videosite.backend.ab.dto.AbVariantRequest;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AbExperimentServiceTest {

    private AbExperimentService createService(JdbcTemplate jdbcTemplate) {
        StorageService storageService = mock(StorageService.class);
        return new AbExperimentService(jdbcTemplate, storageService);
    }

    @Test
    void deleteExperimentShouldDeleteStoppedExperimentData() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AbExperimentService service = createService(jdbcTemplate);

        when(jdbcTemplate.queryForObject(
                "SELECT status FROM ab_experiment WHERE id = ? LIMIT 1",
                String.class,
                1001L
        )).thenReturn("stopped");

        String result = service.deleteExperiment(1001L);

        assertEquals("deleted", result);
        verify(jdbcTemplate).update("DELETE FROM event_log WHERE ab_experiment_id = ?", 1001L);
        verify(jdbcTemplate).update("DELETE FROM ab_assignment WHERE experiment_id = ?", 1001L);
        verify(jdbcTemplate).update("DELETE FROM ab_variant WHERE experiment_id = ?", 1001L);
        verify(jdbcTemplate).update("DELETE FROM ab_experiment WHERE id = ?", 1001L);
    }

    @Test
    void deleteExperimentShouldRejectNonStoppedExperiment() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AbExperimentService service = createService(jdbcTemplate);

        when(jdbcTemplate.queryForObject(
                "SELECT status FROM ab_experiment WHERE id = ? LIMIT 1",
                String.class,
                1002L
        )).thenReturn("running");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteExperiment(1002L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());

        verify(jdbcTemplate, never()).update(eq("DELETE FROM event_log WHERE ab_experiment_id = ?"), eq(1002L));
        verify(jdbcTemplate, never()).update(eq("DELETE FROM ab_assignment WHERE experiment_id = ?"), eq(1002L));
        verify(jdbcTemplate, never()).update(eq("DELETE FROM ab_variant WHERE experiment_id = ?"), eq(1002L));
        verify(jdbcTemplate, never()).update(eq("DELETE FROM ab_experiment WHERE id = ?"), eq(1002L));
    }

    @Test
    void deleteExperimentShouldThrowWhenExperimentNotFound() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AbExperimentService service = createService(jdbcTemplate);

        when(jdbcTemplate.queryForObject(
                "SELECT status FROM ab_experiment WHERE id = ? LIMIT 1",
                String.class,
                1003L
        )).thenThrow(new EmptyResultDataAccessException(1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteExperiment(1003L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createExperimentShouldRejectWhenVariantCoverMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AbExperimentService service = createService(jdbcTemplate);

        AbExperimentSaveRequest request = buildCreateRequest("", "https://cdn.example.com/b.jpg");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createExperiment(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verifyNoInteractions(jdbcTemplate);
    }

    private AbExperimentSaveRequest buildCreateRequest(String coverA, String coverB) {
        AbExperimentSaveRequest request = new AbExperimentSaveRequest();
        request.setName("首页封面对照实验");
        request.setScene("home_cover");
        request.setTargetVideoId(12345L);
        request.setMetricPrimary("ctr");

        AbVariantRequest variantA = new AbVariantRequest();
        variantA.setVariantCode("A");
        variantA.setCoverUrl(coverA);
        variantA.setTrafficRatio(50);

        AbVariantRequest variantB = new AbVariantRequest();
        variantB.setVariantCode("B");
        variantB.setCoverUrl(coverB);
        variantB.setTrafficRatio(50);

        request.setVariants(Arrays.asList(variantA, variantB));
        return request;
    }
}
