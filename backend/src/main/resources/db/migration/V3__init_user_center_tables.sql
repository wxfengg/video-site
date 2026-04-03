CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL COMMENT '普通用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户表';

CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT NOT NULL COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_favorite_unique (user_id, video_id),
    KEY idx_user_favorite_video_created (video_id, created_at),
    KEY idx_user_favorite_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

CREATE TABLE IF NOT EXISTS user_watch_history (
    id BIGINT NOT NULL COMMENT '观看历史ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    last_progress_sec INT NOT NULL DEFAULT 0 COMMENT '最后观看进度秒数',
    duration_sec_snapshot INT NULL COMMENT '视频时长快照',
    completion_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '完播率[0,1]',
    is_completed_90 TINYINT NOT NULL DEFAULT 0 COMMENT '是否达到90%完播',
    play_count INT NOT NULL DEFAULT 1 COMMENT '累计播放次数',
    last_watched_at DATETIME NOT NULL COMMENT '最近观看时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_history_unique (user_id, video_id),
    KEY idx_user_history_user_last_watched (user_id, last_watched_at),
    KEY idx_user_history_video_last_watched (video_id, last_watched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户观看历史表';

CREATE TABLE IF NOT EXISTS video_like (
    id BIGINT NOT NULL COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_like_unique (user_id, video_id),
    KEY idx_video_like_video_created (video_id, created_at),
    KEY idx_video_like_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频点赞表';

CREATE TABLE IF NOT EXISTS video_comment (
    id BIGINT NOT NULL COMMENT '评论ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    status VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '状态:normal/deleted',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_video_comment_video_created (video_id, created_at),
    KEY idx_video_comment_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频评论表';

CREATE TABLE IF NOT EXISTS video_source (
    id BIGINT NOT NULL COMMENT '视频来源ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    source_mode VARCHAR(16) NOT NULL COMMENT '来源模式:local/external',
    source_protocol VARCHAR(16) NULL COMMENT '协议:mp4/hls',
    source_url VARCHAR(1024) NULL COMMENT '外链地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_source_mode (video_id, source_mode),
    KEY idx_video_source_mode_created (source_mode, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频输入源表';

CREATE TABLE IF NOT EXISTS metric_video_5m (
    id BIGINT NOT NULL COMMENT '视频5分钟指标ID',
    bucket_time DATETIME NOT NULL COMMENT '时间桶(5分钟)',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    exposure_uv INT NOT NULL DEFAULT 0 COMMENT '曝光UV',
    click_uv INT NOT NULL DEFAULT 0 COMMENT '点击UV',
    play_uv INT NOT NULL DEFAULT 0 COMMENT '播放UV',
    like_uv INT NOT NULL DEFAULT 0 COMMENT '点赞UV',
    complete_uv INT NOT NULL DEFAULT 0 COMMENT '完播UV',
    completion_rate_90 DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '90%完播率',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_video_5m_unique (bucket_time, video_id),
    KEY idx_metric_video_5m_video_time (video_id, bucket_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频5分钟聚合指标表';

CREATE TABLE IF NOT EXISTS metric_site_5m (
    id BIGINT NOT NULL COMMENT '站点5分钟指标ID',
    bucket_time DATETIME NOT NULL COMMENT '时间桶(5分钟)',
    dau INT NOT NULL DEFAULT 0 COMMENT '日活估算',
    new_users INT NOT NULL DEFAULT 0 COMMENT '新增用户数',
    play_pv INT NOT NULL DEFAULT 0 COMMENT '播放PV',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_site_5m_bucket_time (bucket_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点5分钟聚合指标表';

CREATE TABLE IF NOT EXISTS video_hot_rank_5m (
    id BIGINT NOT NULL COMMENT '热榜ID',
    bucket_time DATETIME NOT NULL COMMENT '时间桶(5分钟)',
    window_type VARCHAR(16) NOT NULL COMMENT '窗口类型:24h/7d',
    rank_index INT NOT NULL COMMENT '名次',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    hot_score DECIMAL(10,6) NOT NULL COMMENT '热度分',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_hot_rank_5m_unique (bucket_time, window_type, rank_index),
    KEY idx_video_hot_rank_5m_window_time (window_type, bucket_time),
    KEY idx_video_hot_rank_5m_video_time (video_id, bucket_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='5分钟视频热榜快照表';
