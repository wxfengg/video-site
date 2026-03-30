CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT NOT NULL COMMENT '管理员ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';

CREATE TABLE IF NOT EXISTS visitor_profile (
    id BIGINT NOT NULL COMMENT '访客记录ID',
    visitor_id VARCHAR(64) NOT NULL COMMENT '匿名访客ID',
    first_seen_at DATETIME NOT NULL COMMENT '首次访问时间',
    last_seen_at DATETIME NOT NULL COMMENT '最近访问时间',
    user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
    country VARCHAR(64) NULL COMMENT '国家/地区',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_visitor_profile_visitor_id (visitor_id),
    KEY idx_visitor_profile_last_seen_at (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匿名访客画像表';

CREATE TABLE IF NOT EXISTS video (
    id BIGINT NOT NULL COMMENT '视频ID',
    title VARCHAR(255) NOT NULL COMMENT '视频标题',
    description TEXT NULL COMMENT '视频简介',
    cover_url VARCHAR(512) NULL COMMENT '封面URL',
    duration_sec INT NULL COMMENT '时长(秒)',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '状态:draft/transcoding/ready/published/offline',
    publish_at DATETIME NULL COMMENT '发布时间',
    created_by BIGINT NULL COMMENT '创建者管理员ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_video_status_publish_at (status, publish_at),
    KEY idx_video_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频主表';

CREATE TABLE IF NOT EXISTS video_file (
    id BIGINT NOT NULL COMMENT '视频文件记录ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    storage_provider VARCHAR(32) NOT NULL COMMENT '存储提供者:local',
    object_key VARCHAR(512) NOT NULL COMMENT '对象存储key或路径',
    file_size BIGINT NULL COMMENT '文件大小',
    mime_type VARCHAR(128) NULL COMMENT 'MIME类型',
    checksum_sha256 VARCHAR(64) NULL COMMENT '文件SHA256',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_video_file_video_id (video_id),
    UNIQUE KEY uk_video_file_checksum (checksum_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始视频文件表';

CREATE TABLE IF NOT EXISTS video_transcode_task (
    id BIGINT NOT NULL COMMENT '转码任务ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    task_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '任务状态:pending/running/success/failed',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1024) NULL COMMENT '错误信息',
    started_at DATETIME NULL COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_transcode_video_id (video_id),
    KEY idx_transcode_status_created_at (task_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频转码任务表';

CREATE TABLE IF NOT EXISTS video_variant (
    id BIGINT NOT NULL COMMENT '转码变体ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    resolution INT NOT NULL COMMENT '分辨率高度:360/720/1080',
    bitrate_kbps INT NULL COMMENT '码率kbps',
    format VARCHAR(16) NOT NULL COMMENT '格式:hls/mp4',
    storage_provider VARCHAR(32) NOT NULL COMMENT '存储提供者',
    object_key VARCHAR(512) NOT NULL COMMENT '对象存储key或路径',
    file_size BIGINT NULL COMMENT '文件大小',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_video_variant_video_id (video_id),
    UNIQUE KEY uk_video_variant_unique (video_id, resolution, format)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频转码产物表';

CREATE TABLE IF NOT EXISTS video_play_source (
    id BIGINT NOT NULL COMMENT '播放源ID',
    video_id BIGINT NOT NULL COMMENT '视频ID',
    source_type VARCHAR(32) NOT NULL COMMENT '源类型:hls_master/mp4_360/mp4_720/mp4_1080',
    play_url VARCHAR(1024) NOT NULL COMMENT '播放URL',
    expires_at DATETIME NULL COMMENT '过期时间(预签名时使用)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_video_play_source_video_id (video_id),
    UNIQUE KEY uk_video_play_source_unique (video_id, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频播放源表';
