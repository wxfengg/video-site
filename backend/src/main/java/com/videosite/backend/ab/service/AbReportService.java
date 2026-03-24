package com.videosite.backend.ab.service;

import com.videosite.backend.ab.dto.AbCtrReportResponse;
import com.videosite.backend.ab.dto.AbCtrVariantReportItem;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AbReportService {

    private final JdbcTemplate jdbcTemplate;

    public AbReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AbCtrReportResponse getCtrReport(Long experimentId) {
        List<String> metricRows = jdbcTemplate.query(
                "SELECT metric_primary FROM ab_experiment WHERE id = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("metric_primary"),
                experimentId
        );

        if (metricRows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实验不存在");
        }

        List<AbCtrVariantReportItem> baseVariants = jdbcTemplate.query(
                "SELECT variant_code FROM ab_variant WHERE experiment_id = ? ORDER BY variant_code ASC",
                (rs, rowNum) -> {
                    AbCtrVariantReportItem item = new AbCtrVariantReportItem();
                    item.setVariantCode(rs.getString("variant_code"));
                    item.setExposureUv(0);
                    item.setClickUv(0);
                    item.setCtr(0);
                    return item;
                },
                experimentId
        );

        Map<String, AbCtrVariantReportItem> variantMap = new HashMap<>();
        for (AbCtrVariantReportItem variant : baseVariants) {
            variantMap.put(variant.getVariantCode(), variant);
        }

        List<AbCtrVariantReportItem> eventAgg = jdbcTemplate.query(
                "SELECT ab_variant AS variant_code, " +
                        "COUNT(DISTINCT CASE WHEN event_type = 'exposure' THEN visitor_id END) AS exposure_uv, " +
                        "COUNT(DISTINCT CASE WHEN event_type = 'click' THEN visitor_id END) AS click_uv " +
                        "FROM event_log WHERE ab_experiment_id = ? GROUP BY ab_variant",
                (rs, rowNum) -> {
                    AbCtrVariantReportItem item = new AbCtrVariantReportItem();
                    item.setVariantCode(rs.getString("variant_code"));
                    item.setExposureUv(rs.getLong("exposure_uv"));
                    item.setClickUv(rs.getLong("click_uv"));
                    return item;
                },
                experimentId
        );

        for (AbCtrVariantReportItem agg : eventAgg) {
            if (agg.getVariantCode() == null || !variantMap.containsKey(agg.getVariantCode())) {
                continue;
            }
            AbCtrVariantReportItem target = variantMap.get(agg.getVariantCode());
            target.setExposureUv(agg.getExposureUv());
            target.setClickUv(agg.getClickUv());
            target.setCtr(calcCtr(agg.getExposureUv(), agg.getClickUv()));
        }

        AbCtrReportResponse response = new AbCtrReportResponse();
        response.setExperimentId(experimentId);
        response.setMetricPrimary(metricRows.get(0));
        response.setVariants(baseVariants);
        return response;
    }

    private double calcCtr(long exposureUv, long clickUv) {
        if (exposureUv <= 0) {
            return 0;
        }
        return (double) clickUv / (double) exposureUv;
    }
}
