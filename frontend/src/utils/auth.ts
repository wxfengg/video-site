import { httpRequest } from "./http"

export interface AdminSessionInfo {
  loggedIn: boolean
  username: string | null
}

export async function loginAdmin(username: string, password: string): Promise<boolean> {
  const response = await httpRequest<AdminSessionInfo>("/api/admin/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  })
  return response.code === 0 && response.data.loggedIn
}

export async function fetchAdminSession(): Promise<AdminSessionInfo> {
  try {
    const response = await httpRequest<AdminSessionInfo>("/api/admin/auth/me", {
      method: "GET",
    })

    if (response.code !== 0) {
      return {
        loggedIn: false,
        username: null,
      }
    }

    return response.data
  } catch (_err) {
    return {
      loggedIn: false,
      username: null,
    }
  }
}

export async function logoutAdmin(): Promise<void> {
  await httpRequest<null>("/api/admin/auth/logout", {
    method: "POST",
  })
}
