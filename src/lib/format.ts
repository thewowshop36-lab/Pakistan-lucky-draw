export function formatPkr(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0)
  return `Rs ${amount.toLocaleString("en-PK", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`
}

export function getProgress(sold: number, total: number): number {
  if (total <= 0) return 0
  return Math.min(100, Math.max(0, (sold / total) * 100))
}

export function safeInternalPath(path: string | null | undefined, fallback = "/profile"): string {
  if (!path || !path.startsWith("/") || path.startsWith("//") || path.includes("\\")) {
    return fallback
  }
  return path
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"
  return new Intl.DateTimeFormat("en-PK", { dateStyle: "medium" }).format(date)
}
