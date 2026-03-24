<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>实验 CTR 报表</span>
        <el-space>
          <el-select v-model="selectedExperimentId" placeholder="选择实验" style="width: 260px" @change="reloadReport">
            <el-option
              v-for="item in experiments"
              :key="item.id"
              :label="`${item.name} (#${item.id})`"
              :value="item.id"
            />
          </el-select>
          <el-button @click="reloadReport">刷新</el-button>
        </el-space>
      </div>
    </template>

    <el-empty v-if="!selectedExperimentId" description="请先选择实验" />

    <template v-else>
      <p class="meta">主指标：{{ report?.metricPrimary || "-" }}</p>
      <el-table :data="report?.variants || []" v-loading="loading" border>
        <el-table-column prop="variantCode" label="变体" width="120" />
        <el-table-column prop="exposureUv" label="曝光UV" width="140" />
        <el-table-column prop="clickUv" label="点击UV" width="140" />
        <el-table-column label="CTR" width="140">
          <template #default="scope"> {{ (Number(scope.row.ctr || 0) * 100).toFixed(2) }}% </template>
        </el-table-column>
      </el-table>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import { ElMessage } from "element-plus"
import { getAbCtrReport, listAbExperiments, type AbCtrReportResponse, type AbExperiment } from "../../apis/ab"

const loading = ref(false)
const experiments = ref<AbExperiment[]>([])
const selectedExperimentId = ref<number | null>(null)
const report = ref<AbCtrReportResponse | null>(null)

onMounted(async () => {
  await loadExperiments()
})

async function loadExperiments() {
  loading.value = true
  try {
    experiments.value = await listAbExperiments()
    if (!selectedExperimentId.value && experiments.value.length > 0) {
      selectedExperimentId.value = experiments.value[0].id
      await reloadReport()
    }
  } catch (_err) {
    ElMessage.error("加载实验列表失败")
  } finally {
    loading.value = false
  }
}

async function reloadReport() {
  if (!selectedExperimentId.value) {
    return
  }

  loading.value = true
  try {
    report.value = await getAbCtrReport(selectedExperimentId.value)
  } catch (_err) {
    ElMessage.error("加载 CTR 报表失败")
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.meta {
  margin: 0 0 12px;
  color: #606266;
}
</style>
