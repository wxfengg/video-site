<template>
  <section class="home">
    <el-card class="hero">
      <h1>发现你喜欢的视频</h1>
      <p>基于当前能力先展示已发布内容，后续可接入混合推荐。</p>
      <p class="visitor">
        访客标识：<code>{{ visitorId }}</code>
      </p>
    </el-card>

    <el-card>
      <template #header>
        <div class="header-row">
          <span>视频列表</span>
          <el-input
            v-model="keyword"
            placeholder="输入关键字搜索…"
            clearable
            style="max-width: 280px"
            name="videoKeyword"
            aria-label="视频搜索关键字"
            @keyup.enter="reload"
          />
        </div>
      </template>

      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <el-empty v-if="videos.length === 0" description="暂无发布视频" />
          <div v-else class="grid">
            <VideoCard v-for="video in videos" :key="video.id" :video="video" @open="openDetail" />
          </div>

          <div class="pager-wrap">
            <el-pagination
              layout="total, prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="page"
              @current-change="onPageChange"
            />
          </div>
        </template>
      </el-skeleton>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import VideoCard from "../../components/video/VideoCard.vue"
import {
  getHomeRecommendations,
  getPublicVideos,
  sendRecommendFeedback,
  type RecommendationItem,
  type VideoListItem,
} from "../../apis/video"
import { trackClick, trackExposure } from "../../utils/tracking"
import { getOrCreateVisitorId } from "../../utils/visitor"

const router = useRouter()
const loading = ref(false)
const videos = ref<VideoListItem[]>([])
const page = ref(1)
const pageSize = 12
const total = ref(0)
const keyword = ref("")
const visitorId = ref(getOrCreateVisitorId())

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    if (!keyword.value) {
      const recs: RecommendationItem[] = await getHomeRecommendations(pageSize)
      if (recs.length > 0) {
        const all = await getPublicVideos(1, 100, "")
        const byId = new Map<string, VideoListItem>((all.records || []).map((item) => [String(item.id), item]))
        videos.value = recs
          .map((rec) => byId.get(String(rec.videoId)))
          .filter((item): item is VideoListItem => Boolean(item))
        total.value = all.total || videos.value.length
      } else {
        const data = await getPublicVideos(page.value, pageSize, keyword.value)
        videos.value = data.records || []
        total.value = data.total || 0
      }
    } else {
      const data = await getPublicVideos(page.value, pageSize, keyword.value)
      videos.value = data.records || []
      total.value = data.total || 0
    }

    videos.value.forEach((item, index) => {
      trackExposure(item.id, { scene: "home", rank: index + 1 })
      void sendRecommendFeedback(item.id, "exposure", "home")
    })
  } catch (_err) {
    ElMessage.error("加载视频列表失败，请稍后重试")
  } finally {
    loading.value = false
  }
}

async function onPageChange(value: number) {
  page.value = value
  await reload()
}

async function openDetail(id: number | string) {
  trackClick(id, { scene: "home" })
  void sendRecommendFeedback(id, "click", "home")
  await router.push(`/videos/${id}`)
}
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero h1 {
  margin: 0;
  font-size: 28px;
  text-wrap: balance;
}

.hero p {
  margin: 6px 0 0;
  color: #606266;
}

.visitor {
  margin-top: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
