package com.videosite.backend.ab.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.ab.dto.AbAssignmentResponse;
import com.videosite.backend.ab.dto.AbExperimentResponse;
import com.videosite.backend.ab.dto.AbExperimentSaveRequest;
import com.videosite.backend.ab.dto.AbVariantCoverUploadResponse;
import com.videosite.backend.ab.dto.AbVariantRequest;
import com.videosite.backend.ab.dto.AbVariantResponse;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.storage.StorageService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AbExperimentService {

    private static final long MAX_COVER_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_COVER_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;

    public AbExperimentService(JdbcTemplate jdbcTemplate,
                               StorageService storageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
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
        validateRequiredVariantCovers(request.getVariants());

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
    public String deleteExperiment(Long experimentId) {
        String status = getExperimentStatus(experimentId);
        if (!"stopped".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持删除已停止实验");
        }

        jdbcTemplate.update("DELETE FROM event_log WHERE ab_experiment_id = ?", experimentId);
        jdbcTemplate.update("DELETE FROM ab_assignment WHERE experiment_id = ?", experimentId);
        jdbcTemplate.update("DELETE FROM ab_variant WHERE experiment_id = ?", experimentId);
        jdbcTemplate.update("DELETE FROM ab_experiment WHERE id = ?", experimentId);

        return "deleted";
    }

    public AbVariantCoverUploadResponse uploadVariantCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面文件不能为空");
        }

        validateCoverFile(file);
        String objectKey = buildAbCoverObjectKey(file.getOriginalFilename(), file.getContentType());
        try (InputStream inputStream = file.getInputStream()) {
            storageService.put(objectKey, inputStream, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "变体封面上传失败");
        }

        String coverUrl = storageService.getUploadUrl(objectKey);
        return new AbVariantCoverUploadResponse(objectKey, coverUrl);
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

    private String getExperimentStatus(Long experimentId) {
        try {
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM ab_experiment WHERE id = ? LIMIT 1",
                    String.class,
                    experimentId
            );
            if (!StringUtils.hasText(status)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "实验不存在");
            }
            return status;
        } catch (EmptyResultDataAccessException ex) {
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

    private void validateRequiredVariantCovers(List<AbVariantRequest> variants) {
        for (AbVariantRequest variant : variants) {
            if (!StringUtils.hasText(variant.getCoverUrl())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "新建实验时每个变体都必须配置封面图");
            }
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

    private String buildAbCoverObjectKey(String fileName, String contentType) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = extractImageExtension(fileName, contentType);
        return "images/covers/ab/" + datePart + "/" + IdWorker.getId() + ext;
    }

    private String extractImageExtension(String fileName, String contentType) {
        if (StringUtils.hasText(contentType)) {
            if ("image/png".equalsIgnoreCase(contentType)) {
                return ".png";
            }
            if ("image/webp".equalsIgnoreCase(contentType)) {
                return ".webp";
            }
            if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
                return ".jpg";
            }
        }

        if (StringUtils.hasText(fileName)) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".png")) {
                return ".png";
            }
            if (lowerName.endsWith(".webp")) {
                return ".webp";
            }
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                return ".jpg";
            }
        }

        return ".jpg";
    }

    private void validateCoverFile(MultipartFile file) {
        if (file.getSize() > MAX_COVER_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面图片不能超过 5MB");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            if (!ALLOWED_COVER_MIME_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式");
            }
            return;
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式");
        }

        String lowerName = originalFilename.toLowerCase();
        boolean ok = lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp");
        if (!ok) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封面仅支持 jpg/jpeg、png、webp 格式");
        }
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
