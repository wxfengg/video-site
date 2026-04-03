<template>
  <el-card>
    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <h2 class="title">{{ detail?.title }}</h2>

        <div class="actions" v-if="userLoggedIn">
          <el-button type="primary" plain size="small" @click="onFavorite">加入收藏</el-button>
          <el-button
            size="small"
            :type="likeSummary?.likedByCurrentUser ? 'danger' : 'default'"
            :loading="likeSubmitting"
            @click="onToggleLike"
          >
            {{ likeSummary?.likedByCurrentUser ? "取消点赞" : "点赞" }}
            <span v-if="likeSummary">（{{ likeSummary.likeCount }}）</span>
          </el-button>
          <span class="resume-tip" v-if="resumeProgressSec > 0">已恢复到上次进度 {{ resumeProgressSec }}s</span>
        </div>

        <div class="actions" v-else>
          <router-link to="/user/login" class="login-tip">登录后可点赞、评论与收藏</router-link>
          <span class="resume-tip" v-if="likeSummary">当前点赞数：{{ likeSummary.likeCount }}</span>
        </div>

        <div class="toolbar">
          <span class="toolbar-label">清晰度</span>
          <el-segmented v-model="selectedQuality" :options="qualityOptions" aria-label="清晰度切换" />
        </div>

        <ArtPlayerWrapper
          :hls-master-url="sources?.hlsMasterUrl"
          :mp4-1080-url="sources?.mp41080Url"
          :mp4-720-url="sources?.mp4720Url"
          :mp4-360-url="sources?.mp4360Url"
          :initial-progress-sec="resumeProgressSec"
          :quality="selectedQuality"
          @play="onPlay"
          @pause="onPause"
          @progress="onProgress"
          @ended="onEnded"
        />

        <p class="desc">{{ detail?.description || "暂无简介" }}</p>

        <el-divider />

        <section class="comments">
          <div class="comments-header">
            <h3>评论区</h3>
            <el-button size="small" @click="reloadComments" :loading="commentsLoading">刷新</el-button>
          </div>

          <div v-if="userLoggedIn" class="comment-editor">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="写下你的评论（单层评论）"
            />
            <div class="editor-actions">
              <el-button type="primary" :loading="commentSubmitting" @click="onSubmitComment">发表评论</el-button>
            </div>
          </div>
          <div v-else class="comment-login-hint">
            <router-link to="/user/login">登录后可发表评论</router-link>
          </div>

          <el-empty v-if="!commentsLoading && comments.length === 0" description="还没有评论，来抢沙发吧" />

          <el-skeleton v-if="commentsLoading" animated :rows="4" />
          <ul v-else class="comment-list">
            <li v-for="item in comments" :key="item.id" class="comment-item">
              <div class="comment-main">
                <div class="comment-meta">
                  <strong>{{ item.username }}</strong>
                  <span>{{ formatDateTime(item.createdAt) }}</span>
                </div>
                <p>{{ item.content }}</p>
              </div>
              <el-button
                v-if="userLoggedIn && currentUserId !== null && String(item.userId) === String(currentUserId)"
                size="small"
                text
                type="danger"
                @click="onDeleteComment(item.id)"
              >
                删除
              </el-button>
            </li>
          </ul>

          <div class="pager-wrap" v-if="commentsTotal > commentsPageSize">
            <el-pagination
              layout="total, prev, pager, next"
              :total="commentsTotal"
              :page-size="commentsPageSize"
              :current-page="commentsPage"
              @current-change="onCommentsPageChange"
            />
          </div>
        </section>
      </template>
    </el-skeleton>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRoute } from "vue-router"
import { ElMessage } from "element-plus"
import {
  addVideoLike,
  createVideoComment,
  deleteVideoComment,
  getVideoComments,
  getVideoDetail,
  getVideoLikeSummary,
  getVideoPlaySources,
  removeVideoLike,
  type VideoCommentItem,
  type VideoDetail,
  type VideoLikeSummary,
  type VideoPlaySources,
} from "../../apis/video"
import ArtPlayerWrapper from "../../components/player/ArtPlayerWrapper.vue"
import { trackComplete, trackPlay, trackProgress } from "../../utils/tracking"
import { addFavorite, fetchUserSession, getMyVideoProgress, updateMyVideoProgress } from "../../apis/user"

const route = useRoute()
const loading = ref(false)
const detail = ref<VideoDetail | null>(null)
const sources = ref<VideoPlaySources | null>(null)
const selectedQuality = ref("auto")
const sentProgressSec = ref(new Set<number>())
const userLoggedIn = ref(false)
const currentUserId = ref<number | null>(null)
const resumeProgressSec = ref(0)
const lastSyncedProgressSec = ref(-1)
const likeSummary = ref<VideoLikeSummary | null>(null)
const likeSubmitting = ref(false)
const comments = ref<VideoCommentItem[]>([])
const commentsLoading = ref(false)
const commentsPage = ref(1)
const commentsPageSize = 10
const commentsTotal = ref(0)
const commentSubmitting = ref(false)
const commentContent = ref("")

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

    const session = await fetchUserSession()
    userLoggedIn.value = session.loggedIn
    currentUserId.value = session.userId ? Number(session.userId) : null
    if (session.loggedIn && numericVideoId.value !== null) {
      const resume = await getMyVideoProgress(numericVideoId.value)
      resumeProgressSec.value = Math.max(0, Number(resume.progressSec || 0))
    }

    if (numericVideoId.value !== null) {
      await Promise.all([loadLikeSummary(), loadComments()])
    }
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
  void syncProgress(seconds)
}

function onEnded() {
  if (numericVideoId.value === null) {
    return
  }
  trackComplete(numericVideoId.value, { quality: selectedQuality.value })
  if (detail.value?.durationSec) {
    void syncProgress(detail.value.durationSec)
  }
}

function onPause() {
  if (numericVideoId.value === null || sentProgressSec.value.size === 0) {
    return
  }
  const latest = Math.max(...Array.from(sentProgressSec.value.values()))
  void syncProgress(latest)
}

async function syncProgress(seconds: number) {
  if (!userLoggedIn.value || numericVideoId.value === null) {
    return
  }

  const safeSeconds = Math.max(0, Math.floor(seconds))
  if (safeSeconds === lastSyncedProgressSec.value) {
    return
  }

  try {
    await updateMyVideoProgress(numericVideoId.value, safeSeconds, detail.value?.durationSec || undefined)
    lastSyncedProgressSec.value = safeSeconds
  } catch (_err) {
    // 进度回写失败不阻断播放
  }
}

async function onFavorite() {
  if (numericVideoId.value === null) {
    return
  }

  try {
    await addFavorite(numericVideoId.value)
    ElMessage.success("已加入收藏")
  } catch (_err) {
    ElMessage.error("加入收藏失败，请先登录")
  }
}

async function loadLikeSummary() {
  if (numericVideoId.value === null) {
    return
  }

  try {
    likeSummary.value = await getVideoLikeSummary(numericVideoId.value)
  } catch (_err) {
    likeSummary.value = null
  }
}

async function onToggleLike() {
  if (!userLoggedIn.value || numericVideoId.value === null) {
    ElMessage.warning("请先登录后再点赞")
    return
  }

  likeSubmitting.value = true
  try {
    if (likeSummary.value?.likedByCurrentUser) {
      await removeVideoLike(numericVideoId.value)
      ElMessage.success("已取消点赞")
    } else {
      await addVideoLike(numericVideoId.value)
      ElMessage.success("点赞成功")
    }
    await loadLikeSummary()
  } catch (_err) {
    ElMessage.error("点赞操作失败")
  } finally {
    likeSubmitting.value = false
  }
}

async function loadComments() {
  if (numericVideoId.value === null) {
    return
  }

  commentsLoading.value = true
  try {
    const result = await getVideoComments(numericVideoId.value, commentsPage.value, commentsPageSize)
    comments.value = result.records || []
    commentsTotal.value = Number(result.total || 0)
  } catch (_err) {
    comments.value = []
    commentsTotal.value = 0
  } finally {
    commentsLoading.value = false
  }
}

async function reloadComments() {
  commentsPage.value = 1
  await loadComments()
}

async function onCommentsPageChange(nextPage: number) {
  commentsPage.value = nextPage
  await loadComments()
}

async function onSubmitComment() {
  if (!userLoggedIn.value || numericVideoId.value === null) {
    ElMessage.warning("请先登录")
    return
  }

  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning("评论内容不能为空")
    return
  }

  commentSubmitting.value = true
  try {
    await createVideoComment(numericVideoId.value, content)
    commentContent.value = ""
    ElMessage.success("评论成功")
    await reloadComments()
  } catch (_err) {
    ElMessage.error("评论失败")
  } finally {
    commentSubmitting.value = false
  }
}

async function onDeleteComment(commentId: number | string) {
  if (!userLoggedIn.value || numericVideoId.value === null) {
    return
  }

  try {
    await deleteVideoComment(numericVideoId.value, commentId)
    ElMessage.success("评论已删除")
    await loadComments()
  } catch (_err) {
    ElMessage.error("删除评论失败")
  }
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-"
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value))
}
</script>

<style scoped>
.title {
  margin: 0 0 14px;
  font-size: 26px;
  text-wrap: balance;
}

.actions {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.resume-tip {
  color: #66758f;
  font-size: 13px;
}

.login-tip {
  color: #4f6ed6;
  font-size: 13px;
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

.comments {
  margin-top: 12px;
}

.comments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comments-header h3 {
  margin: 0;
}

.comment-editor {
  margin-top: 12px;
}

.editor-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.comment-login-hint {
  margin-top: 12px;
}

.comment-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  border: 1px solid #e7ecf7;
  border-radius: 10px;
  padding: 10px;
  background: #fafcff;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #7d8a9f;
  font-size: 12px;
}

.comment-main p {
  margin: 6px 0 0;
  line-height: 1.7;
  word-break: break-word;
}

.pager-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
