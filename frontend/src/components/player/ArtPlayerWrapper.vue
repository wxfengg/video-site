<template>
  <div class="player-wrap">
    <video
      ref="videoRef"
      class="video"
      controls
      preload="metadata"
      :src="activeUrl || undefined"
      @play="emit('play')"
      @timeupdate="onTimeUpdate"
      @ended="emit('ended')"
    />
    <el-empty v-if="!activeUrl" description="播放源尚未就绪" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"

interface Props {
  hlsMasterUrl?: string | null
  mp4360Url?: string | null
  mp4720Url?: string | null
  mp41080Url?: string | null
  quality?: string
}

const props = withDefaults(defineProps<Props>(), {
  hlsMasterUrl: null,
  mp4360Url: null,
  mp4720Url: null,
  mp41080Url: null,
  quality: "auto",
})

const emit = defineEmits<{
  play: []
  ended: []
  progress: [seconds: number]
}>()

const videoRef = ref<HTMLVideoElement | null>(null)

const activeUrl = computed(() => {
  if (props.quality === "1080") {
    return props.mp41080Url || props.mp4720Url || props.mp4360Url || props.hlsMasterUrl || null
  }
  if (props.quality === "720") {
    return props.mp4720Url || props.mp4360Url || props.mp41080Url || props.hlsMasterUrl || null
  }
  if (props.quality === "360") {
    return props.mp4360Url || props.mp4720Url || props.mp41080Url || props.hlsMasterUrl || null
  }

  return props.hlsMasterUrl || props.mp41080Url || props.mp4720Url || props.mp4360Url || null
})

watch(activeUrl, () => {
  if (!videoRef.value) {
    return
  }
  videoRef.value.load()
})

function onTimeUpdate() {
  if (!videoRef.value) {
    return
  }
  emit("progress", Math.floor(videoRef.value.currentTime || 0))
}
</script>

<style scoped>
.player-wrap {
  width: 100%;
}

.video {
  width: 100%;
  border-radius: 10px;
  background: #000;
  aspect-ratio: 16 / 9;
}
</style>
