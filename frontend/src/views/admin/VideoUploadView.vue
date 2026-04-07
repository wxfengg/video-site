<template>
  <el-card>
    <template #header>
      <span>视频上传</span>
    </template>

    <el-form label-width="100px" @submit.prevent>
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="255" name="title" aria-label="视频标题" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          maxlength="2000"
          name="description"
          aria-label="视频简介"
        />
      </el-form-item>
      <el-form-item label="文件" required>
        <input ref="fileInput" type="file" accept="video/*" aria-label="选择视频文件" @change="onFileChange" />
      </el-form-item>
      <el-form-item label="封面图">
        <ImageUploadSelector v-model="selectedCoverFile" button-text="选择封面图" preview-alt="封面预览" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="onSubmit">上传并入库</el-button>
        <el-button v-if="canRetry" @click="retryUpload">重试上次上传</el-button>
      </el-form-item>
    </el-form>

    <el-progress v-if="submitting || uploadProgress > 0" :percentage="uploadProgress" :stroke-width="12" />

    <el-alert v-if="transcodeStatusText" type="info" show-icon :closable="false" class="status-alert">
      <template #title> 转码状态：{{ transcodeStatusText }} </template>
    </el-alert>

    <el-alert v-if="lastResult" type="success" show-icon :closable="false">
      <template #title>
        上传成功：videoId={{ lastResult.videoId }}, transcodeTaskId={{ lastResult.transcodeTaskId }}
      </template>
    </el-alert>

    <el-divider />

    <el-form label-width="120px" @submit.prevent>
      <el-form-item>
        <h3 class="section-title">外链视频录入（直链 MP4/HLS）</h3>
      </el-form-item>
      <el-form-item label="标题" required>
        <el-input v-model="externalForm.title" maxlength="255" placeholder="请输入外链视频标题" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="externalForm.description" type="textarea" :rows="3" maxlength="2000" />
      </el-form-item>
      <el-form-item label="协议" required>
        <el-select v-model="externalForm.sourceProtocol" style="width: 180px">
          <el-option label="HLS(m3u8)" value="hls" />
          <el-option label="MP4" value="mp4" />
        </el-select>
      </el-form-item>
      <el-form-item label="外链地址" required>
        <el-input v-model="externalForm.sourceUrl" placeholder="https://example.com/master.m3u8" />
      </el-form-item>
      <el-form-item label="封面图">
        <ImageUploadSelector
          v-model="selectedExternalCoverFile"
          button-text="选择外链封面"
          preview-alt="外链封面预览"
          :disabled="externalSubmitting"
        />
      </el-form-item>
      <el-form-item label="时长(秒)">
        <el-input-number v-model="externalForm.durationSec" :min="1" :max="86400" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="externalSubmitting" @click="onSubmitExternal">录入外链视频</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="externalResult" type="success" show-icon :closable="false">
      <template #title>
        外链视频已创建：videoId={{ externalResult.id }}，当前状态={{ externalResult.status }}
      </template>
    </el-alert>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import ImageUploadSelector from "../../components/image/ImageUploadSelector.vue"
import {
  createExternalVideo,
  getAdminVideo,
  uploadComplete,
  uploadCoverImage,
  uploadInit,
  uploadLocalFileWithProgress,
  type VideoDetail,
  type UploadCompleteResponse,
} from "../../apis/video"

const fileInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const selectedFile = ref<File | null>(null)
const selectedCoverFile = ref<File | null>(null)
const lastResult = ref<UploadCompleteResponse | null>(null)
const uploadProgress = ref(0)
const transcodeStatusText = ref("")
const canRetry = ref(false)
const lastInit = ref<{ videoId: string; objectKey: string; uploadUrl: string } | null>(null)
const externalSubmitting = ref(false)
const externalResult = ref<VideoDetail | null>(null)
const selectedExternalCoverFile = ref<File | null>(null)

const form = reactive({
  title: "",
  description: "",
})

const externalForm = reactive({
  title: "",
  description: "",
  sourceProtocol: "hls" as "hls" | "mp4",
  sourceUrl: "",
  durationSec: undefined as number | undefined,
})

const FLOW_PROGRESS_PREPARE = 5
const FLOW_PROGRESS_UPLOAD_MAX = 78
const FLOW_PROGRESS_TRANSCODE_START = 82
const FLOW_PROGRESS_TRANSCODE_MAX = 98
const FLOW_PROGRESS_POLL_TIMES = 80

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] || null
}

function resolveUploadVideoId(videoId: number | string, uploadUrl: string): string {
  const matched = uploadUrl.match(/\/api\/videos\/upload\/local\/([^?]+)/)
  if (matched?.[1]) {
    return matched[1]
  }
  return String(videoId)
}

async function onSubmit() {
  if (!form.title.trim()) {
    ElMessage.warning("请填写标题")
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning("请选择要上传的视频文件")
    return
  }

  submitting.value = true
  uploadProgress.value = 0
  transcodeStatusText.value = ""
  canRetry.value = false
  try {
    setFlowProgress(1)
    const file = selectedFile.value
    const init = await uploadInit({
      title: form.title.trim(),
      description: form.description.trim(),
      fileName: file.name,
      mimeType: file.type || "video/mp4",
      fileSize: file.size,
    })

    setFlowProgress(FLOW_PROGRESS_PREPARE)

    const safeVideoId = resolveUploadVideoId(init.videoId, init.uploadUrl)

    lastInit.value = {
      videoId: safeVideoId,
      objectKey: init.objectKey,
      uploadUrl: init.uploadUrl,
    }

    await uploadLocalFileWithProgress({
      videoId: safeVideoId,
      objectKey: init.objectKey,
      uploadUrl: init.uploadUrl,
      file,
      onProgress: (percent) => {
        setFlowProgress(mapUploadProgress(percent))
      },
    })

    setFlowProgress(FLOW_PROGRESS_UPLOAD_MAX)

    if (selectedCoverFile.value) {
      await uploadCoverImage(safeVideoId, selectedCoverFile.value)
    }

    const complete = await uploadComplete({
      videoId: safeVideoId,
      objectKey: init.objectKey,
      mimeType: file.type || "video/mp4",
      fileSize: file.size,
    })

    setFlowProgress(FLOW_PROGRESS_TRANSCODE_START)

    lastResult.value = complete
    ElMessage.success("上传成功，已进入转码队列")
    await pollVideoStatus(safeVideoId)
    form.title = ""
    form.description = ""
    if (fileInput.value) {
      fileInput.value.value = ""
    }
    selectedFile.value = null
    selectedCoverFile.value = null
  } catch (err) {
    canRetry.value = true
    const message = err instanceof Error ? err.message : "上传失败，请检查后端服务与文件大小"
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

async function retryUpload() {
  if (!selectedFile.value || !lastInit.value) {
    ElMessage.warning("没有可重试的上传任务，请重新选择文件")
    return
  }

  submitting.value = true
  canRetry.value = false
  uploadProgress.value = 0
  transcodeStatusText.value = ""
  try {
    setFlowProgress(FLOW_PROGRESS_PREPARE)
    await uploadLocalFileWithProgress({
      videoId: lastInit.value.videoId,
      objectKey: lastInit.value.objectKey,
      uploadUrl: lastInit.value.uploadUrl,
      file: selectedFile.value,
      onProgress: (percent) => {
        setFlowProgress(mapUploadProgress(percent))
      },
    })

    setFlowProgress(FLOW_PROGRESS_UPLOAD_MAX)

    const complete = await uploadComplete({
      videoId: lastInit.value.videoId,
      objectKey: lastInit.value.objectKey,
      mimeType: selectedFile.value.type || "video/mp4",
      fileSize: selectedFile.value.size,
    })

    setFlowProgress(FLOW_PROGRESS_TRANSCODE_START)

    lastResult.value = complete
    ElMessage.success("重试上传成功，已进入转码队列")
    await pollVideoStatus(lastInit.value.videoId)
  } catch (_err) {
    canRetry.value = true
    ElMessage.error("重试上传失败")
  } finally {
    submitting.value = false
  }
}

async function pollVideoStatus(videoId: number | string) {
  transcodeStatusText.value = "排队转码中"
  setFlowProgress(FLOW_PROGRESS_TRANSCODE_START)

  for (let i = 0; i < FLOW_PROGRESS_POLL_TIMES; i += 1) {
    await sleep(3000)
    try {
      const detail = await getAdminVideo(videoId)
      const status = detail.status || "unknown"
      transcodeStatusText.value = mapStatusText(status)

      if (status === "transcoding" || status === "draft") {
        const step =
          FLOW_PROGRESS_TRANSCODE_START +
          ((i + 1) / FLOW_PROGRESS_POLL_TIMES) * (FLOW_PROGRESS_TRANSCODE_MAX - FLOW_PROGRESS_TRANSCODE_START)
        setFlowProgress(step)
      }

      if (status === "ready" || status === "published") {
        setFlowProgress(100)
        ElMessage.success("转码完成，可进入发布流程")
        return
      }

      if (status === "offline") {
        setFlowProgress(FLOW_PROGRESS_TRANSCODE_START)
        ElMessage.warning("转码可能失败，请检查任务日志")
        return
      }
    } catch (_err) {
      transcodeStatusText.value = "状态查询失败，请稍后重试"
      return
    }
  }

  transcodeStatusText.value = "转码中（耗时较长，可在视频管理页查看）"
}

function setFlowProgress(next: number) {
  const safe = Math.max(0, Math.min(100, Math.round(next)))
  uploadProgress.value = Math.max(uploadProgress.value, safe)
}

function mapUploadProgress(percent: number) {
  const safePercent = Math.max(0, Math.min(100, percent))
  const span = FLOW_PROGRESS_UPLOAD_MAX - FLOW_PROGRESS_PREPARE
  return FLOW_PROGRESS_PREPARE + (safePercent / 100) * span
}

function mapStatusText(status: string) {
  const dict: Record<string, string> = {
    draft: "排队中",
    transcoding: "转码中",
    ready: "转码完成（可播放）",
    published: "已发布（可播放）",
    offline: "转码失败或已下线",
  }
  return dict[status] || status
}

async function onSubmitExternal() {
  if (!externalForm.title.trim()) {
    ElMessage.warning("请填写外链视频标题")
    return
  }
  if (!externalForm.sourceUrl.trim()) {
    ElMessage.warning("请填写外链地址")
    return
  }

  externalSubmitting.value = true
  try {
    const created = await createExternalVideo({
      title: externalForm.title.trim(),
      description: externalForm.description.trim() || undefined,
      sourceProtocol: externalForm.sourceProtocol,
      sourceUrl: externalForm.sourceUrl.trim(),
      durationSec: externalForm.durationSec,
    })

    let finalDetail = created
    if (selectedExternalCoverFile.value) {
      try {
        await uploadCoverImage(created.id, selectedExternalCoverFile.value)
        finalDetail = await getAdminVideo(created.id)
        ElMessage.success("外链视频创建成功，封面上传成功")
      } catch (err) {
        const message = err instanceof Error ? err.message : "封面上传失败"
        ElMessage.warning(`外链视频已创建，但${message}`)
      }
    } else {
      ElMessage.success("外链视频创建成功")
    }

    externalResult.value = finalDetail

    externalForm.title = ""
    externalForm.description = ""
    externalForm.sourceUrl = ""
    externalForm.durationSec = undefined
    externalForm.sourceProtocol = "hls"
    selectedExternalCoverFile.value = null
  } catch (_err) {
    ElMessage.error("外链视频创建失败，请检查直链格式")
  } finally {
    externalSubmitting.value = false
  }
}

function sleep(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}
</script>

<style scoped>
.status-alert {
  margin-top: 12px;
}

.section-title {
  margin: 0;
  font-size: 16px;
}
</style>
