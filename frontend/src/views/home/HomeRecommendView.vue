<template>
  <section class="home">
    <section class="hero" aria-label="推荐引导区">
      <div class="hero-main">
        <span class="hero-kicker">智能推荐</span>
        <h1>发现你喜欢的视频</h1>
        <p class="hero-desc">基于当前能力优先展示已发布内容，后续将接入混合推荐与个性化排序。</p>
        <div class="hero-meta">
          <span class="chip">实时更新</span>
          <p class="visitor">
            访客标识：<code>{{ visitorId }}</code>
          </p>
        </div>
      </div>
      <div class="hero-glow" aria-hidden="true"></div>
    </section>

    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="header-row">
          <div>
            <h2 class="section-title">视频列表</h2>
            <p class="section-subtitle">按关键词筛选已发布内容</p>
          </div>
          <el-input
            v-model="keyword"
            placeholder="输入关键字搜索…"
            clearable
            class="search-input"
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
        total.value = Number(all.total || videos.value.length)
      } else {
        const data = await getPublicVideos(page.value, pageSize, keyword.value)
        videos.value = data.records || []
        total.value = Number(data.total || 0)
      }
    } else {
      const data = await getPublicVideos(page.value, pageSize, keyword.value)
      videos.value = data.records || []
      total.value = Number(data.total || 0)
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
  gap: 22px;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  padding: 36px;
  background: linear-gradient(135deg, #4f78ff 0%, #7456f6 58%, #00a8f5 100%);
  color: #f8fbff;
  box-shadow: 0 24px 40px rgba(79, 117, 255, 0.35);
}

.hero-main {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  height: 30px;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.18);
}

.hero h1 {
  margin: 0;
  margin-top: 14px;
  font-size: clamp(30px, 4vw, 38px);
  line-height: 1.18;
  text-wrap: balance;
}

.hero-desc {
  margin: 12px 0 0;
  max-width: 760px;
  color: rgba(245, 250, 255, 0.9);
}

.hero-meta {
  margin-top: 18px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.chip {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.2);
}

.visitor {
  margin: 0;
  color: rgba(245, 250, 255, 0.95);
}

.visitor code {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 8px;
  font-size: 12px;
  background: rgba(8, 16, 45, 0.28);
  color: #f4f7ff;
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.hero-glow {
  position: absolute;
  right: -70px;
  top: -50px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 64%);
}

.header-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.section-subtitle {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.search-input {
  max-width: 300px;
}

.list-card {
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  box-shadow: var(--shadow-soft);
  border-radius: var(--radius-lg);
}

.list-card :deep(.el-card__header) {
  border-bottom-color: rgba(224, 231, 255, 0.75);
}

.list-card :deep(.el-card__body) {
  padding-top: 18px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(136, 153, 190, 0.24);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 860px) {
  .hero {
    padding: 24px;
  }

  .hero-glow {
    display: none;
  }

  .header-row {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: 100%;
  }
}
</style>
