<template>
  <section class="login-page">
    <div class="bg-glow bg-glow-left" aria-hidden="true"></div>
    <div class="bg-glow bg-glow-right" aria-hidden="true"></div>

    <div class="login-shell">
      <aside class="intro" aria-label="登录引导">
        <p class="intro-kicker">Admin Workspace</p>
        <h1>欢迎回来</h1>
        <p class="intro-desc">登录后可管理视频上传、发布状态、A/B 实验与报表分析。</p>
        <router-link to="/" class="back-home">返回首页</router-link>
      </aside>

      <el-card class="login-card" shadow="never">
        <template #header>
          <span class="card-title">管理员登录</span>
        </template>
        <el-form label-position="top" @submit.prevent class="login-form">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="admin123" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="submit-btn" @click="onLogin">登录</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive } from "vue"
import { ElMessage } from "element-plus"
import { useRoute, useRouter } from "vue-router"
import { loginAdmin } from "../../utils/auth"

const router = useRouter()
const route = useRoute()
const form = reactive({ username: "admin", password: "admin123" })

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入账号与密码")
    return
  }

  try {
    const success = await loginAdmin(form.username, form.password)
    if (!success) {
      ElMessage.error("登录失败，请检查账号密码")
      return
    }

    ElMessage.success("登录成功")
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/admin"
    await router.replace(redirect)
  } catch (_err) {
    ElMessage.error("登录请求失败，请稍后再试")
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  padding: 24px;
}

.bg-glow {
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  filter: blur(8px);
}

.bg-glow-left {
  left: -120px;
  top: -90px;
  background: radial-gradient(circle, rgba(78, 124, 255, 0.35) 0%, transparent 72%);
}

.bg-glow-right {
  right: -110px;
  bottom: -100px;
  background: radial-gradient(circle, rgba(122, 92, 255, 0.28) 0%, transparent 72%);
}

.login-shell {
  position: relative;
  z-index: 1;
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  align-items: stretch;
}

.intro {
  border-radius: var(--radius-xl);
  padding: 36px;
  color: #f4f8ff;
  background: linear-gradient(135deg, #4b75ff 0%, #7257f5 62%, #00a6f7 100%);
  box-shadow: 0 24px 38px rgba(78, 98, 160, 0.26);
}

.intro-kicker {
  margin: 0;
  display: inline-flex;
  height: 30px;
  align-items: center;
  border-radius: 999px;
  padding: 0 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.2);
}

.intro h1 {
  margin: 16px 0 0;
  font-size: clamp(30px, 4vw, 38px);
  line-height: 1.2;
}

.intro-desc {
  margin: 14px 0 0;
  max-width: 360px;
  color: rgba(238, 245, 255, 0.92);
  line-height: 1.7;
}

.back-home {
  margin-top: 24px;
  width: fit-content;
  height: 34px;
  border-radius: 999px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: #f5f9ff;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
  transition: transform 0.18s ease;
}

.back-home:hover {
  transform: translateY(-1px);
}

.login-card {
  border: 1px solid var(--card-border);
  border-radius: var(--radius-xl);
  background: var(--card-bg);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow-soft);
}

.login-card :deep(.el-card__header) {
  border-bottom-color: rgba(221, 229, 250, 0.8);
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1d2740;
}

.login-form :deep(.el-form-item__label) {
  color: #4f5f7c;
  font-weight: 600;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(136, 153, 190, 0.28);
}

.submit-btn {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  border: none;
  font-weight: 700;
  letter-spacing: 0.03em;
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-secondary));
  box-shadow: 0 12px 20px rgba(77, 108, 212, 0.28);
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .intro {
    padding: 24px;
  }

  .intro-desc {
    max-width: none;
  }
}
</style>
