<template>
  <section class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card>
          <h3>视频上传</h3>
          <p>上传原始视频并进入转码队列。</p>
          <el-button type="primary" @click="go('/admin/upload')">进入</el-button>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <h3>视频管理</h3>
          <p>编辑、发布、下线与播放预览。</p>
          <el-button @click="go('/admin/videos')">进入</el-button>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <h3>A/B 实验</h3>
          <p>实验配置、变体流量与启停。</p>
          <el-button @click="go('/admin/ab-experiments')">进入</el-button>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <h3>CTR 报表</h3>
          <p>按变体查看曝光、点击与 CTR。</p>
          <el-button @click="go('/admin/ab-reports')">进入</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="tips">
      <template #header>联调建议流程</template>
      <ol>
        <li>上传视频并等待状态从 <code>transcoding</code> 变为 <code>ready</code></li>
        <li>在视频管理页发布后，前台首页可见并可播放</li>
        <li>配置并启动 A/B 实验，在首页触发曝光/点击行为</li>
        <li>到 CTR 报表页查看变体对比结果</li>
      </ol>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"

const router = useRouter()

function go(path: string) {
  void router.push(path)
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

h3 {
  margin: 0 0 8px;
}

p {
  margin: 0 0 10px;
  color: #606266;
  min-height: 42px;
}

.tips ol {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}
</style>
