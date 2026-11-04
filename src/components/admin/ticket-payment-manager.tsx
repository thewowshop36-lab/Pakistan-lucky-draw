"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Check, ExternalLink, FileSearch, X } from "lucide-react"
import { formatDate, formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"
import type { TicketPaymentSubmission } from "@/lib/types"

function one<T>(relation: T | T[] | null | undefined): T | null {
  return Array.isArray(relation) ? relation[0] ?? null : relation ?? null
}

export function TicketPaymentManager({ submissions }: { submissions: TicketPaymentSubmission[] }) {
  const [notes, setNotes] = useState<Record<string, string>>({})
  const [proofUrls, setProofUrls] = useState<Record<string, string>>({})
  const [busyId, setBusyId] = useState("")
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const router = useRouter()
  const supabase = createClient()
  const pending = submissions.filter((submission) => submission.status === "pending")
  const reviewed = submissions.filter((submission) => submission.status !== "pending")

  async function openProof(submission: TicketPaymentSubmission) {
    setBusyId(submission.id)
    const { data, error } = await supabase.storage.from("ticket-payment-proofs").createSignedUrl(submission.proof_path, 300)
    setBusyId("")
    if (error || !data?.signedUrl) {
      setMessage("The proof file could not be opened.")
      setIsError(true)
      return
    }
    setProofUrls((current) => ({ ...current, [submission.id]: data.signedUrl }))
    window.open(data.signedUrl, "_blank", "noopener,noreferrer")
  }

  async function review(submission: TicketPaymentSubmission, decision: "approve" | "reject") {
    if (busyId) return
    setBusyId(submission.id)
    setMessage("")
    const { error } = await supabase.rpc("admin_review_ticket_payment", {
      p_submission_id: submission.id,
      p_decision: decision,
      p_admin_note: notes[submission.id]?.trim() ?? "",
    })
    setBusyId("")

    if (error) {
      const details = error.message.toLowerCase()
      setMessage(details.includes("eligible") ? "The account is no longer eligible. Reject this request or resolve the account verification first." : "The request could not be reviewed. Refresh and check its status.")
      setIsError(true)
      return
    }

    setMessage(decision === "approve" ? "Payment approved and ticket numbers assigned." : "Payment rejected and reserved tickets released.")
    setIsError(false)
    router.refresh()
  }

  function submissionCard(submission: TicketPaymentSubmission) {
    const profile = one(submission.profiles)
    const draw = one(submission.draws)
    const proofUrl = proofUrls[submission.id]
    return (
      <article key={submission.id} className="rounded-2xl border border-line bg-white p-4 sm:p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className={`status-pill ${submission.status === "pending" ? "status-pill--pending" : submission.status === "rejected" ? "status-pill--lost" : "status-pill--won"}`}>{submission.status}</span>
              <span className="text-xs font-semibold text-muted">{submission.provider === "jazzcash" ? "JazzCash" : "Easypaisa"}</span>
            </div>
            <h3 className="mt-2 truncate font-bold text-ink-strong">{profile?.name ?? "User"} · {draw?.title ?? "Draw"}</h3>
            <p className="mt-1 text-xs leading-5 text-muted">Draw {String(draw?.draw_number ?? "—").padStart(2, "0")} · {submission.quantity} ticket{submission.quantity === 1 ? "" : "s"} · Submitted {formatDate(submission.created_at)}</p>
          </div>
          <p className="text-base font-extrabold text-ink-strong">{formatPkr(submission.amount)}</p>
        </div>
        <dl className="mt-4 grid gap-2 rounded-xl bg-surface-soft p-3.5 text-xs sm:grid-cols-2">
          <div><dt className="font-semibold text-muted">Transfer reference</dt><dd className="mt-0.5 break-all font-mono text-ink">{submission.transaction_reference}</dd></div>
          <div><dt className="font-semibold text-muted">Sender</dt><dd className="mt-0.5 text-ink">{profile?.phone ?? "Phone not on profile"}</dd></div>
        </dl>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {proofUrl ? (
            <a className="secondary-button min-h-9 px-3 text-xs" href={proofUrl} target="_blank" rel="noreferrer"><ExternalLink size={14} aria-hidden="true" />Open proof</a>
          ) : (
            <button type="button" className="secondary-button min-h-9 px-3 text-xs" onClick={() => void openProof(submission)} disabled={busyId === submission.id}><FileSearch size={14} aria-hidden="true" />{busyId === submission.id ? "Loading…" : "View proof"}</button>
          )}
          {submission.reviewed_at && <span className="text-xs text-muted">Reviewed {formatDate(submission.reviewed_at)}</span>}
        </div>
        {submission.status === "pending" ? (
          <div className="mt-4 space-y-3 border-t border-line pt-4">
            <div>
              <label className="field-label" htmlFor={`payment-note-${submission.id}`}>Admin note</label>
              <input className="field-control" id={`payment-note-${submission.id}`} maxLength={500} value={notes[submission.id] ?? ""} onChange={(event) => setNotes((current) => ({ ...current, [submission.id]: event.target.value }))} placeholder="Optional note for the user" />
            </div>
            <div className="flex flex-wrap gap-2">
              <button type="button" className="primary-button min-h-10" onClick={() => void review(submission, "approve")} disabled={busyId === submission.id}><Check size={15} aria-hidden="true" />{busyId === submission.id ? "Saving…" : "Approve and assign tickets"}</button>
              <button type="button" className="secondary-button min-h-10 text-[#8d4545]" onClick={() => void review(submission, "reject")} disabled={busyId === submission.id}><X size={15} aria-hidden="true" />Reject and release tickets</button>
            </div>
          </div>
        ) : (
          <p className="mt-3 rounded-lg bg-surface-soft px-3 py-2 text-xs leading-5 text-muted">{submission.admin_note || "No review note."}</p>
        )}
      </article>
    )
  }

  return (
    <div className="space-y-6">
      <section className="surface-card p-5 sm:p-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div><p className="section-eyebrow">Ticket payment review</p><h2 className="mt-1 text-xl font-bold text-ink-strong">Pending transfers</h2><p className="mt-2 text-sm text-muted">Approving a verified transfer assigns ticket numbers; rejection releases the reserved capacity.</p></div>
          <span className="status-pill status-pill--pending">{pending.length} pending</span>
        </div>
        {message && <p className={`mt-4 rounded-xl px-4 py-3 text-sm ${isError ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role={isError ? "alert" : "status"}>{message}</p>}
        {pending.length ? <div className="mt-5 grid gap-3 xl:grid-cols-2">{pending.map(submissionCard)}</div> : <p className="mt-5 rounded-xl bg-surface-soft p-5 text-sm text-muted">No ticket payments are waiting for review.</p>}
      </section>
      {reviewed.length > 0 && (
        <section className="surface-card p-5 sm:p-6">
          <p className="section-eyebrow">History</p>
          <h2 className="mt-1 text-lg font-bold text-ink-strong">Recently reviewed payments</h2>
          <div className="mt-4 grid gap-3 xl:grid-cols-2">{reviewed.slice(0, 20).map(submissionCard)}</div>
        </section>
      )}
    </div>
  )
}
