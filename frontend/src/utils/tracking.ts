import { httpRequest } from "./http"

export type TrackEventType = "exposure" | "click" | "play" | "progress" | "complete"

export interface TrackEventInput {
  eventType: TrackEventType
  videoId?: number | string
  progressSec?: number
  abExperimentId?: number
  abVariant?: string
  extra?: Record<string, unknown>
}

interface TrackEventPayload {
  eventType: TrackEventType
  videoId?: number | string
  progressSec?: number
  abExperimentId?: number
  abVariant?: string
  pagePath?: string
  sessionId?: string
  eventTime: number
  eventId: string
  extraJson?: string
}

const STORAGE_KEY = "tracking_queue_v1"
const MAX_BATCH_SIZE = 20
const MAX_RETRY = 3
const FLUSH_INTERVAL_MS = 5000

let queue: Array<TrackEventPayload & { retryCount: number }> = []
let started = false

function getSessionId() {
  const key = "tracking_session_id"
  const existing = sessionStorage.getItem(key)
  if (existing) {
    return existing
  }
  const next = `s_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
  sessionStorage.setItem(key, next)
  return next
}

function persistQueue() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(queue))
}

function restoreQueue() {
  const text = localStorage.getItem(STORAGE_KEY)
  if (!text) {
    return
  }
  try {
    const parsed = JSON.parse(text) as Array<TrackEventPayload & { retryCount?: number }>
    queue = (parsed || []).map((item) => ({
      ...item,
      retryCount: item.retryCount || 0,
    }))
  } catch (_err) {
    queue = []
  }
}

function makeEventId() {
  return `e_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`
}

async function flush() {
  if (queue.length === 0) {
    return
  }

  const batch = queue.slice(0, MAX_BATCH_SIZE)
  try {
    await httpRequest<{ received: number; stored: number }>("/api/events/batch", {
      method: "POST",
      body: JSON.stringify({
        events: batch.map((item) => ({
          eventType: item.eventType,
          videoId: item.videoId,
          progressSec: item.progressSec,
          abExperimentId: item.abExperimentId,
          abVariant: item.abVariant,
          pagePath: item.pagePath,
          sessionId: item.sessionId,
          eventTime: item.eventTime,
          eventId: item.eventId,
          extraJson: item.extraJson,
        })),
      }),
    })

    queue = queue.slice(batch.length)
    persistQueue()
  } catch (_err) {
    queue = queue
      .map((item, index) => {
        if (index < batch.length) {
          return {
            ...item,
            retryCount: item.retryCount + 1,
          }
        }
        return item
      })
      .filter((item) => item.retryCount <= MAX_RETRY)
    persistQueue()
  }
}

function schedule() {
  setInterval(() => {
    void flush()
  }, FLUSH_INTERVAL_MS)

  window.addEventListener("beforeunload", () => {
    persistQueue()
  })
}

export function initTracking() {
  if (started) {
    return
  }
  started = true
  restoreQueue()
  schedule()
}

export function trackEvent(input: TrackEventInput) {
  if (!started) {
    initTracking()
  }

  const payload: TrackEventPayload & { retryCount: number } = {
    eventType: input.eventType,
    videoId: input.videoId,
    progressSec: input.progressSec,
    abExperimentId: input.abExperimentId,
    abVariant: input.abVariant,
    extraJson: input.extra ? JSON.stringify(input.extra) : undefined,
    eventTime: Date.now(),
    eventId: makeEventId(),
    pagePath: window.location.pathname,
    sessionId: getSessionId(),
    retryCount: 0,
  }

  queue.push(payload)
  persistQueue()

  if (queue.length >= MAX_BATCH_SIZE) {
    void flush()
  }
}

export function trackExposure(videoId: number | string, extra?: Record<string, unknown>) {
  trackEvent({ eventType: "exposure", videoId, extra })
}

export function trackClick(videoId: number | string, extra?: Record<string, unknown>) {
  trackEvent({ eventType: "click", videoId, extra })
}

export function trackPlay(videoId: number | string, extra?: Record<string, unknown>) {
  trackEvent({ eventType: "play", videoId, extra })
}

export function trackProgress(videoId: number | string, progressSec: number, extra?: Record<string, unknown>) {
  trackEvent({ eventType: "progress", videoId, progressSec, extra })
}

export function trackComplete(videoId: number | string, extra?: Record<string, unknown>) {
  trackEvent({ eventType: "complete", videoId, extra })
}
