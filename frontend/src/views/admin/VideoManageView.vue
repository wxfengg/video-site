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
      <el-table-column label="操作" width="420" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="success" @click="onPublish(scope.row.id)">发布</el-button>
            <el-button size="small" type="warning" @click="onUnpublish(scope.row.id)">下线</el-button>
            <el-button v-if="scope.row.status === 'offline'" size="small" type="danger" @click="onDelete(scope.row)">
              删除
            </el-button>
            <el-button size="small" @click="openPlayer(scope.row)">预览</el-button>
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
          <input
            ref="editCoverInput"
            type="file"
            accept="image/*"
            aria-label="上传新封面"
            @change="onEditCoverFileChange"
          />
          <el-button size="small" :loading="coverUploading" @click="uploadEditCover">上传并替换封面</el-button>
        </div>
        <img v-if="editForm.coverUrl" :src="editForm.coverUrl" alt="当前封面" class="cover-preview" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import {
  deleteVideo,
  getAdminVideo,
  getAdminVideos,
  publishVideo,
  unpublishVideo,
  updateVideo,
  uploadCoverImage,
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
const editCoverInput = ref<HTMLInputElement | null>(null)
const editCoverFile = ref<File | null>(null)
const coverUploading = ref(false)
const editForm = reactive({
  title: "",
  description: "",
  coverUrl: "",
})

const MAX_COVER_SIZE_BYTES = 5 * 1024 * 1024
const ALLOWED_COVER_MIME_TYPES = new Set(["image/jpeg", "image/jpg", "image/png", "image/webp"])

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
  if (editCoverInput.value) {
    editCoverInput.value.value = ""
  }

  try {
    const detail = await getAdminVideo(row.id)
    editForm.description = detail.description || ""
    editForm.coverUrl = detail.coverUrl || editForm.coverUrl
  } catch (_err) {
    ElMessage.warning("加载视频详情失败，已使用列表数据")
  }

  editVisible.value = true
}

function onEditCoverFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] || null
  if (file && !validateCoverFile(file)) {
    target.value = ""
    editCoverFile.value = null
    return
  }
  editCoverFile.value = file
}

function validateCoverFile(file: File): boolean {
  if (file.size > MAX_COVER_SIZE_BYTES) {
    ElMessage.warning("封面图片不能超过 5MB")
    return false
  }

  const type = (file.type || "").toLowerCase()
  if (type) {
    if (!ALLOWED_COVER_MIME_TYPES.has(type)) {
      ElMessage.warning("封面仅支持 jpg/jpeg、png、webp 格式")
      return false
    }
    return true
  }

  const lowerName = file.name.toLowerCase()
  if (
    !(
      lowerName.endsWith(".jpg") ||
      lowerName.endsWith(".jpeg") ||
      lowerName.endsWith(".png") ||
      lowerName.endsWith(".webp")
    )
  ) {
    ElMessage.warning("封面仅支持 jpg/jpeg、png、webp 格式")
    return false
  }

  return true
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
    if (editCoverInput.value) {
      editCoverInput.value.value = ""
    }
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
  align-items: center;
  gap: 10px;
}

.cover-preview {
  margin-top: 10px;
  width: 220px;
  max-width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
</style>
