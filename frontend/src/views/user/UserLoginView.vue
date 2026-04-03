<template>
  <section class="user-auth-page">
    <el-card class="auth-card" shadow="never">
      <template #header>
        <div class="header-row">
          <h2>用户登录</h2>
          <router-link to="/user/register">没有账号？去注册</router-link>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="onLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive } from "vue"
import { ElMessage } from "element-plus"
import { useRoute, useRouter } from "vue-router"
import { loginUser } from "../../apis/user"

const router = useRouter()
const route = useRoute()
const form = reactive({
  username: "",
  password: "",
})

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入用户名和密码")
    return
  }

  try {
    const session = await loginUser(form.username, form.password)
    if (!session.loggedIn) {
      ElMessage.error("登录失败，请检查账号或密码")
      return
    }

    ElMessage.success("登录成功")
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/"
    await router.replace(redirect)
  } catch (_err) {
    ElMessage.error("登录失败，请稍后重试")
  }
}
</script>

<style scoped>
.user-auth-page {
  display: grid;
  place-items: center;
  min-height: calc(100vh - 120px);
}

.auth-card {
  width: min(460px, 100%);
  border-radius: var(--radius-lg);
  border: 1px solid var(--card-border);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.header-row h2 {
  margin: 0;
  font-size: 20px;
}

.submit-btn {
  width: 100%;
}
</style>
