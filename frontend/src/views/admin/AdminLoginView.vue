<template>
  <el-card>
    <template #header>
      <span>管理员登录（Task 4）</span>
    </template>
    <el-form label-width="80px" @submit.prevent>
      <el-form-item label="账号">
        <el-input v-model="form.username" placeholder="admin" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="admin123" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onLogin">登录</el-button>
      </el-form-item>
    </el-form>
  </el-card>
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
