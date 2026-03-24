import { httpRequest } from "../utils/http"

export interface AbVariant {
  id?: number
  variantCode: string
  coverUrl?: string
  trafficRatio: number
}

export interface AbExperiment {
  id: number
  name: string
  scene: string
  targetVideoId: number
  status: "draft" | "running" | "stopped"
  metricPrimary: string
  startAt?: string | null
  endAt?: string | null
  variants: AbVariant[]
}

export interface AbExperimentSaveRequest {
  name: string
  scene: string
  targetVideoId: number
  metricPrimary: string
  startAt?: string
  endAt?: string
  variants: AbVariant[]
}

export interface AbCtrVariantReportItem {
  variantCode: string
  exposureUv: number
  clickUv: number
  ctr: number
}

export interface AbCtrReportResponse {
  experimentId: number
  metricPrimary: string
  variants: AbCtrVariantReportItem[]
}

export async function listAbExperiments() {
  const response = await httpRequest<AbExperiment[]>("/api/admin/ab/experiments", { method: "GET" })
  return response.data
}

export async function createAbExperiment(payload: AbExperimentSaveRequest) {
  const response = await httpRequest<AbExperiment>("/api/admin/ab/experiments", {
    method: "POST",
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function updateAbExperiment(experimentId: number, payload: AbExperimentSaveRequest) {
  const response = await httpRequest<AbExperiment>(`/api/admin/ab/experiments/${experimentId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function startAbExperiment(experimentId: number) {
  const response = await httpRequest<AbExperiment>(`/api/admin/ab/experiments/${experimentId}/start`, {
    method: "POST",
  })
  return response.data
}

export async function stopAbExperiment(experimentId: number) {
  const response = await httpRequest<AbExperiment>(`/api/admin/ab/experiments/${experimentId}/stop`, {
    method: "POST",
  })
  return response.data
}

export async function getAbCtrReport(experimentId: number) {
  const response = await httpRequest<AbCtrReportResponse>(`/api/admin/ab/experiments/${experimentId}/ctr-report`, {
    method: "GET",
  })
  return response.data
}
