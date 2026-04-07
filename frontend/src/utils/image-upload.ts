const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
const ALLOWED_IMAGE_MIME_TYPES = new Set(["image/jpeg", "image/jpg", "image/png", "image/webp"])

export const IMAGE_ACCEPT_ATTR = "image/jpeg,image/jpg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
export const IMAGE_UPLOAD_TIP_TEXT = "支持 jpg/jpeg、png、webp，文件不超过 5MB"

export function validateImageFile(file: File): string | null {
  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return "封面图片不能超过 5MB"
  }

  const type = (file.type || "").toLowerCase()
  if (type) {
    if (!ALLOWED_IMAGE_MIME_TYPES.has(type)) {
      return "封面仅支持 jpg/jpeg、png、webp 格式"
    }
    return null
  }

  const lowerName = file.name.toLowerCase()
  if (
    !(
      lowerName.endsWith(".jpg") ||
      lowerName.endsWith(".jpeg") ||
      lowerName.endsWith(".png") ||
      lowerName.endsWith(".webp")
    )
  ) {
    return "封面仅支持 jpg/jpeg、png、webp 格式"
  }

  return null
}
