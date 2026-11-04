"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { ShieldCheck } from "lucide-react"
import type { Draw } from "@/lib/types"

export function FinalizeDrawButton({ draw }: { draw: Draw }) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const router = useRouter()
  const closesByAdmin = draw.end_condition === "admin_close"
  const canCloseEarly = closesByAdmin && draw.sold_tickets < draw.total_tickets
  const ready = draw.sold_tickets > 0 && (closesByAdmin || draw.sold_tickets === draw.total_tickets)

  async function finalize() {
    if (busy || !ready || draw.reserved_tickets > 0) return
    setBusy(true)
    setMessage("")
    try {
      const response = await fetch(`/api/draws/${draw.id}/finalize`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ manual: closesByAdmin }),
      })
      const result = await response.json().catch(() => null) as { error?: string } | null
      if (!response.ok) {
        setMessage(result?.error || "The result could not be published yet.")
        return
      }
      setMessage("Verified result published.")
      router.refresh()
    } catch {
      setMessage("The result could not be published. Check the connection and retry.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mt-4 space-y-2">
      <button type="button" className="secondary-button min-h-9 px-3" onClick={() => void finalize()} disabled={!ready || draw.reserved_tickets > 0 || busy}>
        <ShieldCheck size={15} aria-hidden="true" />
        {busy ? "Waiting for NIST…" : canCloseEarly ? "Close early and draw now" : closesByAdmin ? "Close and draw now" : "Publish verified result"}
      </button>
      {draw.reserved_tickets > 0 && <p className="text-xs text-muted">Review {draw.reserved_tickets} pending payment ticket{draw.reserved_tickets === 1 ? "" : "s"} before closing.</p>}
      {message && <p className={`rounded-lg px-3 py-2 text-xs ${message.includes("published") ? "bg-green-pale text-green" : "bg-[#fbefef] text-[#8d4545]"}`} role={message.includes("published") ? "status" : "alert"}>{message}</p>}
    </div>
  )
}
