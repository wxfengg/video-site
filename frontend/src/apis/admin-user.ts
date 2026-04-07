import { httpRequest } from "../utils/http"
import type { PageResult } from "./video"

export interface AdminUserListItem {
  id: number | string
  username: string
  status: number
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminUserCreateRequest {
  username: string
  password: string
  status?: 0 | 1
}

export interface AdminUserStatusUpdateRequest {
  status: 0 | 1
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

export async function listAdminUsers(page = 1, pageSize = 10, keyword = "") {
  const response = await httpRequest<PageResult<AdminUserListItem>>(
    `/api/admin/users${queryString({ page, pageSize, keyword })}`,
    { method: "GET" },
  )

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "加载用户列表失败")
  }

  return response.data
}

export async function createAdminUser(payload: AdminUserCreateRequest) {
  const response = await httpRequest<AdminUserListItem>("/api/admin/users", {
    method: "POST",
    body: JSON.stringify(payload),
  })

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "创建用户失败")
  }

  return response.data
}

export async function deleteAdminUser(userId: number | string) {
  const response = await httpRequest<string>(`/api/admin/users/${userId}`, {
    method: "DELETE",
  })

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "删除用户失败")
  }

  return response.data
}

export async function updateAdminUserStatus(userId: number | string, payload: AdminUserStatusUpdateRequest) {
  const response = await httpRequest<AdminUserListItem>(`/api/admin/users/${userId}/status`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  })

  if (response.code !== 0 || !response.data) {
    throw new Error(response.message || "更新用户状态失败")
  }

  return response.data
}
