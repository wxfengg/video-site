<template>
  <section class="dashboard" v-loading="loading">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          unlink-panels
        />
        <el-input-number v-model="funnelVideoId" :min="1" placeholder="漏斗视频ID(可选)" />
        <el-button type="primary" @click="reloadAll">刷新看板</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <span class="metric-label">播放 PV</span>
          <strong>{{ formatNumber(overview?.playPv) }}</strong>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <span class="metric-label">DAU 峰值</span>
          <strong>{{ formatNumber(overview?.peakDau) }}</strong>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <span class="metric-label">新增用户</span>
          <strong>{{ formatNumber(overview?.newUsers) }}</strong>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <span class="metric-label">已发布视频</span>
          <strong>{{ formatNumber(overview?.publishedVideos) }}</strong>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <span class="metric-label">运行实验</span>
          <strong>{{ formatNumber(overview?.runningExperiments) }}</strong>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card">
          <span class="metric-label">评论总量</span>
          <strong>{{ formatNumber(overview?.totalComments) }}</strong>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>流量趋势</template>
          <div ref="trafficChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>用户增长</template>
          <div ref="growthChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="header-row">
              <span>播放漏斗</span>
              <span class="meta"
                >CTR {{ toPercent(playFunnel?.ctr) }} · 完播率 {{ toPercent(playFunnel?.completionRate) }}</span
              >
            </div>
          </template>
          <div ref="funnelChartRef" class="chart" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12" class="stack-col">
        <el-card shadow="never">
          <template #header>热门视频 TopN</template>
          <el-table :data="overview?.hotVideos || []" size="small" border>
            <el-table-column prop="rankIndex" label="#" width="70" />
            <el-table-column prop="title" label="视频" min-width="180" />
            <el-table-column label="热度" width="110">
              <template #default="scope">{{ Number(scope.row.hotScore || 0).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header>A/B 结果摘要</template>
          <el-table :data="overview?.abSummary || []" size="small" border>
            <el-table-column prop="experimentName" label="实验" min-width="120" />
            <el-table-column prop="variantCode" label="变体" width="80" />
            <el-table-column prop="exposureUv" label="曝光UV" width="100" />
            <el-table-column prop="clickUv" label="点击UV" width="100" />
            <el-table-column label="CTR" width="90">
              <template #default="scope">{{ toPercent(scope.row.ctr) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import * as echarts from "echarts"
import { ElMessage } from "element-plus"
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue"
import {
  getDashboardOverview,
  getDashboardPlayFunnel,
  getDashboardTrafficTrend,
  getDashboardUserGrowth,
  type DashboardOverviewResponse,
  type DashboardPlayFunnelResponse,
  type DashboardTrafficTrendResponse,
  type DashboardUserGrowthResponse,
} from "../../apis/dashboard"

const loading = ref(false)
const dateRange = ref<[Date, Date] | null>(defaultDateRange())
const funnelVideoId = ref<number | null>(null)

const overview = ref<DashboardOverviewResponse | null>(null)
const trafficTrend = ref<DashboardTrafficTrendResponse | null>(null)
const userGrowth = ref<DashboardUserGrowthResponse | null>(null)
const playFunnel = ref<DashboardPlayFunnelResponse | null>(null)

const trafficChartRef = ref<HTMLDivElement | null>(null)
const growthChartRef = ref<HTMLDivElement | null>(null)
const funnelChartRef = ref<HTMLDivElement | null>(null)

let trafficChart: echarts.ECharts | null = null
let growthChart: echarts.ECharts | null = null
let funnelChart: echarts.ECharts | null = null

onMounted(async () => {
  await nextTick()
  initCharts()
  window.addEventListener("resize", onResize)
  await reloadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", onResize)
  disposeCharts()
})

async function reloadAll() {
  loading.value = true
  try {
    const range = buildRangeParams()
    const overviewData = await getDashboardOverview()
    overview.value = overviewData

    if (funnelVideoId.value === null && overviewData.hotVideos.length > 0) {
      const hotVideoId = Number(overviewData.hotVideos[0].videoId)
      if (!Number.isNaN(hotVideoId) && hotVideoId > 0) {
        funnelVideoId.value = hotVideoId
      }
    }

    const [traffic, growth, funnel] = await Promise.all([
      getDashboardTrafficTrend(range),
      getDashboardUserGrowth(range),
      getDashboardPlayFunnel({
        ...range,
        videoId: funnelVideoId.value || undefined,
      }),
    ])

    trafficTrend.value = traffic
    userGrowth.value = growth
    playFunnel.value = funnel

    renderCharts()
  } catch (_err) {
    ElMessage.error("加载 Dashboard 失败，请稍后重试")
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  renderTrafficChart()
  renderGrowthChart()
  renderFunnelChart()
}

function initCharts() {
  if (trafficChartRef.value && !trafficChart) {
    trafficChart = echarts.init(trafficChartRef.value)
  }
  if (growthChartRef.value && !growthChart) {
    growthChart = echarts.init(growthChartRef.value)
  }
  if (funnelChartRef.value && !funnelChart) {
    funnelChart = echarts.init(funnelChartRef.value)
  }
}

function disposeCharts() {
  trafficChart?.dispose()
  growthChart?.dispose()
  funnelChart?.dispose()
  trafficChart = null
  growthChart = null
  funnelChart = null
}

function onResize() {
  trafficChart?.resize()
  growthChart?.resize()
  funnelChart?.resize()
}

function renderTrafficChart() {
  if (!trafficChart || !trafficTrend.value) {
    return
  }

  const points = trafficTrend.value.points || []
  const labels = points.map((item) => formatBucketTime(item.bucketTime))
  const playPvData = points.map((item) => Number(item.playPv || 0))
  const dauData = points.map((item) => Number(item.dau || 0))

  trafficChart.setOption(
    {
      tooltip: { trigger: "axis" },
      legend: { data: ["播放PV", "DAU"] },
      grid: { left: 40, right: 16, top: 32, bottom: 34 },
      xAxis: { type: "category", data: labels },
      yAxis: { type: "value" },
      series: [
        { name: "播放PV", type: "line", smooth: true, data: playPvData },
        { name: "DAU", type: "line", smooth: true, data: dauData },
      ],
    },
    true,
  )
}

function renderGrowthChart() {
  if (!growthChart || !userGrowth.value) {
    return
  }

  const points = userGrowth.value.points || []
  const labels = points.map((item) => item.day)
  const newUsers = points.map((item) => Number(item.newUsers || 0))
  const cumulative = points.map((item) => Number(item.cumulativeUsers || 0))

  growthChart.setOption(
    {
      tooltip: { trigger: "axis" },
      legend: { data: ["新增用户", "累计用户"] },
      grid: { left: 40, right: 16, top: 32, bottom: 34 },
      xAxis: { type: "category", data: labels },
      yAxis: [{ type: "value" }, { type: "value" }],
      series: [
        { name: "新增用户", type: "bar", data: newUsers },
        { name: "累计用户", type: "line", yAxisIndex: 1, smooth: true, data: cumulative },
      ],
    },
    true,
  )
}

function renderFunnelChart() {
  if (!funnelChart || !playFunnel.value) {
    return
  }

  const stages = playFunnel.value.stages || []
  const labels = stages.map((item) => item.stage)
  const values = stages.map((item) => Number(item.uv || 0))

  funnelChart.setOption(
    {
      tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
      grid: { left: 70, right: 30, top: 16, bottom: 28 },
      xAxis: { type: "value" },
      yAxis: { type: "category", data: labels },
      series: [
        {
          name: "UV",
          type: "bar",
          data: values,
          label: { show: true, position: "right" },
          itemStyle: { color: "#5b7cfa" },
        },
      ],
    },
    true,
  )
}

function defaultDateRange(): [Date, Date] {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - 6)
  return [start, end]
}

function buildRangeParams() {
  if (!dateRange.value) {
    return {}
  }

  const [from, to] = dateRange.value
  return {
    from: formatDate(from),
    to: formatDate(to),
  }
}

function formatDate(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, "0")
  const day = String(value.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function formatBucketTime(value: string | null | undefined) {
  if (!value) {
    return "--"
  }
  return value.replace("T", " ").slice(5, 16)
}

function formatNumber(value: number | null | undefined) {
  return new Intl.NumberFormat("zh-CN").format(Number(value || 0))
}

function toPercent(value: number | null | undefined) {
  const safe = Number(value || 0)
  return `${(safe * 100).toFixed(2)}%`
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.metric-row {
  margin-bottom: 2px;
}

.metric-card {
  min-height: 102px;
}

.metric-label {
  display: block;
  color: #6b7280;
  margin-bottom: 8px;
  font-size: 13px;
}

.metric-card strong {
  font-size: 24px;
  color: #1f2937;
}

.chart {
  height: 320px;
}

.stack-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.meta {
  color: #6b7280;
  font-size: 12px;
}
</style>
