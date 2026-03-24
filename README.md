# video-site

个人视频网站毕业设计项目（前后端分离）：

- 前端：Vue 3 + Vite + Pinia + Vue Router + Element Plus
- 后端：Spring Boot + MyBatis-Plus + MySQL

当前已完成到：上传/转码/播放、视频管理、埋点上报、A/B 实验与 CTR 报表、封面标签分析（规则版）、TF-IDF+混合推荐基础链路。

## 包管理器约定

本项目前端统一使用 **pnpm**。

- 前端声明：`frontend/package.json` 中 `packageManager: pnpm@10.13.1`
- 后端不使用 pnpm（后端使用 Maven）

## 目录结构

- `frontend`：前端工程
- `backend`：后端工程
- `docs/plans`：设计与实施计划
- `docs/acceptance-checklist.md`：验收清单

## 环境准备

- Node.js 14+（建议 18+，当前项目可在 14 运行）
- pnpm 10+
- JDK 17
- Maven 3.6+
- MySQL 8（后续任务需要）

## 环境变量模板

- 后端模板：`backend/.env.example`
- 前端模板：`frontend/.env.example`

建议复制后按本机环境填写：

- `backend/.env`（或在 IDE/终端注入同名环境变量）
- `frontend/.env.local`

## 快速开始

### 1) 安装依赖（前端）

先进入前端目录执行：

- `cd frontend`
- `pnpm install`

### 2) 启动前端

- `cd frontend`
- `pnpm dev`

### 3) 构建前端

- `cd frontend`
- `pnpm build`

### 4) 启动后端

- `mvn -f backend/pom.xml spring-boot:run`

### 5) 打包后端

- `mvn -f backend/pom.xml -DskipTests package`

## 配置说明

关键后端配置项：

- `FLYWAY_ENABLED`：本地默认建议 `false`（MySQL 8.4 兼容阶段）
- `FFMPEG_BIN`：ffmpeg 可执行路径
- `TRANSCODE_WORKER_CRON`：转码任务轮询
- `COVER_ANALYSIS_WORKER_CRON`：封面分析轮询
- `RECOMMEND_WORKER_CRON`：推荐重建轮询

关键前端配置项：

- `VITE_API_BASE_URL`：后端 API 地址
- `VITE_TRACKING_ENABLED`：埋点开关（预留）

## 关键页面路由

- 前台：`/`、`/videos/:id`
- 后台：`/admin`、`/admin/upload`、`/admin/videos`、`/admin/ab-experiments`、`/admin/ab-reports`

## 快速验收建议

按 `docs/acceptance-checklist.md` 执行：

1. 上传视频并观察转码状态
2. 发布后在前台播放
3. 创建并启动 A/B 实验
4. 查看 CTR 报表
5. 验证埋点与推荐回流

> 如需一键演示脚本，可在后续任务补充。
