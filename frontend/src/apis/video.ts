import { httpRequest } from "../utils/http"

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

export interface VideoListItem {
  id: number | string
  title: string
  coverUrl: string | null
  status: string
  durationSec: number | null
  publishAt: string | null
  createdAt: string
  recommendReason?: string | null
}

export interface VideoDetail {
  id: number | string
  title: string
  description: string | null
  coverUrl: string | null
  durationSec: number | null
  status: string
  publishAt: string | null
  createdAt: string
  updatedAt: string
  aiSummary: string | null
  aiTags: string[] | null
  aiCategories: string[] | null
}

export interface VideoPlaySources {
  videoId: number | string
  hlsMasterUrl: string | null
  mp4360Url: string | null
  mp4720Url: string | null
  mp41080Url: string | null
}

export interface UploadInitRequest {
  title: string
  description?: string
  fileName: string
  mimeType: string
  fileSize: number
}

export interface UploadInitResponse {
  videoId: number | string
  storageProvider: string
  objectKey: string
  uploadUrl: string
}

export interface UploadCompleteRequest {
  videoId: number | string
  objectKey: string
  mimeType?: string
  fileSize?: number
  checksumSha256?: string
}

export interface UploadCompleteResponse {
  videoId: number | string
  videoFileId: number
  transcodeTaskId: number
  status: string
}

export interface UploadCoverResponse {
  objectKey: string
  coverUrl: string
}

export interface VideoUpdateRequest {
  title?: string
  description?: string
  coverUrl?: string
}

export interface RecommendationItem {
  videoId: number | string
  rankIndex: number
  scoreTotal: number
  scoreContent: number
  scoreCf: number
  scoreHot: number
  title: string
  coverUrl: string | null
  durationSec: number | null
  recommendReason: string | null
}

export interface VideoHotRankItem {
  windowType: "24h" | "7d"
  bucketTime: string
  videoId: number | string
  rankIndex: number
  hotScore: number
  title: string
  coverUrl: string | null
  durationSec: number | null
  publishAt: string | null
}

export interface VideoLikeSummary {
  videoId: number | string
  likeCount: number
  likedByCurrentUser: boolean
}

export interface VideoCommentItem {
  id: number | string
  userId: number | string
  username: string
  content: string
  createdAt: string
  pinned?: boolean
}

export interface ExternalVideoCreateRequest {
  title: string
  description?: string
  coverUrl?: string
  sourceProtocol: "mp4" | "hls"
  sourceUrl: string
  durationSec?: number
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

export async function getPublicVideos(page = 1, pageSize = 12, keyword = "") {
  const response = await httpRequest<PageResult<VideoListItem>>(
    `/api/videos${queryString({ page, pageSize, keyword })}`,
    { method: "GET" },
  )
  return response.data
}

export async function getHomeRecommendations(limit = 12) {
  const response = await httpRequest<RecommendationItem[]>(`/api/recommend/home${queryString({ limit })}`, {
    method: "GET",
  })
  return response.data
}

export async function getHotRank(windowType: "24h" | "7d" = "24h", limit = 10) {
  const response = await httpRequest<VideoHotRankItem[]>(`/api/recommend/hot${queryString({ windowType, limit })}`, {
    method: "GET",
  })
  return response.data
}

export async function sendRecommendFeedback(videoId: number | string, action: string, scene = "home") {
  const response = await httpRequest<string>("/api/recommend/feedback", {
    method: "POST",
    body: JSON.stringify({ videoId, action, scene }),
  })
  return response.data
}

export async function getVideoDetail(videoId: number | string) {
  const response = await httpRequest<VideoDetail>(`/api/videos/${videoId}`, { method: "GET" })
  return response.data
}

export async function getVideoPlaySources(videoId: number | string) {
  const response = await httpRequest<VideoPlaySources>(`/api/videos/${videoId}/play-sources`, { method: "GET" })
  return response.data
}

export async function getAdminVideos(page = 1, pageSize = 10, status = "", keyword = "") {
  const response = await httpRequest<PageResult<VideoListItem>>(
    `/api/admin/videos${queryString({ page, pageSize, status, keyword })}`,
    { method: "GET" },
  )
  return response.data
}

export async function getAdminVideo(videoId: number | string) {
  const response = await httpRequest<VideoDetail>(`/api/admin/videos/${videoId}`, { method: "GET" })
  return response.data
}

export async function updateVideo(videoId: number | string, payload: VideoUpdateRequest) {
  const response = await httpRequest<VideoDetail>(`/api/admin/videos/${videoId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function publishVideo(videoId: number | string) {
  const response = await httpRequest<VideoDetail>(`/api/admin/videos/${videoId}/publish`, { method: "POST" })
  return response.data
}

export async function unpublishVideo(videoId: number | string) {
  const response = await httpRequest<VideoDetail>(`/api/admin/videos/${videoId}/unpublish`, { method: "POST" })
  return response.data
}

export async function deleteVideo(videoId: number | string) {
  const response = await httpRequest<string>(`/api/admin/videos/${videoId}`, { method: "DELETE" })
  return response.data
}

export async function uploadInit(payload: UploadInitRequest) {
  const response = await httpRequest<UploadInitResponse>("/api/videos/upload/init", {
    method: "POST",
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function uploadLocalFile(videoId: number | string, objectKey: string, file: File) {
  const formData = new FormData()
  formData.append("file", file)
  const response = await httpRequest<string>(`/api/videos/upload/local/${videoId}${queryString({ objectKey })}`, {
    method: "POST",
    body: formData,
    skipJsonContentType: true,
  })
  return response.data
}

export interface UploadLocalFileWithProgressParams {
  videoId: number | string
  objectKey: string
  file: File
  uploadUrl?: string
  onProgress?: (percent: number) => void
}

export async function uploadLocalFileWithProgress(params: UploadLocalFileWithProgressParams): Promise<string> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ""
  const fallbackUrl = `/api/videos/upload/local/${params.videoId}${queryString({ objectKey: params.objectKey })}`
  const rawUploadUrl = params.uploadUrl?.trim() || fallbackUrl
  const uploadUrl =
    rawUploadUrl.startsWith("http://") || rawUploadUrl.startsWith("https://")
      ? rawUploadUrl
      : `${baseUrl}${rawUploadUrl}`

  return await new Promise<string>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.withCredentials = true
    xhr.open("POST", uploadUrl)

    const visitorId = localStorage.getItem("visitor_id")
    if (visitorId) {
      xhr.setRequestHeader("X-Visitor-Id", visitorId)
    }

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable || !params.onProgress) {
        return
      }
      const percent = Math.max(0, Math.min(100, Math.round((event.loaded * 100) / event.total)))
      params.onProgress(percent)
    }

    xhr.onerror = () => {
      reject(new Error("上传请求失败"))
    }

    xhr.onload = () => {
      try {
        const payload = JSON.parse(xhr.responseText) as {
          code: number
          message: string
          data: string
        }

        if (xhr.status >= 200 && xhr.status < 300 && payload.code === 0) {
          params.onProgress?.(100)
          resolve(payload.data)
          return
        }

        reject(new Error(payload.message || "上传失败"))
      } catch (_err) {
        reject(new Error("上传响应解析失败"))
      }
    }

    const formData = new FormData()
    formData.append("file", params.file)
    xhr.send(formData)
  })
}

export async function uploadComplete(payload: UploadCompleteRequest) {
  const response = await httpRequest<UploadCompleteResponse>("/api/videos/upload/complete", {
    method: "POST",
    body: JSON.stringify(payload),
  })

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "上传入库失败")
  }

  return response.data
}

export async function uploadCoverImage(videoId: number | string, file: File) {
  const formData = new FormData()
  formData.append("file", file)

  const response = await httpRequest<UploadCoverResponse>(`/api/videos/upload/cover/${videoId}`, {
    method: "POST",
    body: formData,
    skipJsonContentType: true,
  })

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "封面上传失败")
  }

  return response.data
}

export async function createExternalVideo(payload: ExternalVideoCreateRequest) {
  const response = await httpRequest<VideoDetail>("/api/admin/videos/external", {
    method: "POST",
    body: JSON.stringify(payload),
  })
  return response.data
}

export async function getVideoLikeSummary(videoId: number | string) {
  const response = await httpRequest<VideoLikeSummary>(`/api/videos/${videoId}/likes/summary`, { method: "GET" })
  return response.data
}

export async function addVideoLike(videoId: number | string) {
  const response = await httpRequest<string>(`/api/videos/${videoId}/likes`, { method: "POST" })
  return response.data
}

export async function removeVideoLike(videoId: number | string) {
  const response = await httpRequest<string>(`/api/videos/${videoId}/likes`, { method: "DELETE" })
  return response.data
}

export async function getVideoComments(videoId: number | string, page = 1, pageSize = 20) {
  const response = await httpRequest<PageResult<VideoCommentItem>>(
    `/api/videos/${videoId}/comments${queryString({ page, pageSize })}`,
    { method: "GET" },
  )
  return response.data
}

export async function createVideoComment(videoId: number | string, content: string) {
  const response = await httpRequest<VideoCommentItem>(`/api/videos/${videoId}/comments`, {
    method: "POST",
    body: JSON.stringify({ content }),
  })
  return response.data
}

export async function deleteVideoComment(videoId: number | string, commentId: number | string) {
  const response = await httpRequest<string>(`/api/videos/${videoId}/comments/${commentId}`, {
    method: "DELETE",
  })
  return response.data
}

export async function pinVideoComment(videoId: number | string, commentId: number | string) {
  const response = await httpRequest<string>(`/api/videos/${videoId}/comments/${commentId}/pin`, {
    method: "POST",
  })
  return response.data
}

export async function unpinVideoComment(videoId: number | string, commentId: number | string) {
  const response = await httpRequest<string>(`/api/videos/${videoId}/comments/${commentId}/unpin`, {
    method: "POST",
  })
  return response.data
}

export async function getAdminVideoComments(videoId: number | string, page = 1, pageSize = 10) {
  const response = await httpRequest<PageResult<VideoCommentItem>>(
    `/api/admin/videos/${videoId}/comments${queryString({ page, pageSize })}`,
    { method: "GET" },
  )
  return response.data
}

export async function deleteAdminVideoComment(videoId: number | string, commentId: number | string) {
  const response = await httpRequest<string>(`/api/admin/videos/${videoId}/comments/${commentId}`, {
    method: "DELETE",
  })
  return response.data
}

export async function pinAdminVideoComment(videoId: number | string, commentId: number | string) {
  const response = await httpRequest<string>(`/api/admin/videos/${videoId}/comments/${commentId}/pin`, {
    method: "POST",
  })
  return response.data
}

export async function unpinAdminVideoComment(videoId: number | string, commentId: number | string) {
  const response = await httpRequest<string>(`/api/admin/videos/${videoId}/comments/${commentId}/unpin`, {
    method: "POST",
  })
  return response.data
}
