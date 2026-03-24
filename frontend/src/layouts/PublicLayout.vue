<template>
  <el-container class="public-layout">
    <el-header class="header">
      <div class="inner">
        <router-link to="/" class="brand">Video Site</router-link>
        <el-space>
          <el-button text @click="goLogin">管理登录</el-button>
          <el-button type="primary" @click="goAdmin">进入后台</el-button>
        </el-space>
      </div>
    </el-header>

    <el-main class="main" id="main-content" tabindex="-1">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"

const router = useRouter()

async function goLogin() {
  await router.push("/admin/login")
}

async function goAdmin() {
  await router.push("/admin")
}
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #fff;
  border-bottom: 1px solid #eceef2;
}

.inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  font-size: 20px;
  color: #303133;
  font-weight: 700;
  text-decoration: none;
}

.main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}
</style>
