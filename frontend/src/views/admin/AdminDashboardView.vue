<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card>
        <h3>当前管理员</h3>
        <p>
          <b>{{ username || "未知" }}</b>
        </p>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card>
        <h3>上传入口</h3>
        <el-button type="primary" @click="goUpload">去上传页</el-button>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card>
        <h3>视频管理</h3>
        <el-button @click="goVideos">去管理页</el-button>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { fetchAdminSession } from "../../utils/auth"

const router = useRouter()
const username = ref<string | null>(null)

onMounted(async () => {
  const session = await fetchAdminSession()
  username.value = session.username
})

async function goUpload() {
  await router.push("/admin/upload")
}

async function goVideos() {
  await router.push("/admin/videos")
}
</script>
