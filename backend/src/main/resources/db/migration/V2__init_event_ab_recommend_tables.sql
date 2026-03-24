CREATE TABLE IF NOT EXISTS event_log (
    id BIGINT NOT NULL COMMENT '事件ID',
    visitor_id VARCHAR(64) NOT NULL COMMENT '匿名访客ID',
    video_id BIGINT NULL COMMENT '视频ID',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型:exposure/click/play/progress/complete',
    event_time DATETIME NOT NULL COMMENT '事件发生时间',
    session_id VARCHAR(64) NULL COMMENT '会话ID',
    page_path VARCHAR(255) NULL COMMENT '页面路径',
    ab_experiment_id BIGINT NULL COMMENT '实验ID',
    ab_variant VARCHAR(16) NULL COMMENT '实验变体',
    progress_sec INT NULL COMMENT '播放进度秒数',
    extra_json JSON NULL COMMENT '扩展字段',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_event_log_visitor_time (visitor_id, event_time),
    KEY idx_event_log_video_type_time (video_id, event_type, event_time),
    KEY idx_event_log_experiment_variant (ab_experiment_id, ab_variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行为事件日志表';

CREATE TABLE IF NOT EXISTS ab_experiment (
    id BIGINT NOT NULL COMMENT '实验ID',
    name VARCHAR(128) NOT NULL COMMENT '实验名称',
    scene VARCHAR(64) NOT NULL COMMENT '实验场景:video_cover',
    target_video_id BIGINT NOT NULL COMMENT '目标视频ID',
    status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态:draft/running/stopped',
    metric_primary VARCHAR(32) NOT NULL DEFAULT 'ctr' COMMENT '主指标',
    start_at DATETIME NULL COMMENT '开始时间',
    end_at DATETIME NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ab_experiment_status (status),
    KEY idx_ab_experiment_target_video (target_video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A/B实验表';

CREATE TABLE IF NOT EXISTS ab_variant (
    id BIGINT NOT NULL COMMENT '变体ID',
    experiment_id BIGINT NOT NULL COMMENT '实验ID',
    variant_code VARCHAR(16) NOT NULL COMMENT '变体编码:A/B',
    cover_url VARCHAR(512) NULL COMMENT '封面URL',
    traffic_ratio INT NOT NULL COMMENT '流量占比(0-100)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ab_variant_unique (experiment_id, variant_code),
    KEY idx_ab_variant_experiment (experiment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A/B实验变体表';

CREATE TABLE IF NOT EXISTS ab_assignment (
    id BIGINT NOT NULL COMMENT '分配记录ID',
    experiment_id BIGINT NOT NULL COMMENT '实验ID',
    visitor_id VARCHAR(64) NOT NULL COMMENT '访客ID',
    variant_code VARCHAR(16) NOT NULL COMMENT '变体编码',
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ab_assignment_unique (experiment_id, visitor_id),
    KEY idx_ab_assignment_visitor (visitor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A/B分流记录表';

CREATE TABLE IF NOT EXISTS video_tag (
    id BIGINT NOT NULL COMMENT '标签ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    tag_name VARCHAR(64) NOT NULL COMMENT '标签名',
    tag_source VARCHAR(16) NOT NULL COMMENT '来源:rule/ai/manual',
    confidence DECIMAL(5,4) NULL COMMENT '置信度',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_video_tag_video_id (video_id),
    KEY idx_video_tag_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频标签表';

CREATE TABLE IF NOT EXISTS cover_analysis_task (
    id BIGINT NOT NULL COMMENT '封面分析任务ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    task_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '任务状态:pending/running/success/failed',
    analyzer_type VARCHAR(32) NOT NULL DEFAULT 'rule' COMMENT '分析器类型:rule/baidu_vision',
    result_json JSON NULL COMMENT '分析结果JSON',
    error_message VARCHAR(1024) NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cover_analysis_video_id (video_id),
    KEY idx_cover_analysis_status_created_at (task_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='封面分析任务表';

CREATE TABLE IF NOT EXISTS video_tfidf_profile (
    id BIGINT NOT NULL COMMENT 'TFIDF画像ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    tokens_json JSON NULL COMMENT '分词结果JSON',
    tfidf_vector_json JSON NULL COMMENT 'TFIDF向量JSON',
    version_hour VARCHAR(16) NOT NULL COMMENT '小时版本:yyyyMMddHH',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_tfidf_version (video_id, version_hour),
    KEY idx_video_tfidf_version_hour (version_hour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频TFIDF画像表';

CREATE TABLE IF NOT EXISTS video_similarity (
    id BIGINT NOT NULL COMMENT '相似度记录ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    related_video_id BIGINT NOT NULL COMMENT '相关视频ID',
    similarity_score DECIMAL(10,8) NOT NULL COMMENT '相似度分值',
    version_hour VARCHAR(16) NOT NULL COMMENT '小时版本:yyyyMMddHH',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_similarity_unique (video_id, related_video_id, version_hour),
    KEY idx_video_similarity_video_version (video_id, version_hour),
    KEY idx_video_similarity_related_video (related_video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频相似度表';

CREATE TABLE IF NOT EXISTS recommendation_result (
    id BIGINT NOT NULL COMMENT '推荐记录ID',
    visitor_id VARCHAR(64) NOT NULL COMMENT '访客ID',
    video_id BIGINT NOT NULL COMMENT '推荐视频ID',
    scene VARCHAR(32) NOT NULL COMMENT '推荐场景:home/related',
    rank_index INT NOT NULL COMMENT '排序位次',
    score_total DECIMAL(10,6) NOT NULL COMMENT '总分',
    score_content DECIMAL(10,6) NULL COMMENT '内容分',
    score_cf DECIMAL(10,6) NULL COMMENT '协同分',
    score_hot DECIMAL(10,6) NULL COMMENT '热度分',
    version_hour VARCHAR(16) NOT NULL COMMENT '小时版本:yyyyMMddHH',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_recommend_result_visitor_scene_version (visitor_id, scene, version_hour),
    KEY idx_recommend_result_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐结果表';
