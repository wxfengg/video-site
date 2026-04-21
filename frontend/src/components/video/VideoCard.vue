<template>
  <el-card class="video-card" shadow="hover" @click="emit('open', video.id)">
    <div class="cover-wrap">
      <img
        v-if="video.coverUrl"
        :src="video.coverUrl"
        width="320"
        height="180"
        :alt="`视频封面：${video.title}`"
        class="cover"
        loading="lazy"
      />
      <div v-else class="cover placeholder" aria-hidden="true">No Cover</div>
    </div>
    <div class="content">
      <div v-if="video.recommendReason" class="rec-reason">
        <el-tag size="small" type="warning" effect="light" class="rec-tag"
          ><span class="rec-ai">AI</span>{{ video.recommendReason }}</el-tag
        >
      </div>
      <h3 class="title">{{ video.title }}</h3>
      <div class="meta">
        <span class="status-pill">{{ mapStatus(video.status) }}</span>
        <span v-if="video.publishAt" class="publish-time">{{ formatDate(video.publishAt) }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { VideoListItem } from "../../apis/video"

const props = defineProps<{
  video: VideoListItem
}>()

const emit = defineEmits<{
  open: [id: number | string]
}>()

function formatDate(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value))
}

function mapStatus(status: string) {
  const dict: Record<string, string> = {
    published: "已发布",
    ready: "已就绪",
    transcoding: "转码中",
    draft: "草稿",
    offline: "已下线",
  }
  return dict[status] || status
}

void props
</script>

<style scoped>
.video-card {
  cursor: pointer;
  border: 1px solid rgba(221, 229, 250, 0.88);
  border-radius: 16px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.video-card:hover {
  transform: translateY(-3px);
  border-color: rgba(105, 132, 241, 0.4);
  box-shadow: 0 16px 30px rgba(80, 102, 170, 0.16);
}

.cover-wrap {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #eff3ff 0%, #dfeaff 100%);
  overflow: hidden;
  border-radius: 12px;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.28s ease;
}

.video-card:hover .cover {
  transform: scale(1.04);
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7c87a1;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.content {
  margin-top: 12px;
  min-width: 0;
}

.title {
  margin: 0;
  font-size: 16px;
  color: #1a2238;
  line-height: 1.45;
  text-wrap: balance;
  word-break: break-word;
}

.meta {
  margin: 10px 0 0;
  font-size: 12px;
  color: #8090ad;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.status-pill {
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(78, 124, 255, 0.12);
  color: #4f6ed6;
  font-weight: 600;
}

.rec-reason {
  margin-bottom: 6px;
}

.rec-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, #fff7e6 0%, #ffe8cc 100%);
  border-color: rgba(255, 166, 0, 0.35);
  color: #d46b08;
  font-weight: 600;
}

.rec-ai {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  background: linear-gradient(135deg, #ff9c6e, #ff7875);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.publish-time {
  white-space: nowrap;
}
</style>
