"use client"

import { useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import useSWRMutation from "swr/mutation"
import { ShieldCheck } from "lucide-react"

async function requestDrawResult(url: string) {
  const response = await fetch(url, { method: "POST" })
  const result = await response.json().catch(() => null) as { error?: string } | null
  if (!response.ok) throw new Error(result?.error || "The result could not be published yet.")
  return result
}

export function SoldOutDrawFinalizer({ drawId }: { drawId: string }) {
  const [countdown, setCountdown] = useState(5)
  const [message, setMessage] = useState("")
  const attempted = useRef(false)
  const router = useRouter()
  const { trigger, isMutating } = useSWRMutation(`/api/draws/${drawId}/finalize`, requestDrawResult)

  async function finalize() {
    if (attempted.current || isMutating) return
    attempted.current = true
    setMessage("")
    try {
      await trigger()
      router.refresh()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "The result could not be published yet.")
    }
  }

  useEffect(() => {
    if (countdown > 0 || attempted.current) return
    void finalize()
  }, [countdown])

  useEffect(() => {
    if (countdown <= 0 || message) return
    const timer = window.setTimeout(() => setCountdown((value) => Math.max(0, value - 1)), 1000)
    return () => window.clearTimeout(timer)
  }, [countdown, message])

  return (
    <div className="mt-3 rounded-xl border border-green/15 bg-green-pale p-3.5">
      <p className="flex items-center gap-2 text-xs font-bold text-green" role="status">
        <ShieldCheck size={15} aria-hidden="true" />
        {isMutating ? "Checking the public randomness beacon…" : message ? "Result publication paused" : `Sold out · publishing a verifiable result in ${countdown}…`}
      </p>
      {message && (
        <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs leading-5 text-muted">{message}</p>
          <button type="button" className="secondary-button min-h-9 px-3 text-xs" onClick={() => { attempted.current = false; setCountdown(0); setMessage(""); void finalize() }} disabled={isMutating}>Retry</button>
        </div>
      )}
    </div>
  )
}
