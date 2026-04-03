<template>
  <section class="user-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <h2>我的收藏</h2>
          <el-button @click="reload" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="title" label="视频标题" min-width="220" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="收藏时间" width="180">
          <template #default="scope">{{ formatDateTime(scope.row.favoritedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-space>
              <el-button size="small" type="primary" @click="openVideo(scope.row.id)">观看</el-button>
              <el-button size="small" type="danger" plain @click="remove(scope.row.id)">取消收藏</el-button>
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
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { ElMessage } from "element-plus"
import { useRouter } from "vue-router"
import { listMyFavorites, removeFavorite, type UserFavoriteItem } from "../../apis/user"

const router = useRouter()
const loading = ref(false)
const rows = ref<UserFavoriteItem[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    const result = await listMyFavorites(page.value, pageSize)
    rows.value = result.records || []
    total.value = Number(result.total || 0)
  } catch (_err) {
    ElMessage.error("加载收藏列表失败")
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

async function remove(videoId: number | string) {
  try {
    await removeFavorite(videoId)
    ElMessage.success("已取消收藏")
    await reload()
  } catch (_err) {
    ElMessage.error("取消收藏失败")
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

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
