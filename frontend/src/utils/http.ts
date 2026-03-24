import { getOrCreateVisitorId } from "./visitor"

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ""

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string | null
}

interface RequestOptions extends RequestInit {
  skipJsonContentType?: boolean
}

export async function httpRequest<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const { skipJsonContentType, headers, ...restOptions } = options
  const visitorId = getOrCreateVisitorId()

  const finalHeaders = new Headers(headers)
  finalHeaders.set("X-Visitor-Id", visitorId)
  if (!skipJsonContentType && !finalHeaders.has("Content-Type")) {
    finalHeaders.set("Content-Type", "application/json")
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    ...restOptions,
    headers: finalHeaders,
  })

  const payload = (await response.json()) as ApiResponse<T>
  return payload
}
