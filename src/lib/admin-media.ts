export async function uploadAdminMedia(file: File): Promise<string> {
  const body = new FormData()
  body.append("file", file)

  const response = await fetch("/api/admin/media", { method: "POST", body })
  const result = await response.json().catch(() => null) as { url?: string; error?: string } | null

  if (!response.ok || !result?.url) {
    throw new Error(result?.error || "Upload failed. Use an image or MP4/WebM video up to 15 MB.")
  }

  return result.url
}
