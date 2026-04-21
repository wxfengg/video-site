<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>视频管理</span>
        <el-space>
          <el-select
            v-model="status"
            placeholder="状态筛选"
            style="width: 140px"
            aria-label="视频状态筛选"
            @change="onFilterChange"
          >
            <el-option label="全部" value="" />
            <el-option label="草稿" value="draft" />
            <el-option label="转码中" value="transcoding" />
            <el-option label="就绪" value="ready" />
            <el-option label="已发布" value="published" />
            <el-option label="已下线" value="offline" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="搜索标题/简介…"
            clearable
            style="width: 220px"
            name="adminVideoKeyword"
            aria-label="管理端视频搜索"
            @keyup.enter="onFilterChange"
            @clear="onFilterChange"
          />
        </el-space>
      </div>
    </template>

    <el-table :data="pagedRows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="160" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="480" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button
              v-if="scope.row.status !== 'published'"
              size="small"
              type="success"
              @click="onPublish(scope.row.id)"
            >
              发布
            </el-button>
            <el-button size="small" type="warning" @click="onUnpublish(scope.row.id)">下线</el-button>
            <el-button v-if="scope.row.status === 'offline'" size="small" type="danger" @click="onDelete(scope.row)">
              删除
            </el-button>
            <el-button size="small" @click="openPlayer(scope.row)">预览</el-button>
            <el-button size="small" @click="openComments(scope.row)">评论</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        :page-sizes="[10, 20, 50]"
        @size-change="onPageSizeChange"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="editVisible" title="编辑视频" width="520px">
    <el-form label-width="90px">
      <el-form-item label="标题">
        <el-input v-model="editForm.title" maxlength="255" name="editTitle" aria-label="编辑标题" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input
          v-model="editForm.description"
          type="textarea"
          :rows="4"
          maxlength="2000"
          name="editDescription"
          aria-label="编辑简介"
        />
      </el-form-item>
      <el-form-item label="封面">
        <el-input v-model="editForm.coverUrl" maxlength="512" name="editCoverUrl" aria-label="编辑封面链接" />
        <div class="cover-edit-actions">
          <ImageUploadSelector
            v-model="editCoverFile"
            :preview-url="editForm.coverUrl"
            button-text="选择封面图片"
            preview-alt="封面预览"
          />
          <el-button size="small" :loading="coverUploading" @click="uploadEditCover">上传并替换封面</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="commentsVisible" title="视频评论管理" width="720px">
    <el-table :data="commentsRows" v-loading="commentsLoading" border size="small">
      <el-table-column prop="username" label="用户" width="140" />
      <el-table-column label="时间" width="160">
        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button
            size="small"
            text
            :type="scope.row.pinned ? 'info' : 'primary'"
            @click="onTogglePinComment(scope.row)"
          >
            {{ scope.row.pinned ? "取消置顶" : "置顶" }}
          </el-button>
          <el-button size="small" type="danger" text @click="onDeleteComment(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager-wrap">
      <el-pagination
        layout="total, prev, pager, next"
        :total="commentsTotal"
        :page-size="commentsPageSize"
        :current-page="commentsPage"
        @current-change="onCommentsPageChange"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import ImageUploadSelector from "../../components/image/ImageUploadSelector.vue"
import {
  deleteAdminVideoComment,
  deleteVideo,
  getAdminVideo,
  getAdminVideoComments,
  getAdminVideos,
  pinAdminVideoComment,
  publishVideo,
  unpublishVideo,
  unpinAdminVideoComment,
  updateVideo,
  uploadCoverImage,
  type VideoCommentItem,
  type VideoListItem,
} from "../../apis/video"

const router = useRouter()
const loading = ref(false)
const rows = ref<VideoListItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const status = ref("")
const keyword = ref("")

const total = computed(() => rows.value.length)
const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return rows.value.slice(start, start + pageSize.value)
})

const editVisible = ref(false)
const editId = ref<string | number | null>(null)
const editCoverFile = ref<File | null>(null)
const coverUploading = ref(false)
const editForm = reactive({
  title: "",
  description: "",
  coverUrl: "",
})

const commentsVisible = ref(false)
const commentsVideoId = ref<string | number | null>(null)
const commentsLoading = ref(false)
const commentsRows = ref<VideoCommentItem[]>([])
const commentsPage = ref(1)
const commentsPageSize = ref(10)
const commentsTotal = ref(0)

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    const data = await getAdminVideos(1, 1000, status.value, keyword.value)
    rows.value = data.records || []

    const maxPage = Math.max(1, Math.ceil(rows.value.length / pageSize.value))
    if (page.value > maxPage) {
      page.value = maxPage
    }
  } catch (_err) {
    ElMessage.error("加载管理列表失败")
  } finally {
    loading.value = false
  }
}

async function onFilterChange() {
  page.value = 1
  await reload()
}

async function onPageSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
}

async function onPageChange(next: number) {
  page.value = next
}

async function openEdit(row: VideoListItem) {
  editId.value = row.id
  editForm.title = row.title || ""
  editForm.description = ""
  editForm.coverUrl = row.coverUrl || ""
  editCoverFile.value = null

  try {
    const detail = await getAdminVideo(row.id)
    editForm.description = detail.description || ""
    editForm.coverUrl = detail.coverUrl || editForm.coverUrl
  } catch (_err) {
    ElMessage.warning("加载视频详情失败，已使用列表数据")
  }

  editVisible.value = true
}

async function uploadEditCover() {
  if (!editId.value) {
    return
  }
  if (!editCoverFile.value) {
    ElMessage.warning("请先选择封面图片")
    return
  }

  coverUploading.value = true
  try {
    const result = await uploadCoverImage(editId.value, editCoverFile.value)
    editForm.coverUrl = result.coverUrl
    ElMessage.success("封面上传成功")
    editCoverFile.value = null
  } catch (err) {
    const message = err instanceof Error ? err.message : "封面上传失败"
    ElMessage.error(message)
  } finally {
    coverUploading.value = false
  }
}

async function submitEdit() {
  if (!editId.value) return
  try {
    await updateVideo(editId.value, {
      title: editForm.title,
      description: editForm.description,
      coverUrl: editForm.coverUrl,
    })
    ElMessage.success("保存成功")
    editVisible.value = false
    await reload()
  } catch (_err) {
    ElMessage.error("保存失败")
  }
}

async function onPublish(videoId: number | string) {
  try {
    await publishVideo(videoId)
    ElMessage.success("发布成功")
    await reload()
  } catch (_err) {
    ElMessage.error("发布失败")
  }
}

async function onUnpublish(videoId: number | string) {
  try {
    await unpublishVideo(videoId)
    ElMessage.success("下线成功")
    await reload()
  } catch (_err) {
    ElMessage.error("下线失败")
  }
}

async function onDelete(row: VideoListItem) {
  if (row.status !== "offline") {
    ElMessage.warning("仅支持删除已下线视频")
    return
  }

  try {
    await ElMessageBox.confirm("删除后不可恢复，确认删除该视频吗？", "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    })
  } catch (_err) {
    return
  }

  try {
    await deleteVideo(row.id)
    ElMessage.success("删除成功")
    await reload()
  } catch (err) {
    const message = err instanceof Error ? err.message : "删除失败"
    ElMessage.error(message)
  }
}

async function openPlayer(row: VideoListItem) {
  if (row.status !== "ready" && row.status !== "published") {
    ElMessage.warning("当前视频未就绪，需转码完成后才能预览")
    return
  }
  await router.push(`/videos/${row.id}`)
}

async function openComments(row: VideoListItem) {
  commentsVideoId.value = row.id
  commentsPage.value = 1
  commentsPageSize.value = 10
  commentsVisible.value = true
  await loadComments()
}

async function loadComments() {
  if (!commentsVideoId.value) return
  commentsLoading.value = true
  try {
    const result = await getAdminVideoComments(commentsVideoId.value, commentsPage.value, commentsPageSize.value)
    commentsRows.value = result.records || []
    commentsTotal.value = Number(result.total || 0)
  } catch (_err) {
    ElMessage.error("加载评论失败")
    commentsRows.value = []
    commentsTotal.value = 0
  } finally {
    commentsLoading.value = false
  }
}

async function onCommentsPageChange(nextPage: number) {
  commentsPage.value = nextPage
  await loadComments()
}

async function onTogglePinComment(row: VideoCommentItem) {
  if (!commentsVideoId.value) return
  try {
    if (row.pinned) {
      await unpinAdminVideoComment(commentsVideoId.value, row.id)
      ElMessage.success("已取消置顶")
    } else {
      await pinAdminVideoComment(commentsVideoId.value, row.id)
      ElMessage.success("置顶成功")
    }
    await loadComments()
  } catch (err) {
    const message = err instanceof Error ? err.message : "操作失败"
    ElMessage.error(message)
  }
}

async function onDeleteComment(commentId: number | string) {
  if (!commentsVideoId.value) return
  try {
    await ElMessageBox.confirm("确认删除该评论吗？此操作不可恢复。", "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    })
  } catch (_err) {
    return
  }
  try {
    await deleteAdminVideoComment(commentsVideoId.value, commentId)
    ElMessage.success("删除成功")
    await loadComments()
  } catch (err) {
    const message = err instanceof Error ? err.message : "删除失败"
    ElMessage.error(message)
  }
}

function formatDateTime(value: string | null) {
  if (!value) return "-"
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
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.cover-edit-actions {
  margin-top: 8px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
</style>
