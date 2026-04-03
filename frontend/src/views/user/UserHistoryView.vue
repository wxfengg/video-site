<template>
  <section class="user-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <h2>观看历史</h2>
          <el-button @click="reload" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="title" label="视频标题" min-width="220" />
        <el-table-column label="进度" min-width="240">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="Math.round(Number(scope.row.completionRate || 0) * 100)" :stroke-width="10" />
              <span>{{ scope.row.lastProgressSec || 0 }}s</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="完播(90%)" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.completed90 ? 'success' : 'info'">{{
              scope.row.completed90 ? "已达成" : "未达成"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近观看" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.lastWatchedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openVideo(scope.row.id)">继续观看</el-button>
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
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { ElMessage } from "element-plus"
import { useRouter } from "vue-router"
import { listMyHistory, type UserWatchHistoryItem } from "../../apis/user"

const router = useRouter()
const loading = ref(false)
const rows = ref<UserWatchHistoryItem[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    const result = await listMyHistory(page.value, pageSize)
    rows.value = result.records || []
    total.value = Number(result.total || 0)
  } catch (_err) {
    ElMessage.error("加载观看历史失败")
  } finally {
    loading.value = false
  }
}

async function onPageChange(nextPage: number) {
  page.value = nextPage
  await reload()
}

async function openVideo(videoId: number | string) {
  await router.push(`/videos/${videoId}`)
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
.user-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-row h2 {
  margin: 0;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-cell :deep(.el-progress) {
  flex: 1;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
