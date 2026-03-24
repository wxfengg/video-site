# 个人视频网站内容迭代与智能分析系统 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在 Spring Boot + Vue 3 技术栈下，完成视频上传转码播放、A/B 测试、TF-IDF 推荐、封面规则标签与管理后台的最小可用系统。  
**Architecture:** 采用单体后端 + 前端 SPA 架构，使用 DB 任务表驱动异步转码/分析/推荐重建；通过 Provider 抽象预留 OSS 与百度云视觉接入点。  
**Tech Stack:** Vue 3, Vite, Pinia, Element Plus, Artplayer, Spring Boot, MyBatis-Plus, MySQL, FFmpeg

---

### Task 1: 工程初始化与目录骨架

**Files:**

- Create: `backend/`（Spring Boot 工程）
- Create: `frontend/`（Vite Vue3 工程）
- Create: `docs/plans/2026-03-23-personal-video-site-design.md`
- Create: `docs/plans/2026-03-23-personal-video-site-implementation-plan.md`

**Step 1: 初始化后端工程**

- 创建 Spring Boot + MyBatis-Plus 基础工程

**Step 2: 初始化前端工程**

- 创建 Vue3 + Vite + Pinia + Router + Element Plus

**Step 3: 建立分层目录**

- 后端：controller/service/repository/domain/infrastructure/common
- 前端：views/components/stores/apis/router/utils

**Step 4: 运行基础启动验证**

- 后端启动成功；前端首页可访问

**Step 5: Commit**

- `chore: bootstrap frontend and backend skeleton`

---

### Task 2: 数据库 schema 与基础迁移

**Files:**

- Create: `backend/src/main/resources/db/migration/V1__init_core_tables.sql`
- Create: `backend/src/main/resources/db/migration/V2__init_event_ab_recommend_tables.sql`
- Modify: `backend/src/main/resources/application.yml`

**Step 1: 编写核心表迁移**

- 完成 video/video_file/video_transcode_task/video_variant/video_play_source/admin_user/visitor_profile

**Step 2: 编写行为与推荐相关迁移**

- 完成 event_log/ab_experiment/ab_variant/ab_assignment/video_tag/cover_analysis_task/video_tfidf_profile/video_similarity/recommendation_result

**Step 3: 配置迁移工具**

- 配置 Flyway 或 Liquibase（二选一）

**Step 4: 本地执行迁移并检查表结构**

**Step 5: Commit**

- `feat: add database schema migrations`

---

### Task 3: 统一 API 响应与错误码体系

**Files:**

- Create: `backend/src/main/java/.../common/api/ApiResponse.java`
- Create: `backend/src/main/java/.../common/api/ErrorCode.java`
- Create: `backend/src/main/java/.../common/exception/GlobalExceptionHandler.java`

**Step 1: 定义统一返回结构**

- `code/message/data/traceId`

**Step 2: 定义业务错误码**

- 上传失败、转码失败、资源不存在、参数错误、权限错误等

**Step 3: 全局异常处理**

- 参数校验异常、业务异常、未知异常

**Step 4: 编写接口层单测/集成验证**

**Step 5: Commit**

- `feat: add unified api response and exception handling`

---

### Task 4: 匿名访客标识与管理员认证

**Files:**

- Create: `frontend/src/utils/visitor.ts`
- Create: `backend/src/main/java/.../auth/*`
- Modify: `frontend/src/router/index.ts`

**Step 1: visitor_id 生成与持久化**

- 首次访问生成 UUID，写入 Cookie(180d)+localStorage

**Step 2: 后端读取 visitor_id**

- 统一拦截器提取并注入上下文

**Step 3: 管理员登录机制**

- 简单 JWT 或 Session（二选一）

**Step 4: 后台路由守卫**

**Step 5: Commit**

- `feat: add visitor identity and admin auth`

---

### Task 5: 上传接口与存储抽象

**Files:**

- Create: `backend/src/main/java/.../storage/StorageProvider.java`
- Create: `backend/src/main/java/.../storage/LocalStorageProvider.java`
- Create: `backend/src/main/java/.../storage/OssStorageProvider.java`（占位）
- Create: `backend/src/main/java/.../video/controller/VideoUploadController.java`

**Step 1: 定义 StorageProvider 抽象**

- `put/getSignedUrl/delete`

**Step 2: 实现本地存储 provider**

**Step 3: OSS provider 预留实现**

- 读取配置但默认不启用

**Step 4: 实现 upload init/complete 接口**

**Step 5: Commit**

- `feat: add upload api with pluggable storage provider`

---

### Task 6: FFmpeg 转码任务（360/720/1080 + HLS/MP4）

**Files:**

- Create: `backend/src/main/java/.../transcode/*`
- Create: `backend/src/main/java/.../scheduler/TranscodeWorker.java`
- Modify: `backend/src/main/resources/application.yml`

**Step 1: 定义转码任务状态机**

- pending/running/success/failed

**Step 2: 实现 FFmpeg 命令构造器**

- 输出 3 档分辨率，产出 HLS + MP4

**Step 3: 实现 Worker 拉取执行**

- 失败重试与错误记录

**Step 4: 回写变体与播放源表**

**Step 5: Commit**

- `feat: implement ffmpeg transcode worker and play sources`

---

### Task 7: 视频管理与播放 API

**Files:**

- Create: `backend/src/main/java/.../video/controller/VideoController.java`
- Create: `backend/src/main/java/.../video/service/VideoService.java`

**Step 1: 实现列表/详情/编辑/发布/下线接口**

**Step 2: 实现播放源聚合接口**

- 同时返回 HLS master 与 MP4 各清晰度

**Step 3: 添加分页与筛选**

**Step 4: 编写接口测试**

**Step 5: Commit**

- `feat: add video management and playback apis`

---

### Task 8: 前端上传页与播放页

**Files:**

- Create: `frontend/src/views/admin/VideoUploadView.vue`
- Create: `frontend/src/views/video/VideoPlayerView.vue`
- Create: `frontend/src/components/player/ArtPlayerWrapper.vue`
- Create: `frontend/src/apis/video.ts`

**Step 1: 上传页实现**

- 进度展示、状态轮询、失败重试

**Step 2: 播放页实现**

- HLS 优先，MP4 兜底，清晰度切换

**Step 3: 管理页基本操作**

- 编辑、发布、下线

**Step 4: 手工回归测试**

**Step 5: Commit**

- `feat: add upload and player frontend pages`

---

### Task 9: 埋点采集 SDK 与事件上报

**Files:**

- Create: `frontend/src/utils/tracking.ts`
- Create: `backend/src/main/java/.../tracking/controller/EventController.java`
- Create: `backend/src/main/java/.../tracking/service/EventService.java`

**Step 1: 前端埋点 SDK**

- 曝光/点击/播放/5s 进度/完播

**Step 2: 批量队列与失败重传**

**Step 3: 后端批量接收与入库**

**Step 4: 幂等策略（可选 event_id）**

**Step 5: Commit**

- `feat: implement tracking sdk and event collect api`

---

### Task 10: A/B 实验配置与稳定分流

**Files:**

- Create: `backend/src/main/java/.../ab/*`
- Create: `frontend/src/views/admin/AbExperimentView.vue`

**Step 1: 实验 CRUD + 启停接口**

**Step 2: 分流算法实现**

- hash(visitor_id + experiment_id) 稳定分流

**Step 3: assignment 持久化与幂等**

**Step 4: 前端实验管理页**

**Step 5: Commit**

- `feat: add ab experiment and deterministic assignment`

---

### Task 11: CTR 报表与实验评估

**Files:**

- Create: `backend/src/main/java/.../ab/service/AbReportService.java`
- Create: `frontend/src/views/admin/AbReportView.vue`

**Step 1: CTR 聚合 SQL/服务逻辑**

**Step 2: 报表接口输出**

- A/B 各自曝光 UV、点击 UV、CTR

**Step 3: 前端可视化展示**

**Step 4: 验证与样本数据测试**

**Step 5: Commit**

- `feat: add ctr report for ab experiments`

---

### Task 12: 封面规则标签分析模块

**Files:**

- Create: `backend/src/main/java/.../cover/CoverTagAnalyzer.java`
- Create: `backend/src/main/java/.../cover/RuleBasedCoverTagAnalyzer.java`
- Create: `backend/src/main/java/.../cover/BaiduVisionAnalyzer.java`（占位）
- Create: `backend/src/main/java/.../cover/CoverTagController.java`

**Step 1: 定义分析器抽象接口**

**Step 2: 实现规则算法**

- 标题/文件名关键词 + 主色调规则

**Step 3: 异步任务执行与结果回写**

**Step 4: 人工修正接口**

**Step 5: Commit**

- `feat: implement rule-based cover tag analysis with ai extension point`

---

### Task 13: TF-IDF 相关视频与混合推荐

**Files:**

- Create: `backend/src/main/java/.../recommend/*`
- Create: `backend/src/main/java/.../scheduler/RecommendRebuildWorker.java`
- Create: `frontend/src/views/home/HomeRecommendView.vue`

**Step 1: 小时级向量重建任务**

- 文本源：标题+简介+标签

**Step 2: 相似度计算与落库**

**Step 3: 首页混合推荐实现**

- `score = α*content + β*cf + γ*hot`

**Step 4: 推荐反馈回流接口**

**Step 5: Commit**

- `feat: add tfidf related and hybrid recommendation`

---

### Task 14: 管理后台整合与验收脚本

**Files:**

- Create: `frontend/src/views/admin/DashboardView.vue`
- Create: `backend/src/test/.../integration/*`
- Create: `docs/acceptance-checklist.md`

**Step 1: 后台导航整合**

- 视频、实验、转码任务、标签结果

**Step 2: 集成测试关键链路**

- 上传->转码->播放；实验->报表；标签->修正

**Step 3: 验收清单固化**

**Step 4: 端到端演示脚本准备**

**Step 5: Commit**

- `chore: integrate admin dashboard and acceptance checklist`

---

### Task 15: 配置模板与交付说明

**Files:**

- Create: `backend/.env.example`
- Create: `frontend/.env.example`
- Create: `README.md`

**Step 1: 后端环境变量模板**

- DB、FFMPEG、STORAGE*PROVIDER、OSS*\_、BAIDU\_\_、SCHEDULER\_\*

**Step 2: 前端环境变量模板**

- API Base URL、埋点开关等

**Step 3: README 编写**

- 本地启动、数据初始化、演示流程

**Step 4: 全链路最终验证**

**Step 5: Commit**

- `docs: add env templates and setup guide`
