<template>
  <section class="user-auth-page">
    <el-card class="auth-card" shadow="never">
      <template #header>
        <div class="header-row">
          <h2>用户注册</h2>
          <router-link to="/user/login">已有账号？去登录</router-link>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="3-64位，字母数字下划线" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="onRegister">注册并登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive } from "vue"
import { ElMessage } from "element-plus"
import { useRouter } from "vue-router"
import { registerUser } from "../../apis/user"

const router = useRouter()
const form = reactive({
  username: "",
  password: "",
  confirmPassword: "",
})

async function onRegister() {
  if (!form.username || !form.password || !form.confirmPassword) {
    ElMessage.warning("请完整填写注册信息")
    return
  }

  if (form.password !== form.confirmPassword) {
    ElMessage.warning("两次输入的密码不一致")
    return
  }

  try {
    const session = await registerUser(form.username, form.password)
    if (!session.loggedIn) {
      ElMessage.error("注册失败，请稍后重试")
      return
    }

    ElMessage.success("注册成功，已自动登录")
    await router.replace("/")
  } catch (_err) {
    ElMessage.error("注册失败，请检查用户名是否已存在")
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
