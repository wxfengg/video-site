<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">后台管理</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/admin">控制台</el-menu-item>
        <el-menu-item index="/admin/upload">视频上传</el-menu-item>
        <el-menu-item index="/admin/videos">视频管理</el-menu-item>
        <el-menu-item index="/admin/ab-experiments">A/B 实验</el-menu-item>
        <el-menu-item index="/admin/ab-reports">A/B 报表</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="title">管理员后台</div>
        <el-space>
          <span class="user">{{ username || "Admin" }}</span>
          <el-button @click="onLogout">退出</el-button>
        </el-space>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { fetchAdminSession, logoutAdmin } from "../utils/auth"

const route = useRoute()
const router = useRouter()
const username = ref<string | null>(null)

const active = computed(() => route.path)

onMounted(async () => {
  const session = await fetchAdminSession()
  username.value = session.username
})

async function onLogout() {
  await logoutAdmin()
  ElMessage.success("已退出登录")
  await router.replace("/admin/login")
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  border-bottom: 1px solid #ebeef5;
}

.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.user {
  color: #606266;
}

.main {
  background: #f7f8fa;
}
</style>
