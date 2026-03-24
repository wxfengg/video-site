const VISITOR_ID_KEY = "visitor_id"
const VISITOR_COOKIE_KEY = "visitor_id"
const VISITOR_COOKIE_MAX_AGE_DAYS = 180

function generateVisitorId(): string {
  const random = Math.random().toString(36).slice(2, 10)
  return `v_${Date.now().toString(36)}_${random}`
}

function setCookie(name: string, value: string, days: number) {
  const expires = new Date(Date.now() + days * 24 * 60 * 60 * 1000).toUTCString()
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`
}

function getCookie(name: string): string | null {
  const encodedName = `${name}=`
  const items = document.cookie.split(";")
  for (const item of items) {
    const trimmed = item.trim()
    if (trimmed.startsWith(encodedName)) {
      return decodeURIComponent(trimmed.substring(encodedName.length))
    }
  }
  return null
}

export function getOrCreateVisitorId(): string {
  let visitorId = localStorage.getItem(VISITOR_ID_KEY)
  if (!visitorId) {
    visitorId = getCookie(VISITOR_COOKIE_KEY)
  }

  if (!visitorId) {
    visitorId = generateVisitorId()
  }

  localStorage.setItem(VISITOR_ID_KEY, visitorId)
  setCookie(VISITOR_COOKIE_KEY, visitorId, VISITOR_COOKIE_MAX_AGE_DAYS)
  return visitorId
}

export function getVisitorId(): string | null {
  return localStorage.getItem(VISITOR_ID_KEY) || getCookie(VISITOR_COOKIE_KEY)
}
