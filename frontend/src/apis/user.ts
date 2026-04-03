import { httpRequest } from "../utils/http"
import type { PageResult } from "./video"

export interface UserSessionInfo {
  loggedIn: boolean
  userId: number | null
  username: string | null
}

export interface UserFavoriteItem {
  id: number | string
  title: string
  coverUrl: string | null
  status: string
  durationSec: number | null
  publishAt: string | null
  favoritedAt: string | null
}

export interface UserWatchHistoryItem {
  id: number | string
  title: string
  coverUrl: string | null
  status: string
  durationSec: number | null
  lastProgressSec: number
  durationSecSnapshot: number | null
  completionRate: number
  completed90: boolean
  lastWatchedAt: string | null
}

export interface UserWatchProgress {
  videoId: number | string
  progressSec: number
  durationSecSnapshot: number | null
  completionRate: number
  completed90: boolean
  lastWatchedAt: string | null
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

export async function fetchUserSession() {
  const response = await httpRequest<UserSessionInfo>("/api/auth/me", {
    method: "GET",
  })
  return response.data
}

export async function registerUser(username: string, password: string) {
  const response = await httpRequest<UserSessionInfo>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  })
  return response.data
}

export async function loginUser(username: string, password: string) {
  const response = await httpRequest<UserSessionInfo>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  })
  return response.data
}

export async function logoutUser() {
  const response = await httpRequest<UserSessionInfo>("/api/auth/logout", {
    method: "POST",
  })
  return response.data
}

export async function listMyFavorites(page = 1, pageSize = 12) {
  const response = await httpRequest<PageResult<UserFavoriteItem>>(
    `/api/users/me/favorites${queryString({ page, pageSize })}`,
    { method: "GET" },
  )
  return response.data
}

export async function addFavorite(videoId: number | string) {
  const response = await httpRequest<string>(`/api/users/me/favorites/${videoId}`, {
    method: "POST",
  })
  return response.data
}

export async function removeFavorite(videoId: number | string) {
  const response = await httpRequest<string>(`/api/users/me/favorites/${videoId}`, {
    method: "DELETE",
  })
  return response.data
}

export async function listMyHistory(page = 1, pageSize = 12) {
  const response = await httpRequest<PageResult<UserWatchHistoryItem>>(
    `/api/users/me/history${queryString({ page, pageSize })}`,
    { method: "GET" },
  )
  return response.data
}

export async function getMyVideoProgress(videoId: number | string) {
  const response = await httpRequest<UserWatchProgress>(`/api/users/me/history/${videoId}/progress`, {
    method: "GET",
  })
  return response.data
}

export async function updateMyVideoProgress(
  videoId: number | string,
  progressSec: number,
  durationSecSnapshot?: number,
) {
  const response = await httpRequest<UserWatchProgress>(`/api/users/me/history/${videoId}/progress`, {
    method: "PUT",
    body: JSON.stringify({ progressSec, durationSecSnapshot }),
  })
  return response.data
}
