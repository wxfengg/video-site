import { httpRequest } from "../utils/http"

export interface DashboardOverviewResponse {
  from: string
  to: string
  playPv: number
  peakDau: number
  newUsers: number
  publishedVideos: number
  runningExperiments: number
  totalComments: number
  totalLikes: number
  hotVideos: DashboardHotVideoItem[]
  abSummary: DashboardAbSummaryItem[]
}

export interface DashboardHotVideoItem {
  videoId: number | string
  rankIndex: number
  hotScore: number
  title: string
  coverUrl: string | null
}

export interface DashboardAbSummaryItem {
  experimentId: number
  experimentName: string
  variantCode: string
  exposureUv: number
  clickUv: number
  ctr: number
}

export interface DashboardTrafficTrendResponse {
  from: string
  to: string
  totalPlayPv: number
  peakDau: number
  points: DashboardTrafficPoint[]
}

export interface DashboardTrafficPoint {
  bucketTime: string
  dau: number
  newUsers: number
  playPv: number
}

export interface DashboardUserGrowthResponse {
  from: string
  to: string
  totalNewUsers: number
  currentUserTotal: number
  points: DashboardUserGrowthPoint[]
}

export interface DashboardUserGrowthPoint {
  day: string
  newUsers: number
  cumulativeUsers: number
}

export interface DashboardPlayFunnelResponse {
  videoId: number | null
  from: string
  to: string
  exposureUv: number
  clickUv: number
  playUv: number
  completeUv: number
  ctr: number
  playThroughRate: number
  completionRate: number
  stages: DashboardFunnelStage[]
}

export interface DashboardFunnelStage {
  stage: string
  uv: number
}

interface DashboardRangeQuery extends Record<string, unknown> {
  from?: string
  to?: string
}

interface DashboardFunnelQuery extends DashboardRangeQuery {
  videoId?: number
}

function queryString<T extends object>(params: T) {
  const search = new URLSearchParams()
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value === null || value === undefined || value === "") {
      return
    }
    search.set(key, String(value))
  })

  const encoded = search.toString()
  return encoded ? `?${encoded}` : ""
}

export async function getDashboardOverview() {
  const response = await httpRequest<DashboardOverviewResponse>("/api/admin/dashboard/overview", {
    method: "GET",
  })
  return response.data
}

export async function getDashboardTrafficTrend(query: DashboardRangeQuery = {}) {
  const response = await httpRequest<DashboardTrafficTrendResponse>(
    `/api/admin/dashboard/traffic-trend${queryString(query)}`,
    { method: "GET" },
  )
  return response.data
}

export async function getDashboardUserGrowth(query: DashboardRangeQuery = {}) {
  const response = await httpRequest<DashboardUserGrowthResponse>(
    `/api/admin/dashboard/user-growth${queryString(query)}`,
    { method: "GET" },
  )
  return response.data
}

export async function getDashboardPlayFunnel(query: DashboardFunnelQuery = {}) {
  const response = await httpRequest<DashboardPlayFunnelResponse>(
    `/api/admin/dashboard/play-funnel${queryString(query)}`,
    { method: "GET" },
  )
  return response.data
}
