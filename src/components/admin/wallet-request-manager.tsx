"use client"

import { useState } from "react"
import { ArrowDownToLine, ArrowUpFromLine, Check, Clock3, X } from "lucide-react"
import { useRouter } from "next/navigation"
import { formatDate, formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"
import type { Profile, WalletRequest } from "@/lib/types"

type Decision = "approve" | "paid" | "reject"
type Feedback = { kind: "success" | "error"; text: string }

const statusLabels: Record<WalletRequest["status"], string> = {
  pending: "Awaiting review",
  approved: "Credited",
  paid: "Paid",
  rejected: "Rejected",
}

export function WalletRequestManager({ requests, profiles }: { requests: WalletRequest[]; profiles: Profile[] }) {
  const [filter, setFilter] = useState<"pending" | "all">("pending")
  const [notes, setNotes] = useState<Record<string, string>>({})
  const [busyId, setBusyId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const router = useRouter()
  const supabase = createClient()
  const profilesById = new Map(profiles.map((profile) => [profile.id, profile]))
  const pendingCount = requests.filter((request) => request.status === "pending").length
  const visibleRequests = filter === "pending" ? requests.filter((request) => request.status === "pending") : requests

  async function reviewRequest(request: WalletRequest, decision: Decision) {
    const note = notes[request.id]?.trim() ?? ""
    if (!note) {
      setFeedback({ kind: "error", text: "Add a short note for the user before processing this request." })
      return
    }

    const amount = formatPkr(request.amount)
    const confirmation = decision === "approve"
      ? `Verify and credit the ${amount} deposit?`
      : decision === "paid"
        ? `Confirm that the ${amount} withdrawal was sent manually?`
        : `Reject this ${amount} request? A pending withdrawal will be returned to the wallet.`
    if (!window.confirm(confirmation)) return

    setBusyId(request.id)
    setFeedback(null)
    const { error } = await supabase.rpc("admin_review_wallet_request", {
      p_request_id: request.id,
      p_decision: decision,
      p_admin_note: note,
    })
    setBusyId(null)

    if (error) {
      setFeedback({ kind: "error", text: "The request could not be updated. Refresh and try again." })
      return
    }

    setNotes((current) => ({ ...current, [request.id]: "" }))
    setFeedback({ kind: "success", text: "Wallet request updated." })
    router.refresh()
  }

  return (
    <section className="space-y-5" aria-labelledby="wallet-requests-heading">
      <div className="surface-card flex flex-wrap items-end justify-between gap-4 p-5 sm:p-6">
        <div>
          <p className="section-eyebrow">Manual transfers</p>
          <h2 id="wallet-requests-heading" className="mt-1 text-xl font-bold text-ink-strong">Deposit and withdrawal requests</h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">Verify deposits before crediting. Send withdrawals through JazzCash or Easypaisa before marking them paid. Rejecting a pending withdrawal returns its reserved balance.</p>
        </div>
        <span className="status-pill"><Clock3 size={14} aria-hidden="true" />{pendingCount} pending</span>
      </div>

      <div className="flex gap-2" role="group" aria-label="Filter wallet requests">
        <button type="button" aria-pressed={filter === "pending"} onClick={() => setFilter("pending")} className={filter === "pending" ? "primary-button" : "secondary-button"}>Pending ({pendingCount})</button>
        <button type="button" aria-pressed={filter === "all"} onClick={() => setFilter("all")} className={filter === "all" ? "primary-button" : "secondary-button"}>All requests</button>
      </div>

      {feedback && <p className={`rounded-xl px-4 py-3 text-sm ${feedback.kind === "error" ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role={feedback.kind === "error" ? "alert" : "status"}>{feedback.text}</p>}

      {visibleRequests.length ? (
        <div className="space-y-3">
          {visibleRequests.map((request) => {
            const member = profilesById.get(request.user_id)
            const isPending = request.status === "pending"
            const RequestIcon = request.request_type === "deposit" ? ArrowDownToLine : ArrowUpFromLine
            return (
              <article key={request.id} className="surface-card p-4 sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="flex min-w-0 items-start gap-3">
                    <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-green-pale text-green"><RequestIcon size={18} aria-hidden="true" /></span>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="font-bold text-ink-strong">{request.request_type === "deposit" ? "Deposit" : "Withdrawal"} · {request.provider === "jazzcash" ? "JazzCash" : "Easypaisa"}</h3>
                        <span className={`rounded-full px-2.5 py-1 text-[0.65rem] font-bold ${request.status === "rejected" ? "bg-[#fbefef] text-[#8d4545]" : request.status === "pending" ? "bg-gold-soft text-[#765c22]" : "bg-green-pale text-green"}`}>{statusLabels[request.status]}</span>
                      </div>
                      <p className="mt-1 text-xs text-muted">{member?.name || "Member"} · {request.user_id.slice(0, 8)} · {formatDate(request.created_at)}</p>
                    </div>
                  </div>
                  <p className="text-lg font-extrabold text-ink-strong">{formatPkr(request.amount)}</p>
                </div>

                <dl className="mt-4 grid gap-2 border-t border-line pt-4 text-sm sm:grid-cols-2">
                  <div><dt className="text-xs font-semibold text-muted">Transfer account</dt><dd className="mt-0.5 font-semibold text-ink">{request.account_phone}</dd></div>
                  {request.transaction_reference && <div><dt className="text-xs font-semibold text-muted">Transaction reference</dt><dd className="mt-0.5 break-all font-semibold text-ink">{request.transaction_reference}</dd></div>}
                </dl>

                {request.admin_note && !isPending && <p className="mt-3 rounded-lg bg-surface-soft px-3 py-2 text-xs leading-5 text-muted">Review note: {request.admin_note}</p>}

                {isPending && (
                  <div className="mt-4 border-t border-line pt-4">
                    <label className="field-label" htmlFor={`wallet-review-note-${request.id}`}>Note shown to the user</label>
                    <textarea
                      id={`wallet-review-note-${request.id}`}
                      className="field-control min-h-20"
                      maxLength={240}
                      value={notes[request.id] ?? ""}
                      onChange={(event) => setNotes((current) => ({ ...current, [request.id]: event.target.value }))}
                      placeholder={request.request_type === "deposit" ? "Example: transfer verified and credited." : "Example: transfer sent to the supplied number."}
                      required
                    />
                    <div className="mt-3 flex flex-wrap gap-2">
                      {request.request_type === "deposit" ? (
                        <button type="button" className="primary-button" disabled={busyId === request.id} onClick={() => void reviewRequest(request, "approve")}>
                          <Check size={15} aria-hidden="true" />{busyId === request.id ? "Updating…" : "Verify and credit"}
                        </button>
                      ) : (
                        <button type="button" className="primary-button" disabled={busyId === request.id} onClick={() => void reviewRequest(request, "paid")}>
                          <Check size={15} aria-hidden="true" />{busyId === request.id ? "Updating…" : "Mark as paid"}
                        </button>
                      )}
                      <button type="button" className="danger-button" disabled={busyId === request.id} onClick={() => void reviewRequest(request, "reject")}>
                        <X size={15} aria-hidden="true" />Reject request
                      </button>
                    </div>
                  </div>
                )}
              </article>
            )
          })}
        </div>
      ) : (
        <div className="surface-card p-6 text-sm text-muted">{filter === "pending" ? "There are no pending wallet requests." : "No wallet requests have been submitted."}</div>
      )}
    </section>
  )
}
