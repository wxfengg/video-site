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
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import {
  getAdminVideo,
  uploadComplete,
  uploadInit,
  uploadLocalFileWithProgress,
  type UploadCompleteResponse,
} from "../../apis/video"

const fileInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const selectedFile = ref<File | null>(null)
const lastResult = ref<UploadCompleteResponse | null>(null)
const uploadProgress = ref(0)
const transcodeStatusText = ref("")
const canRetry = ref(false)
const lastInit = ref<{ videoId: string; objectKey: string; uploadUrl: string } | null>(null)

const form = reactive({
  title: "",
  description: "",
})

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
    selectedFile.value = null
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
</style>
