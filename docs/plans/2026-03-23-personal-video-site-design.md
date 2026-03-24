# 个人视频网站内容迭代与智能分析系统设计文档

**日期**: 2026-03-23  
**状态**: 已确认（用户约束已冻结）

---

## 1. 设计目标

构建一个前后端分离的个人视频网站，完成以下闭环：

- 视频上传 -> 自动转码（360p/720p/1080p） -> HLS + MP4 播放
- 访客行为采集 -> A/B 实验（按 visitor_id 稳定分流）-> CTR 驱动优化
- 小型推荐能力（内容 + 协同过滤混合）与 TF-IDF 相关视频
- 封面图自动标签（本地规则算法实现，预留 AI 服务接口）

约束：

- 匿名访客 + 管理员后台
- OSS、百度云视觉先预留实现入口
- 推荐小时级更新
- A/B 主指标为 CTR

---

## 2. 总体架构（推荐方案 B：单体 + 轻量异步）

- 前端：Vue 3 + Vite + Pinia + Vue Router + Element Plus + Artplayer（播放器）
- 后端：Spring Boot + MyBatis-Plus + MySQL
- 异步任务：数据库任务表 + Spring 调度/线程池 Worker
- 媒体处理：FFmpeg
- 存储：StorageProvider 抽象（LocalStorageProvider 默认，OssStorageProvider 预留）
- 智能标签：CoverTagAnalyzer 抽象（RuleBasedCoverTagAnalyzer 默认，BaiduVisionAnalyzer 预留）

### 2.1 模块边界

1. 视频与播放模块

- 上传、元数据管理、转码任务、播放源聚合

2. 埋点与 A/B 模块

- 事件采集、实验配置、稳定分流、CTR 报表

3. 推荐模块

- TF-IDF 相关视频、混合推荐、反馈回流

4. 封面标签模块

- 封面分析、标签回写、人工修正

5. 管理后台模块

- 视频管理、实验管理、任务监控

---

## 3. 数据流与关键时序

### 3.1 上传与转码

1. 前端请求上传初始化，获取上传策略（本地/OSS）
2. 上传完成后调用 complete 接口
3. 后端写入 `video` / `video_file`，创建 `transcode_task`
4. Worker 拉取待处理任务，执行 FFmpeg 输出 HLS + MP4
5. 写入 `video_variant` 与 `video_play_source`
6. 视频可发布上线

### 3.2 A/B 与埋点

1. 客户端首次生成 `visitor_id`（Cookie + localStorage）
2. 请求分组接口，后端按 `hash(visitor_id, experiment_id)` 稳定分流
3. 前端上报曝光/点击/播放事件（批量，5s 播放进度）
4. 后端聚合统计，输出 CTR 报表

### 3.3 推荐与相关视频

1. 小时任务重建 TF-IDF 向量（标题+简介+标签）
2. 计算相似视频索引
3. 混合推荐按权重输出首页推荐

### 3.4 封面智能标签

1. 上传封面后触发分析任务
2. 规则算法生成标签与置信度
3. 回写 `video_tag`，支持管理员修正

---

## 4. ER 草案（v1）

> 主键默认 `BIGINT`（雪花或自增），时间字段统一 `created_at`,`updated_at`

### 4.1 账号与访客

#### `admin_user`

- id
- username (unique)
- password_hash
- status
- last_login_at
- created_at, updated_at

#### `visitor_profile`

- id
- visitor_id (unique, varchar64)
- first_seen_at
- last_seen_at
- user_agent
- country
- created_at, updated_at

### 4.2 视频域

#### `video`

- id
- title
- description
- cover_url
- duration_sec
- status (draft/transcoding/ready/published/offline)
- publish_at
- created_by (admin_user.id)
- created_at, updated_at

#### `video_file`

- id
- video_id
- storage_provider (local/oss)
- object_key
- file_size
- mime_type
- checksum_sha256
- created_at, updated_at

#### `video_transcode_task`

- id
- video_id
- task_status (pending/running/success/failed)
- retry_count
- error_message
- started_at
- finished_at
- created_at, updated_at

#### `video_variant`

- id
- video_id
- resolution (360/720/1080)
- bitrate_kbps
- format (hls/mp4)
- storage_provider
- object_key
- file_size
- created_at, updated_at

#### `video_play_source`

- id
- video_id
- source_type (hls_master/mp4_360/mp4_720/mp4_1080)
- play_url
- expires_at
- created_at, updated_at

### 4.3 标签与封面分析

#### `video_tag`

- id
- video_id
- tag_name
- tag_source (rule/ai/manual)
- confidence
- created_at, updated_at

#### `cover_analysis_task`

- id
- video_id
- task_status (pending/running/success/failed)
- analyzer_type (rule/baidu_vision)
- result_json
- error_message
- created_at, updated_at

### 4.4 埋点与实验

#### `event_log`

- id
- visitor_id
- video_id
- event_type (exposure/click/play/progress/complete)
- event_time
- session_id
- page_path
- ab_experiment_id (nullable)
- ab_variant (nullable)
- progress_sec (nullable)
- extra_json
- created_at

#### `ab_experiment`

- id
- name
- scene (video_cover)
- target_video_id
- status (draft/running/stopped)
- metric_primary (ctr)
- start_at
- end_at
- created_at, updated_at

#### `ab_variant`

- id
- experiment_id
- variant_code (A/B)
- cover_url
- traffic_ratio
- created_at, updated_at

#### `ab_assignment`

- id
- experiment_id
- visitor_id
- variant_code
- assigned_at
- unique key(experiment_id, visitor_id)

### 4.5 推荐

#### `video_tfidf_profile`

- id
- video_id
- tokens_json
- tfidf_vector_json
- version_hour
- created_at

#### `video_similarity`

- id
- video_id
- related_video_id
- similarity_score
- version_hour
- created_at

#### `recommendation_result`

- id
- visitor_id
- video_id
- scene (home/related)
- rank_index
- score_total
- score_content
- score_cf
- score_hot
- version_hour
- created_at

---

## 5. 接口契约总览（v1）

统一响应：

```json
{ "code": 0, "message": "ok", "data": {}, "traceId": "xxx" }
```

### 5.1 视频与播放

- `POST /api/videos/upload/init`
- `POST /api/videos/upload/complete`
- `GET /api/videos/{id}`
- `GET /api/videos/{id}/play-sources`
- `GET /api/videos`
- `PATCH /api/videos/{id}`
- `POST /api/videos/{id}/publish`
- `POST /api/videos/{id}/unpublish`
- `GET /api/transcode/tasks/{taskId}`

### 5.2 埋点与 A/B

- `POST /api/events/collect`
- `POST /api/ab/experiments`
- `POST /api/ab/experiments/{id}/start`
- `POST /api/ab/experiments/{id}/stop`
- `GET /api/ab/experiments/{id}/report`
- `GET /api/ab/assignments`

### 5.3 推荐

- `GET /api/recommend/home`
- `GET /api/recommend/related/{videoId}`
- `POST /api/recommend/feedback`
- `POST /api/recommend/rebuild`

### 5.4 封面标签

- `POST /api/cover-tags/analyze`
- `GET /api/cover-tags/tasks/{taskId}`
- `PATCH /api/videos/{id}/tags`
- `GET /api/videos/{id}/tags/suggested`

---

## 6. 关键算法设计（轻量版）

### 6.1 稳定分流

- `bucket = hash(visitor_id + experiment_id) % 100`
- 按流量比例映射到变体 A/B
- 写入 `ab_assignment`，保证幂等

### 6.2 CTR

$$CTR = \frac{click\_uv}{exposure\_uv}$$

主指标：CTR；辅指标：平均播放时长、完播率（用于说明，不作为首要判定）

### 6.3 TF-IDF 相似度

- 文本字段：标题 + 简介 + 标签
- 分词 -> TF-IDF 向量 -> 余弦相似度
  $$sim(a,b)=\frac{\vec a \cdot \vec b}{||\vec a||\,||\vec b||}$$

### 6.4 封面标签规则算法

- 文件名与标题关键词规则（如 vlog / 游戏 / 教程）
- 主色调映射（明亮/暗色/高饱和）
- 可选 OCR 文本关键词（后续）
- 输出 `tag_name + confidence`

---

## 7. 预留扩展点

- `StorageProvider`：
  - `LocalStorageProvider`（当前）
  - `OssStorageProvider`（后续启用）

- `CoverAnalyzerProvider`：
  - `RuleBasedCoverTagAnalyzer`（当前）
  - `BaiduVisionAnalyzer`（后续启用）

---

## 8. 风险与规避

- FFmpeg 不可用：启动健康检查 + 管理后台提示
- 事件丢失：客户端批量重传 + 服务端幂等去重
- 推荐冷启动：默认热门+最新兜底
- 任务失败：重试上限 + 错误记录

---

## 9. 验收标准（阶段 1）

- 可完成上传、转码、播放全链路（3档 + HLS/MP4）
- 可配置并运行封面 A/B，拿到 CTR 报表
- 可展示“相关视频”推荐（TF-IDF）
- 封面标签自动生成并可人工修正
- 管理后台可查看任务状态与错误信息
