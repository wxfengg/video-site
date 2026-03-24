<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>A/B 实验管理</span>
        <el-button type="primary" @click="openCreate">新建实验</el-button>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="name" label="实验名" min-width="160" />
      <el-table-column prop="scene" label="场景" width="120" />
      <el-table-column prop="targetVideoId" label="目标视频ID" width="140" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="变体" min-width="280">
        <template #default="scope">
          <el-tag
            v-for="variant in scope.row.variants"
            :key="`${scope.row.id}_${variant.variantCode}`"
            style="margin-right: 8px"
          >
            {{ variant.variantCode }} / {{ variant.trafficRatio }}%
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="success" @click="onStart(scope.row.id)">启动</el-button>
            <el-button size="small" type="warning" @click="onStop(scope.row.id)">停止</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑实验' : '新建实验'" width="680px">
    <el-form label-width="110px">
      <el-form-item label="实验名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="场景">
        <el-input v-model="form.scene" placeholder="例如 home_cover" />
      </el-form-item>
      <el-form-item label="目标视频ID">
        <el-input-number v-model="form.targetVideoId" :min="1" />
      </el-form-item>
      <el-form-item label="主指标">
        <el-input v-model="form.metricPrimary" placeholder="ctr" />
      </el-form-item>

      <el-divider>变体配置</el-divider>
      <div v-for="(variant, index) in form.variants" :key="index" class="variant-row">
        <el-input v-model="variant.variantCode" placeholder="变体编码（A/B）" style="width: 120px" />
        <el-input v-model="variant.coverUrl" placeholder="封面 URL（可选）" style="width: 260px" />
        <el-input-number v-model="variant.trafficRatio" :min="1" :max="100" style="width: 120px" />
        <el-button text type="danger" @click="removeVariant(index)">删除</el-button>
      </div>
      <el-form-item>
        <el-button @click="addVariant">添加变体</el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import {
  createAbExperiment,
  listAbExperiments,
  startAbExperiment,
  stopAbExperiment,
  updateAbExperiment,
  type AbExperiment,
} from "../../apis/ab"

const loading = ref(false)
const rows = ref<AbExperiment[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: "",
  scene: "home_cover",
  targetVideoId: 1,
  metricPrimary: "ctr",
  variants: [
    { variantCode: "A", coverUrl: "", trafficRatio: 50 },
    { variantCode: "B", coverUrl: "", trafficRatio: 50 },
  ],
})

onMounted(async () => {
  await reload()
})

async function reload() {
  loading.value = true
  try {
    rows.value = await listAbExperiments()
  } catch (_err) {
    ElMessage.error("加载实验列表失败")
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ""
  form.scene = "home_cover"
  form.targetVideoId = 1
  form.metricPrimary = "ctr"
  form.variants = [
    { variantCode: "A", coverUrl: "", trafficRatio: 50 },
    { variantCode: "B", coverUrl: "", trafficRatio: 50 },
  ]
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: AbExperiment) {
  editingId.value = item.id
  form.name = item.name
  form.scene = item.scene
  form.targetVideoId = item.targetVideoId
  form.metricPrimary = item.metricPrimary
  form.variants = item.variants.map((v) => ({
    variantCode: v.variantCode,
    coverUrl: v.coverUrl || "",
    trafficRatio: v.trafficRatio,
  }))
  dialogVisible.value = true
}

function addVariant() {
  form.variants.push({
    variantCode: `V${form.variants.length + 1}`,
    coverUrl: "",
    trafficRatio: 10,
  })
}

function removeVariant(index: number) {
  form.variants.splice(index, 1)
}

async function submit() {
  const ratioSum = form.variants.reduce((sum, item) => sum + Number(item.trafficRatio || 0), 0)
  if (ratioSum !== 100) {
    ElMessage.warning("变体流量占比总和必须为100")
    return
  }

  try {
    const payload = {
      name: form.name,
      scene: form.scene,
      targetVideoId: Number(form.targetVideoId),
      metricPrimary: form.metricPrimary,
      variants: form.variants.map((item) => ({
        variantCode: item.variantCode,
        coverUrl: item.coverUrl,
        trafficRatio: Number(item.trafficRatio),
      })),
    }

    if (editingId.value) {
      await updateAbExperiment(editingId.value, payload)
      ElMessage.success("实验已更新")
    } else {
      await createAbExperiment(payload)
      ElMessage.success("实验已创建")
    }

    dialogVisible.value = false
    await reload()
  } catch (_err) {
    ElMessage.error("保存实验失败")
  }
}

async function onStart(experimentId: number) {
  try {
    await startAbExperiment(experimentId)
    ElMessage.success("实验已启动")
    await reload()
  } catch (_err) {
    ElMessage.error("启动失败")
  }
}

async function onStop(experimentId: number) {
  try {
    await stopAbExperiment(experimentId)
    ElMessage.success("实验已停止")
    await reload()
  } catch (_err) {
    ElMessage.error("停止失败")
  }
}
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.variant-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
</style>
