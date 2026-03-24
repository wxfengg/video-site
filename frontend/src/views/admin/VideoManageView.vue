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
            @change="reload"
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
            @keyup.enter="reload"
          />
        </el-space>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="160" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="success" @click="onPublish(scope.row.id)">发布</el-button>
            <el-button size="small" type="warning" @click="onUnpublish(scope.row.id)">下线</el-button>
            <el-button size="small" @click="openPlayer(scope.row)">预览</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
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
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { getAdminVideos, publishVideo, unpublishVideo, updateVideo, type VideoListItem } from "../../apis/video"

const router = useRouter()
const loading = ref(false)
const rows = ref<VideoListItem[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const status = ref("")
const keyword = ref("")

const editVisible = ref(false)
const editId = ref<string | number | null>(null)
const editForm = reactive({
  title: "",
  description: "",
  coverUrl: "",
})

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    const data = await getAdminVideos(page.value, pageSize, status.value, keyword.value)
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (_err) {
    ElMessage.error("加载管理列表失败")
  } finally {
    loading.value = false
  }
}

async function onPageChange(next: number) {
  page.value = next
  await reload()
}

function openEdit(row: VideoListItem) {
  editId.value = row.id
  editForm.title = row.title || ""
  editForm.description = ""
  editForm.coverUrl = row.coverUrl || ""
  editVisible.value = true
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
</style>
