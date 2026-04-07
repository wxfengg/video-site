<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>用户管理</span>
        <el-space>
          <el-input
            v-model="keyword"
            placeholder="搜索用户名…"
            clearable
            style="width: 220px"
            name="adminUserKeyword"
            aria-label="管理端用户搜索"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
          <el-button @click="onSearch">搜索</el-button>
          <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
        </el-space>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="用户ID" width="200" />
      <el-table-column prop="username" label="用户名" min-width="160" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? "启用" : "禁用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="180">
        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="最后登录" width="180">
        <template #default="scope">{{ formatDateTime(scope.row.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button
              size="small"
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              :loading="statusUpdatingKey === String(scope.row.id)"
              @click="onToggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? "禁用" : "启用" }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="statusUpdatingKey === String(scope.row.id)"
              @click="onDelete(scope.row)"
            >
              删除
            </el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        :page-sizes="[10, 20, 50]"
        @size-change="onPageSizeChange"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="createDialogVisible" title="新增普通用户" width="520px">
    <el-form label-width="100px">
      <el-form-item label="用户名">
        <el-input v-model="createForm.username" maxlength="64" name="newUsername" aria-label="新增用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="createForm.password"
          type="password"
          maxlength="64"
          show-password
          name="newPassword"
          aria-label="新增用户密码"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="createForm.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="createSubmitting" @click="submitCreate">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import {
  createAdminUser,
  deleteAdminUser,
  listAdminUsers,
  updateAdminUserStatus,
  type AdminUserListItem,
} from "../../apis/admin-user"

const loading = ref(false)
const rows = ref<AdminUserListItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref("")
const statusUpdatingKey = ref<string | null>(null)

const createDialogVisible = ref(false)
const createSubmitting = ref(false)
const createForm = reactive({
  username: "",
  password: "",
  status: 1 as 0 | 1,
})

const USERNAME_PATTERN = /^[a-zA-Z0-9_]+$/

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    const data = await listAdminUsers(page.value, pageSize.value, keyword.value)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } catch (_err) {
    ElMessage.error("加载用户列表失败")
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  page.value = 1
  await reload()
}

async function onPageChange(nextPage: number) {
  page.value = nextPage
  await reload()
}

async function onPageSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
  await reload()
}

function openCreateDialog() {
  createForm.username = ""
  createForm.password = ""
  createForm.status = 1
  createDialogVisible.value = true
}

async function submitCreate() {
  const username = createForm.username.trim()
  const password = createForm.password

  if (username.length < 3 || username.length > 64) {
    ElMessage.warning("用户名长度需在 3 到 64 之间")
    return
  }

  if (!USERNAME_PATTERN.test(username)) {
    ElMessage.warning("用户名仅支持字母、数字和下划线")
    return
  }

  if (password.length < 6 || password.length > 64) {
    ElMessage.warning("密码长度需在 6 到 64 之间")
    return
  }

  createSubmitting.value = true
  try {
    await createAdminUser({
      username,
      password,
      status: createForm.status,
    })
    ElMessage.success("用户创建成功")
    createDialogVisible.value = false
    page.value = 1
    await reload()
  } catch (err) {
    const message = err instanceof Error ? err.message : "创建用户失败"
    ElMessage.error(message)
  } finally {
    createSubmitting.value = false
  }
}

async function onDelete(row: AdminUserListItem) {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？此操作不可恢复。`, "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    })
  } catch (_err) {
    return
  }

  try {
    await deleteAdminUser(row.id)
    ElMessage.success("删除成功")

    const maxPage = Math.max(1, Math.ceil(Math.max(total.value - 1, 0) / pageSize.value))
    if (page.value > maxPage) {
      page.value = maxPage
    }

    await reload()
  } catch (err) {
    const message = err instanceof Error ? err.message : "删除失败"
    ElMessage.error(message)
  }
}

async function onToggleStatus(row: AdminUserListItem) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? "启用" : "禁用"

  try {
    await ElMessageBox.confirm(`确认${actionText}用户 ${row.username} 吗？`, `${actionText}确认`, {
      type: "warning",
      confirmButtonText: actionText,
      cancelButtonText: "取消",
    })
  } catch (_err) {
    return
  }

  statusUpdatingKey.value = String(row.id)
  try {
    await updateAdminUserStatus(row.id, { status: nextStatus as 0 | 1 })
    ElMessage.success(`用户已${actionText}`)
    await reload()
  } catch (err) {
    const message = err instanceof Error ? err.message : `${actionText}失败`
    ElMessage.error(message)
  } finally {
    statusUpdatingKey.value = null
  }
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "--"
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace("T", " ").slice(0, 19)
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  const hour = String(date.getHours()).padStart(2, "0")
  const minute = String(date.getMinutes()).padStart(2, "0")
  const second = String(date.getSeconds()).padStart(2, "0")
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
