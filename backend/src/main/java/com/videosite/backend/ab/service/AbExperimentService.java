package com.videosite.backend.ab.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.ab.dto.AbAssignmentResponse;
import com.videosite.backend.ab.dto.AbExperimentResponse;
import com.videosite.backend.ab.dto.AbExperimentSaveRequest;
import com.videosite.backend.ab.dto.AbVariantRequest;
import com.videosite.backend.ab.dto.AbVariantResponse;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AbExperimentService {

    private final JdbcTemplate jdbcTemplate;

    public AbExperimentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AbExperimentResponse> listExperiments() {
        List<AbExperimentResponse> experiments = jdbcTemplate.query(
                "SELECT id, name, scene, target_video_id, status, metric_primary, start_at, end_at FROM ab_experiment ORDER BY created_at DESC",
                (rs, rowNum) -> {
                    AbExperimentResponse item = new AbExperimentResponse();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setScene(rs.getString("scene"));
                    item.setTargetVideoId(rs.getLong("target_video_id"));
                    item.setStatus(rs.getString("status"));
                    item.setMetricPrimary(rs.getString("metric_primary"));
                    item.setStartAt(toText(rs.getTimestamp("start_at")));
                    item.setEndAt(toText(rs.getTimestamp("end_at")));
                    return item;
                }
        );

        for (AbExperimentResponse experiment : experiments) {
            experiment.setVariants(listVariants(experiment.getId()));
        }
        return experiments;
    }

    @Transactional(rollbackFor = Exception.class)
    public AbExperimentResponse createExperiment(AbExperimentSaveRequest request) {
        validateRatios(request.getVariants());

        Long experimentId = IdWorker.getId();
        jdbcTemplate.update(
                "INSERT INTO ab_experiment (id, name, scene, target_video_id, status, metric_primary, start_at, end_at, created_at, updated_at) VALUES (?, ?, ?, ?, 'draft', ?, ?, ?, NOW(), NOW())",
                experimentId,
                request.getName(),
                request.getScene(),
                request.getTargetVideoId(),
                request.getMetricPrimary(),
                parseTimestamp(request.getStartAt()),
                parseTimestamp(request.getEndAt())
        );

        upsertVariants(experimentId, request.getVariants());
        return findById(experimentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AbExperimentResponse updateExperiment(Long experimentId, AbExperimentSaveRequest request) {
        ensureExperimentExists(experimentId);
        validateRatios(request.getVariants());

        jdbcTemplate.update(
                "UPDATE ab_experiment SET name = ?, scene = ?, target_video_id = ?, metric_primary = ?, start_at = ?, end_at = ?, updated_at = NOW() WHERE id = ?",
                request.getName(),
                request.getScene(),
                request.getTargetVideoId(),
                request.getMetricPrimary(),
                parseTimestamp(request.getStartAt()),
                parseTimestamp(request.getEndAt()),
                experimentId
        );

        jdbcTemplate.update("DELETE FROM ab_variant WHERE experiment_id = ?", experimentId);
        upsertVariants(experimentId, request.getVariants());

        return findById(experimentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AbExperimentResponse startExperiment(Long experimentId) {
        ensureExperimentExists(experimentId);
        jdbcTemplate.update(
                "UPDATE ab_experiment SET status = 'running', start_at = COALESCE(start_at, NOW()), updated_at = NOW() WHERE id = ?",
                experimentId
        );
        return findById(experimentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AbExperimentResponse stopExperiment(Long experimentId) {
        ensureExperimentExists(experimentId);
        jdbcTemplate.update(
                "UPDATE ab_experiment SET status = 'stopped', end_at = COALESCE(end_at, NOW()), updated_at = NOW() WHERE id = ?",
                experimentId
        );
        return findById(experimentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AbAssignmentResponse assign(String visitorId, String scene, Long targetVideoId) {
        ExperimentRow experiment = findRunningExperiment(scene, targetVideoId);
        if (experiment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到运行中的实验");
        }

        String existing = findAssignedVariantCode(experiment.id, visitorId);
        if (StringUtils.hasText(existing)) {
            return buildAssignmentResponse(experiment, existing);
        }

        List<AbVariantResponse> variants = listVariants(experiment.id);
        if (variants.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "实验未配置变体");
        }

        String variantCode = chooseVariant(visitorId, experiment.id, variants);

        try {
            jdbcTemplate.update(
                    "INSERT INTO ab_assignment (id, experiment_id, visitor_id, variant_code, assigned_at) VALUES (?, ?, ?, ?, NOW())",
                    IdWorker.getId(),
                    experiment.id,
                    visitorId,
                    variantCode
            );
        } catch (DuplicateKeyException ex) {
            String raceAssigned = findAssignedVariantCode(experiment.id, visitorId);
            if (StringUtils.hasText(raceAssigned)) {
                return buildAssignmentResponse(experiment, raceAssigned);
            }
            throw ex;
        }

        return buildAssignmentResponse(experiment, variantCode);
    }

    private void upsertVariants(Long experimentId, List<AbVariantRequest> variants) {
        for (AbVariantRequest variant : variants) {
            jdbcTemplate.update(
                    "INSERT INTO ab_variant (id, experiment_id, variant_code, cover_url, traffic_ratio, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                    IdWorker.getId(),
                    experimentId,
                    variant.getVariantCode(),
                    variant.getCoverUrl(),
                    variant.getTrafficRatio()
            );
        }
    }

    private AbExperimentResponse findById(Long experimentId) {
        try {
            AbExperimentResponse experiment = jdbcTemplate.queryForObject(
                    "SELECT id, name, scene, target_video_id, status, metric_primary, start_at, end_at FROM ab_experiment WHERE id = ?",
                    (rs, rowNum) -> {
                        AbExperimentResponse item = new AbExperimentResponse();
                        item.setId(rs.getLong("id"));
                        item.setName(rs.getString("name"));
                        item.setScene(rs.getString("scene"));
                        item.setTargetVideoId(rs.getLong("target_video_id"));
                        item.setStatus(rs.getString("status"));
                        item.setMetricPrimary(rs.getString("metric_primary"));
                        item.setStartAt(toText(rs.getTimestamp("start_at")));
                        item.setEndAt(toText(rs.getTimestamp("end_at")));
                        return item;
                    },
                    experimentId
            );
            if (experiment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "实验不存在");
            }
            experiment.setVariants(listVariants(experimentId));
            return experiment;
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实验不存在");
        }
    }

    private List<AbVariantResponse> listVariants(Long experimentId) {
        return jdbcTemplate.query(
                "SELECT id, variant_code, cover_url, traffic_ratio FROM ab_variant WHERE experiment_id = ? ORDER BY variant_code ASC",
                (rs, rowNum) -> {
                    AbVariantResponse variant = new AbVariantResponse();
                    variant.setId(rs.getLong("id"));
                    variant.setVariantCode(rs.getString("variant_code"));
                    variant.setCoverUrl(rs.getString("cover_url"));
                    variant.setTrafficRatio(rs.getInt("traffic_ratio"));
                    return variant;
                },
                experimentId
        );
    }

    private void ensureExperimentExists(Long experimentId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ab_experiment WHERE id = ?", Integer.class, experimentId);
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实验不存在");
        }
    }

    private void validateRatios(List<AbVariantRequest> variants) {
        int sum = 0;
        List<String> seen = new ArrayList<>();
        for (AbVariantRequest variant : variants) {
            if (seen.contains(variant.getVariantCode())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "variantCode 不能重复");
            }
            seen.add(variant.getVariantCode());
            sum += variant.getTrafficRatio() == null ? 0 : variant.getTrafficRatio();
        }

        if (sum != 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "trafficRatio 总和必须为100");
        }
    }

    private Timestamp parseTimestamp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(value));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时间格式错误，期望 ISO 日期时间");
        }
    }

    private String toText(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private ExperimentRow findRunningExperiment(String scene, Long targetVideoId) {
        List<ExperimentRow> list = jdbcTemplate.query(
                "SELECT id, name FROM ab_experiment WHERE status = 'running' AND scene = ? AND (? IS NULL OR target_video_id = ?) ORDER BY updated_at DESC LIMIT 1",
                (rs, rowNum) -> new ExperimentRow(rs.getLong("id"), rs.getString("name")),
                scene,
                targetVideoId,
                targetVideoId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private String chooseVariant(String visitorId, Long experimentId, List<AbVariantResponse> variants) {
        String seed = visitorId + "#" + experimentId;
        long hash = 1469598103934665603L;
        for (int i = 0; i < seed.length(); i += 1) {
            hash ^= seed.charAt(i);
            hash *= 1099511628211L;
        }

        int bucket = (int) (Math.abs(hash) % 100);
        int cumulative = 0;
        for (AbVariantResponse variant : variants) {
            cumulative += variant.getTrafficRatio() == null ? 0 : variant.getTrafficRatio();
            if (bucket < cumulative) {
                return variant.getVariantCode();
            }
        }
        return variants.get(variants.size() - 1).getVariantCode();
    }

    private String findAssignedVariantCode(Long experimentId, String visitorId) {
        List<String> list = jdbcTemplate.query(
                "SELECT variant_code FROM ab_assignment WHERE experiment_id = ? AND visitor_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("variant_code"),
                experimentId,
                visitorId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private AbAssignmentResponse buildAssignmentResponse(ExperimentRow experiment, String variantCode) {
        AbAssignmentResponse response = new AbAssignmentResponse();
        response.setExperimentId(experiment.id);
        response.setExperimentName(experiment.name);
        response.setVariantCode(variantCode);

        List<String> coverList = jdbcTemplate.query(
                "SELECT cover_url FROM ab_variant WHERE experiment_id = ? AND variant_code = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("cover_url"),
                experiment.id,
                variantCode
        );
        response.setCoverUrl(coverList.isEmpty() ? null : coverList.get(0));
        return response;
    }

    private static class ExperimentRow {
        private final Long id;
        private final String name;

        private ExperimentRow(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
