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
      <h3 class="title">{{ video.title }}</h3>
      <p class="meta">
        <span>状态：{{ video.status }}</span>
        <span v-if="video.publishAt">发布时间：{{ formatDate(video.publishAt) }}</span>
      </p>
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

void props
</script>

<style scoped>
.video-card {
  cursor: pointer;
}

.cover-wrap {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #f2f3f5;
  overflow: hidden;
  border-radius: 8px;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.content {
  margin-top: 10px;
  min-width: 0;
}

.title {
  margin: 0;
  font-size: 16px;
  line-height: 1.4;
  text-wrap: balance;
  word-break: break-word;
}

.meta {
  margin: 6px 0 0;
  color: #909399;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
</style>
