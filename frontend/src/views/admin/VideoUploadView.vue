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
        <input ref="coverInput" type="file" accept="image/*" aria-label="选择封面图" @change="onCoverFileChange" />
        <div v-if="coverPreviewUrl" class="cover-preview-wrap">
          <img :src="coverPreviewUrl" alt="封面预览" class="cover-preview" />
        </div>
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
      <el-form-item label="封面URL">
        <el-input v-model="externalForm.coverUrl" placeholder="可选：封面图片地址" />
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
import { onUnmounted, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
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
const coverInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const selectedFile = ref<File | null>(null)
const selectedCoverFile = ref<File | null>(null)
const coverPreviewUrl = ref("")
const lastResult = ref<UploadCompleteResponse | null>(null)
const uploadProgress = ref(0)
const transcodeStatusText = ref("")
const canRetry = ref(false)
const lastInit = ref<{ videoId: string; objectKey: string; uploadUrl: string } | null>(null)
const externalSubmitting = ref(false)
const externalResult = ref<VideoDetail | null>(null)

const form = reactive({
  title: "",
  description: "",
})

const externalForm = reactive({
  title: "",
  description: "",
  sourceProtocol: "hls" as "hls" | "mp4",
  sourceUrl: "",
  coverUrl: "",
  durationSec: undefined as number | undefined,
})

const MAX_COVER_SIZE_BYTES = 5 * 1024 * 1024
const ALLOWED_COVER_MIME_TYPES = new Set(["image/jpeg", "image/jpg", "image/png", "image/webp"])

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] || null
}

function onCoverFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] || null

  if (file && !validateCoverFile(file)) {
    target.value = ""
    selectedCoverFile.value = null
    if (coverPreviewUrl.value) {
      URL.revokeObjectURL(coverPreviewUrl.value)
      coverPreviewUrl.value = ""
    }
    return
  }

  selectedCoverFile.value = file

  if (coverPreviewUrl.value) {
    URL.revokeObjectURL(coverPreviewUrl.value)
    coverPreviewUrl.value = ""
  }

  if (file) {
    coverPreviewUrl.value = URL.createObjectURL(file)
  }
}

function validateCoverFile(file: File): boolean {
  if (file.size > MAX_COVER_SIZE_BYTES) {
    ElMessage.warning("封面图片不能超过 5MB")
    return false
  }

  const type = (file.type || "").toLowerCase()
  if (type) {
    if (!ALLOWED_COVER_MIME_TYPES.has(type)) {
      ElMessage.warning("封面仅支持 jpg/jpeg、png、webp 格式")
      return false
    }
    return true
  }

  const lowerName = file.name.toLowerCase()
  if (
    !(
      lowerName.endsWith(".jpg") ||
      lowerName.endsWith(".jpeg") ||
      lowerName.endsWith(".png") ||
      lowerName.endsWith(".webp")
    )
  ) {
    ElMessage.warning("封面仅支持 jpg/jpeg、png、webp 格式")
    return false
  }
  return true
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
    const file = selectedFile.value
    const init = await uploadInit({
      title: form.title.trim(),
      description: form.description.trim(),
      fileName: file.name,
      mimeType: file.type || "video/mp4",
      fileSize: file.size,
    })

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
        uploadProgress.value = percent
      },
    })

    if (selectedCoverFile.value) {
      await uploadCoverImage(safeVideoId, selectedCoverFile.value)
    }

    const complete = await uploadComplete({
      videoId: safeVideoId,
      objectKey: init.objectKey,
      mimeType: file.type || "video/mp4",
      fileSize: file.size,
    })

    lastResult.value = complete
    ElMessage.success("上传成功，已进入转码队列")
    await pollVideoStatus(safeVideoId)
    form.title = ""
    form.description = ""
    if (fileInput.value) {
      fileInput.value.value = ""
    }
    if (coverInput.value) {
      coverInput.value.value = ""
    }
    selectedFile.value = null
    selectedCoverFile.value = null
    if (coverPreviewUrl.value) {
      URL.revokeObjectURL(coverPreviewUrl.value)
      coverPreviewUrl.value = ""
    }
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
  try {
    await uploadLocalFileWithProgress({
      videoId: lastInit.value.videoId,
      objectKey: lastInit.value.objectKey,
      uploadUrl: lastInit.value.uploadUrl,
      file: selectedFile.value,
      onProgress: (percent) => {
        uploadProgress.value = percent
      },
    })

    const complete = await uploadComplete({
      videoId: lastInit.value.videoId,
      objectKey: lastInit.value.objectKey,
      mimeType: selectedFile.value.type || "video/mp4",
      fileSize: selectedFile.value.size,
    })

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
  transcodeStatusText.value = "转码中"
  for (let i = 0; i < 20; i += 1) {
    await sleep(3000)
    try {
      const detail = await getAdminVideo(videoId)
      const status = detail.status || "unknown"
      transcodeStatusText.value = status

      if (status === "ready" || status === "published") {
        ElMessage.success("转码完成，可进入发布流程")
        return
      }

      if (status === "offline") {
        ElMessage.warning("转码可能失败，请检查任务日志")
        return
      }
    } catch (_err) {
      return
    }
  }
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
      coverUrl: externalForm.coverUrl.trim() || undefined,
      sourceProtocol: externalForm.sourceProtocol,
      sourceUrl: externalForm.sourceUrl.trim(),
      durationSec: externalForm.durationSec,
    })
    externalResult.value = created
    ElMessage.success("外链视频创建成功")

    externalForm.title = ""
    externalForm.description = ""
    externalForm.sourceUrl = ""
    externalForm.coverUrl = ""
    externalForm.durationSec = undefined
    externalForm.sourceProtocol = "hls"
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

onUnmounted(() => {
  if (coverPreviewUrl.value) {
    URL.revokeObjectURL(coverPreviewUrl.value)
  }
})
</script>

<style scoped>
.status-alert {
  margin-top: 12px;
}

.cover-preview-wrap {
  margin-top: 8px;
}

.cover-preview {
  width: 220px;
  max-width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.section-title {
  margin: 0;
  font-size: 16px;
}
</style>
