# 验收清单（MVP）

## 1) 上传 → 转码 → 播放

- [ ] 管理员登录后可进入 `视频上传` 页面
- [ ] 上传成功后显示 `videoId` 与 `transcodeTaskId`
- [ ] 视频状态从 `transcoding` 最终进入 `ready`（或 `published`）
- [ ] 在前台 `/videos/{id}` 可正常播放（HLS 优先，MP4 兜底）

## 2) 视频管理

- [ ] 管理页可分页、筛选、搜索视频
- [ ] 编辑标题/简介/封面后可保存
- [ ] 发布后前台可见，下线后前台不可见

## 3) 事件埋点

- [ ] 首页曝光、点击可产生事件
- [ ] 播放页触发 play/progress/complete 事件
- [ ] 后端 `event_log` 可见批量入库数据

## 4) A/B 实验与报表

- [ ] 可创建实验并配置 A/B 变体与流量比（总和=100）
- [ ] 可启动/停止实验
- [ ] 分流结果稳定（同 visitor + 同实验得到同变体）
- [ ] CTR 报表可展示每个变体的曝光UV、点击UV、CTR

## 5) 封面标签分析

- [ ] 可提交封面分析任务（rule/baidu_vision）
- [ ] Worker 可处理 pending 任务并回写 `video_tag`
- [ ] 支持人工修正标签（manual）

## 6) 推荐模块

- [ ] 小时级推荐重建 worker 可执行
- [ ] 首页在无关键词时优先拉取推荐结果
- [ ] 推荐反馈接口可回流行为事件

## 7) 构建与基础可运行性

- [ ] 后端 `mvn -DskipTests compile` 通过
- [ ] 前端 `pnpm build` 通过
- [ ] 关键管理页面路由可访问（upload/videos/ab-experiments/ab-reports）
