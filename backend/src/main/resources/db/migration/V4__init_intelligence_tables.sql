-- 视频智能分析结果
CREATE TABLE IF NOT EXISTS video_intelligence (
    id BIGINT NOT NULL COMMENT 'ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    summary TEXT NULL COMMENT 'AI摘要',
    tags_json JSON NULL COMMENT 'AI标签数组',
    categories_json JSON NULL COMMENT '分类路径',
    sentiment VARCHAR(16) NULL COMMENT '情感基调:positive/neutral/negative',
    audience VARCHAR(128) NULL COMMENT '目标受众',
    keywords TEXT NULL COMMENT '扩展关键词',
    embedding_text TEXT NULL COMMENT '用于TF-IDF的语义聚合文本',
    model_version VARCHAR(32) NULL COMMENT '模型版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_intelligence_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频智能分析结果';

-- 视频智能分析任务
CREATE TABLE IF NOT EXISTS video_intelligence_task (
    id BIGINT NOT NULL COMMENT '任务ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    task_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态:pending/running/success/failed',
    analyzer_type VARCHAR(32) NOT NULL DEFAULT 'kimi' COMMENT '分析器类型',
    audio_transcript TEXT NULL COMMENT '音频转录文本',
    result_json JSON NULL COMMENT '分析结果JSON',
    error_message VARCHAR(1024) NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vi_task_status_created (task_status, created_at),
    KEY idx_vi_task_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频智能分析任务';

-- 访客兴趣画像
CREATE TABLE IF NOT EXISTS visitor_interest_profile (
    id BIGINT NOT NULL COMMENT 'ID',
    visitor_id VARCHAR(64) NOT NULL COMMENT '访客ID',
    interest_tags_json JSON NULL COMMENT '兴趣标签及权重',
    preferred_categories_json JSON NULL COMMENT '偏好分类数组',
    profile_summary TEXT NULL COMMENT '画像摘要',
    version_day VARCHAR(16) NOT NULL COMMENT '版本日期:yyyyMMdd',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_visitor_profile (visitor_id, version_day),
    KEY idx_visitor_profile_version (version_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客兴趣画像';

-- 推荐结果表新增推荐理由字段
ALTER TABLE recommendation_result ADD COLUMN recommend_reason VARCHAR(128) NULL COMMENT '推荐理由';
