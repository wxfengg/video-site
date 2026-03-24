<template>
  <el-card>
    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <h2 class="title">{{ detail?.title }}</h2>

        <div class="toolbar">
          <span class="toolbar-label">清晰度</span>
          <el-segmented v-model="selectedQuality" :options="qualityOptions" aria-label="清晰度切换" />
        </div>

        <ArtPlayerWrapper
          :hls-master-url="sources?.hlsMasterUrl"
          :mp4-1080-url="sources?.mp41080Url"
          :mp4-720-url="sources?.mp4720Url"
          :mp4-360-url="sources?.mp4360Url"
          :quality="selectedQuality"
          @play="onPlay"
          @progress="onProgress"
          @ended="onEnded"
        />

        <p class="desc">{{ detail?.description || "暂无简介" }}</p>
      </template>
    </el-skeleton>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRoute } from "vue-router"
import { ElMessage } from "element-plus"
import { getVideoDetail, getVideoPlaySources, type VideoDetail, type VideoPlaySources } from "../../apis/video"
import ArtPlayerWrapper from "../../components/player/ArtPlayerWrapper.vue"
import { trackComplete, trackPlay, trackProgress } from "../../utils/tracking"

const route = useRoute()
const loading = ref(false)
const detail = ref<VideoDetail | null>(null)
const sources = ref<VideoPlaySources | null>(null)
const selectedQuality = ref("auto")
const sentProgressSec = ref(new Set<number>())

const qualityOptions = [
  { label: "自动", value: "auto" },
  { label: "1080P", value: "1080" },
  { label: "720P", value: "720" },
  { label: "360P", value: "360" },
]

const videoId = computed(() => String(route.params.id || ""))
const numericVideoId = computed(() => {
  const value = Number(videoId.value)
  return Number.isSafeInteger(value) ? value : null
})

onMounted(async () => {
  loading.value = true
  try {
    const [detailData, sourceData] = await Promise.all([
      getVideoDetail(videoId.value),
      getVideoPlaySources(videoId.value),
    ])
    detail.value = detailData
    sources.value = sourceData
  } catch (_err) {
    ElMessage.error("加载播放页失败，请稍后再试")
  } finally {
    loading.value = false
  }
})

function onPlay() {
  if (numericVideoId.value === null) {
    return
  }
  trackPlay(numericVideoId.value, { quality: selectedQuality.value })
}

function onProgress(seconds: number) {
  if (numericVideoId.value === null || seconds < 5 || seconds % 5 !== 0) {
    return
  }

  if (sentProgressSec.value.has(seconds)) {
    return
  }

  sentProgressSec.value.add(seconds)
  trackProgress(numericVideoId.value, seconds, { quality: selectedQuality.value })
}

function onEnded() {
  if (numericVideoId.value === null) {
    return
  }
  trackComplete(numericVideoId.value, { quality: selectedQuality.value })
}
</script>

<style scoped>
.title {
  margin: 0 0 14px;
  font-size: 26px;
  text-wrap: balance;
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-label {
  color: #606266;
  font-size: 14px;
}

.desc {
  margin-top: 14px;
  line-height: 1.7;
  color: #606266;
  word-break: break-word;
}
</style>
