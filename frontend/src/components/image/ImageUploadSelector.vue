<template>
  <div class="image-upload-selector">
    <div class="actions">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :show-file-list="false"
        :accept="IMAGE_ACCEPT_ATTR"
        :on-change="onChange"
        :limit="1"
        :disabled="disabled"
      >
        <el-button size="small" :disabled="disabled">{{ buttonText }}</el-button>
      </el-upload>

      <el-button v-if="modelValue" size="small" text @click="clearSelection">清空</el-button>
    </div>

    <div v-if="showTip" class="tip">{{ tipText }}</div>

    <img v-if="displayPreviewUrl" :src="displayPreviewUrl" :alt="previewAlt" class="preview" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue"
import { ElMessage } from "element-plus"
import type { UploadFile, UploadFiles, UploadInstance } from "element-plus"
import { IMAGE_ACCEPT_ATTR, IMAGE_UPLOAD_TIP_TEXT, validateImageFile } from "../../utils/image-upload"

interface Props {
  modelValue: File | null
  previewUrl?: string | null
  buttonText?: string
  previewAlt?: string
  tipText?: string
  showTip?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  previewUrl: "",
  buttonText: "选择图片",
  previewAlt: "图片预览",
  tipText: IMAGE_UPLOAD_TIP_TEXT,
  showTip: true,
  disabled: false,
})

const emit = defineEmits<{
  "update:modelValue": [file: File | null]
}>()

const uploadRef = ref<UploadInstance | null>(null)
const localPreviewUrl = ref("")

const displayPreviewUrl = computed(() => {
  return localPreviewUrl.value || props.previewUrl || ""
})

watch(
  () => props.modelValue,
  (file) => {
    if (!file) {
      revokeLocalPreview()
      clearUploadFiles()
      return
    }

    updatePreview(file)
  },
  { immediate: true },
)

function onChange(uploadFile: UploadFile, _uploadFiles: UploadFiles) {
  const raw = uploadFile.raw
  if (!raw) {
    return
  }

  const errorMessage = validateImageFile(raw)
  if (errorMessage) {
    ElMessage.warning(errorMessage)
    emit("update:modelValue", null)
    clearUploadFiles()
    return
  }

  emit("update:modelValue", raw)
  updatePreview(raw)
  clearUploadFiles()
}

function clearSelection() {
  emit("update:modelValue", null)
}

function updatePreview(file: File) {
  revokeLocalPreview()
  localPreviewUrl.value = URL.createObjectURL(file)
}

function clearUploadFiles() {
  uploadRef.value?.clearFiles()
}

function revokeLocalPreview() {
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
    localPreviewUrl.value = ""
  }
}

onBeforeUnmount(() => {
  revokeLocalPreview()
})
</script>

<style scoped>
.image-upload-selector {
  width: 100%;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.preview {
  margin-top: 10px;
  width: 220px;
  max-width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
</style>
