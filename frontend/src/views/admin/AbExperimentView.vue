<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>A/B 实验管理</span>
        <el-button type="primary" @click="openCreate">新建实验</el-button>
      </div>
    </template>

    <el-table :data="pagedRows" v-loading="loading" border>
      <el-table-column prop="name" label="实验名" min-width="160" />
      <el-table-column prop="scene" label="场景" width="120" />
      <el-table-column label="目标视频" min-width="220">
        <template #default="scope">
          <div class="video-target-cell">
            <span class="video-title">{{ resolveVideoTitle(scope.row.targetVideoId) }}</span>
            <span class="video-id">ID: {{ scope.row.targetVideoId }}</span>
          </div>
        </template>
      </el-table-column>
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
      <el-table-column label="操作" width="380" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="success" @click="onStart(scope.row.id)">启动</el-button>
            <el-button size="small" type="warning" @click="onStop(scope.row.id)">停止</el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="scope.row.status !== 'stopped'"
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
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑实验' : '新建实验'" width="680px">
    <el-form label-width="110px">
      <el-form-item label="实验名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="场景">
        <el-input v-model="form.scene" placeholder="例如 home_cover" />
      </el-form-item>
      <el-form-item label="目标视频" required>
        <el-select
          v-model="form.targetVideoId"
          filterable
          placeholder="请选择目标视频"
          style="width: 320px"
          :loading="videoOptionsLoading"
        >
          <el-option v-for="item in videoOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="主指标">
        <el-input v-model="form.metricPrimary" placeholder="ctr" />
      </el-form-item>

      <el-divider>变体配置</el-divider>
      <div v-for="(variant, index) in form.variants" :key="index" class="variant-row">
        <el-input v-model="variant.variantCode" placeholder="变体编码（A/B）" style="width: 120px" />
        <div class="variant-cover-editor">
          <ImageUploadSelector
            v-model="variant.coverFile"
            @update:modelValue="onVariantCoverFileChange(variant, $event)"
            :preview-url="variant.coverUrl"
            button-text="选择封面图"
            preview-alt="变体封面预览"
            :show-tip="false"
            :disabled="variant.coverUploading || submitting"
          />
          <div class="variant-cover-actions">
            <span class="upload-status" :class="`is-${variant.coverStatus}`">
              <i class="status-dot"></i>
              <span class="status-text">{{ resolveCoverStatusText(variant) }}</span>
            </span>
            <el-button
              size="small"
              text
              :disabled="(!variant.coverUrl && !variant.coverFile) || submitting || variant.coverUploading"
              @click="clearVariantCover(variant)"
            >
              清空
            </el-button>
          </div>
        </div>
        <el-input-number v-model="variant.trafficRatio" :min="1" :max="100" style="width: 120px" />
        <el-button text type="danger" @click="removeVariant(index)">删除</el-button>
      </div>
      <el-form-item>
        <el-button @click="addVariant">添加变体</el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import ImageUploadSelector from "../../components/image/ImageUploadSelector.vue"
import { getAdminVideos } from "../../apis/video"
import {
  createAbExperiment,
  deleteAbExperiment,
  listAbExperiments,
  startAbExperiment,
  stopAbExperiment,
  updateAbExperiment,
  uploadAbVariantCover,
  type AbExperiment,
} from "../../apis/ab"

interface AbVariantFormItem {
  variantCode: string
  coverUrl: string
  trafficRatio: number
  coverFile: File | null
  coverUploading: boolean
  coverStatus: "idle" | "uploading" | "success" | "error"
}

function buildVariantFormItem(variantCode: string, coverUrl: string, trafficRatio: number): AbVariantFormItem {
  return {
    variantCode,
    coverUrl,
    trafficRatio,
    coverFile: null,
    coverUploading: false,
    coverStatus: coverUrl ? "success" : "idle",
  }
}

function defaultVariants() {
  return [buildVariantFormItem("A", "", 50), buildVariantFormItem("B", "", 50)]
}

const loading = ref(false)
const rows = ref<AbExperiment[]>([])
const page = ref(1)
const pageSize = 10
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const videoOptionsLoading = ref(false)
const videoOptions = ref<Array<{ value: string; label: string }>>([])
const submitting = ref(false)

const total = computed(() => rows.value.length)
const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return rows.value.slice(start, start + pageSize)
})

const videoTitleMap = computed(() => {
  return new Map(videoOptions.value.map((item) => [item.value, item.label]))
})

const form = reactive({
  name: "",
  scene: "home_cover",
  targetVideoId: "",
  metricPrimary: "ctr",
  variants: defaultVariants() as AbVariantFormItem[],
})

onMounted(async () => {
  await Promise.all([reload(), loadVideoOptions()])
})

async function reload() {
  loading.value = true
  try {
    rows.value = await listAbExperiments()
    const maxPage = Math.max(1, Math.ceil(rows.value.length / pageSize))
    if (page.value > maxPage) {
      page.value = maxPage
    }
  } catch (_err) {
    ElMessage.error("加载实验列表失败")
  } finally {
    loading.value = false
  }
}

function onPageChange(nextPage: number) {
  page.value = nextPage
}

function resetForm() {
  form.name = ""
  form.scene = "home_cover"
  form.targetVideoId = videoOptions.value[0]?.value || ""
  form.metricPrimary = "ctr"
  form.variants = defaultVariants()
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
  form.targetVideoId = String(item.targetVideoId)
  form.metricPrimary = item.metricPrimary
  form.variants = item.variants.map((v) => buildVariantFormItem(v.variantCode, v.coverUrl || "", v.trafficRatio))
  dialogVisible.value = true
}

function addVariant() {
  form.variants.push(buildVariantFormItem(`V${form.variants.length + 1}`, "", 10))
}

function removeVariant(index: number) {
  form.variants.splice(index, 1)
}

async function submit() {
  if (submitting.value) {
    return
  }

  if (form.variants.some((item) => item.coverUploading)) {
    ElMessage.warning("封面上传中，请稍后再保存")
    return
  }

  if (!form.targetVideoId) {
    ElMessage.warning("请选择目标视频")
    return
  }

  if (!editingId.value && form.variants.some((item) => !item.coverUrl || !item.coverUrl.trim())) {
    ElMessage.warning("新建实验时每个变体都必须上传封面图")
    return
  }

  if (form.variants.some((item) => item.coverFile)) {
    ElMessage.warning("检测到待处理封面，请稍后重试")
    return
  }

  const ratioSum = form.variants.reduce((sum, item) => sum + Number(item.trafficRatio || 0), 0)
  if (ratioSum !== 100) {
    ElMessage.warning("变体流量占比总和必须为100")
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      scene: form.scene,
      targetVideoId: form.targetVideoId,
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
  } finally {
    submitting.value = false
  }
}

async function onVariantCoverFileChange(variant: AbVariantFormItem, file: File | null) {
  variant.coverFile = file
  if (!file) {
    variant.coverStatus = variant.coverUrl ? "success" : "idle"
    return
  }

  await uploadVariantCover(variant)
}

async function uploadVariantCover(variant: AbVariantFormItem) {
  if (!variant.coverFile || variant.coverUploading) {
    return
  }

  variant.coverUploading = true
  variant.coverStatus = "uploading"
  try {
    const result = await uploadAbVariantCover(variant.coverFile)
    variant.coverUrl = result.coverUrl
    variant.coverFile = null
    variant.coverStatus = "success"
    ElMessage.success("变体封面上传成功")
  } catch (err) {
    const message = err instanceof Error ? err.message : "变体封面上传失败"
    ElMessage.error(message)
    variant.coverFile = null
    variant.coverStatus = "error"
  } finally {
    variant.coverUploading = false
  }
}

function clearVariantCover(variant: AbVariantFormItem) {
  variant.coverFile = null
  variant.coverUrl = ""
  variant.coverStatus = "idle"
}

function resolveCoverStatusText(variant: AbVariantFormItem) {
  if (variant.coverStatus === "uploading") {
    return "上传中"
  }
  if (variant.coverStatus === "success") {
    return "上传成功"
  }
  if (variant.coverStatus === "error") {
    return "上传失败"
  }
  return "未上传"
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

async function onDelete(item: AbExperiment) {
  if (item.status !== "stopped") {
    ElMessage.warning("仅支持删除已停止实验")
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除实验「${item.name}」吗？将同步删除变体、分流记录与实验事件数据。`, "删除实验", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    })
  } catch (_err) {
    return
  }

  try {
    await deleteAbExperiment(item.id)
    ElMessage.success("实验及相关数据已删除")
    await reload()
  } catch (err) {
    const message = err instanceof Error ? err.message : "删除失败"
    ElMessage.error(message)
  }
}

async function loadVideoOptions() {
  videoOptionsLoading.value = true
  try {
    const data = await getAdminVideos(1, 1000, "", "")
    const mapped = (data.records || []).map((item) => ({
      value: String(item.id),
      label: item.title || `视频 ${item.id}`,
    }))

    const unique = new Map<string, { value: string; label: string }>()
    for (const item of mapped) {
      if (!unique.has(item.value)) {
        unique.set(item.value, item)
      }
    }

    videoOptions.value = Array.from(unique.values())
    if (!form.targetVideoId && videoOptions.value.length > 0) {
      form.targetVideoId = videoOptions.value[0].value
    }
  } catch (_err) {
    videoOptions.value = []
  } finally {
    videoOptionsLoading.value = false
  }
}

function resolveVideoTitle(videoId: number | string) {
  return videoTitleMap.value.get(String(videoId)) || "未知视频"
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
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.variant-cover-editor {
  width: 320px;
}

.variant-cover-actions {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  background: #c0c4cc;
}

.upload-status.is-uploading {
  color: #409eff;
}

.upload-status.is-uploading .status-dot {
  background: #409eff;
}

.upload-status.is-success {
  color: #67c23a;
}

.upload-status.is-success .status-dot {
  background: #67c23a;
}

.upload-status.is-error {
  color: #f56c6c;
}

.upload-status.is-error .status-dot {
  background: #f56c6c;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.video-target-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.video-title {
  color: #303133;
}

.video-id {
  font-size: 12px;
  color: #909399;
}
</style>
