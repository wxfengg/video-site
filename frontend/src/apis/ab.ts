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
  targetVideoId: number | string
  status: "draft" | "running" | "stopped"
  metricPrimary: string
  startAt?: string | null
  endAt?: string | null
  variants: AbVariant[]
}

export interface AbExperimentSaveRequest {
  name: string
  scene: string
  targetVideoId: number | string
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

export interface AbAssignmentResponse {
  experimentId: number
  experimentName: string
  variantCode: string
  coverUrl: string | null
}

export interface AbVariantCoverUploadResponse {
  objectKey: string
  coverUrl: string
}

function queryString(params: Record<string, unknown>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return
    }
    search.set(key, String(value))
  })
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ""
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

export async function deleteAbExperiment(experimentId: number) {
  const response = await httpRequest<string>(`/api/admin/ab/experiments/${experimentId}`, {
    method: "DELETE",
  })

  if (response.code !== 0) {
    throw new Error(response.message || "删除实验失败")
  }

  return response.data
}

export async function getAbAssignment(scene: string, targetVideoId?: number | string) {
  try {
    const response = await httpRequest<AbAssignmentResponse>(
      `/api/ab/assignment${queryString({ scene, targetVideoId })}`,
      {
        method: "GET",
      },
    )

    if (response.code !== 0 || !response.data) {
      return null
    }

    return response.data
  } catch (_err) {
    return null
  }
}

export async function uploadAbVariantCover(file: File) {
  const formData = new FormData()
  formData.append("file", file)

  const response = await httpRequest<AbVariantCoverUploadResponse>("/api/admin/ab/variants/cover/upload", {
    method: "POST",
    body: formData,
    skipJsonContentType: true,
  })

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "上传变体封面失败")
  }

  return response.data
}

export async function getAbCtrReport(experimentId: number) {
  const response = await httpRequest<AbCtrReportResponse>(`/api/admin/ab/experiments/${experimentId}/ctr-report`, {
    method: "GET",
  })
  return response.data
}
