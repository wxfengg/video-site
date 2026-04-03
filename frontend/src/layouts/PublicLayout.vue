<template>
  <el-container class="public-layout">
    <el-header class="header">
      <div class="inner">
        <router-link to="/" class="brand" aria-label="返回首页">
          <span class="brand-mark">VS</span>
          <span class="brand-copy">
            <span class="brand-title">Video Site</span>
            <span class="brand-subtitle">SaaS 推荐体验</span>
          </span>
        </router-link>
        <div class="header-actions">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/me/favorites" class="nav-link">我的收藏</router-link>
          <router-link to="/me/history" class="nav-link">观看历史</router-link>

          <template v-if="userSession.loggedIn">
            <span class="user-pill">{{ userSession.username }}</span>
            <el-button size="small" text @click="onLogout">退出</el-button>
          </template>
          <template v-else>
            <router-link to="/user/login" class="nav-link">登录</router-link>
            <router-link to="/user/register" class="nav-link">注册</router-link>
          </template>

          <span class="header-badge">BETA</span>
        </div>
      </div>
    </el-header>

    <el-main class="main" id="main-content" tabindex="-1">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, reactive, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { fetchUserSession, logoutUser } from "../apis/user"

const route = useRoute()
const router = useRouter()

const userSession = reactive({
  loggedIn: false,
  username: null as string | null,
})

async function syncUserSession() {
  try {
    const session = await fetchUserSession()
    userSession.loggedIn = session.loggedIn
    userSession.username = session.username
  } catch (_err) {
    userSession.loggedIn = false
    userSession.username = null
  }
}

async function onLogout() {
  await logoutUser()
  await syncUserSession()
  await router.push("/")
}

onMounted(() => {
  void syncUserSession()
})

watch(
  () => route.fullPath,
  () => {
    void syncUserSession()
  },
)
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 72px;
  background: rgba(244, 248, 255, 0.75);
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
}

.inner {
  max-width: 1200px;
  height: 72px;
  padding: 0 24px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  color: #3f4d69;
  font-size: 13px;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.62);
}

.user-pill {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: #3854a7;
  border: 1px solid rgba(78, 124, 255, 0.2);
  background: rgba(78, 124, 255, 0.1);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-secondary));
  box-shadow: 0 8px 18px rgba(78, 124, 255, 0.4);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-title {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.brand-subtitle {
  color: var(--text-muted);
  font-size: 12px;
}

.header-badge {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(78, 124, 255, 0.22);
  background: rgba(255, 255, 255, 0.7);
  color: #4b67ce;
  font-size: 12px;
  letter-spacing: 0.08em;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 26px 24px 36px;
}

@media (max-width: 768px) {
  .inner {
    padding: 0 14px;
  }

  .brand-subtitle,
  .header-badge,
  .nav-link:nth-child(2),
  .nav-link:nth-child(3),
  .user-pill {
    display: none;
  }

  .main {
    padding: 18px 14px 28px;
  }
}
</style>
