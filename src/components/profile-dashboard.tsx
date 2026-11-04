"use client"

import { useEffect, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { Check, Copy, Gift, Pencil, Ticket, Wallet } from "lucide-react"
import { formatDate, formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"
import { WalletPanel } from "@/components/wallet-panel"
import { EligibilityForm } from "@/components/eligibility-form"
import { PhoneVerification } from "@/components/phone-verification"
import type { Draw, PaymentAccount, Profile, TicketPaymentSubmission, UserTicket, WalletRequest, WalletTransaction } from "@/lib/types"

function one<T>(relation: T | T[] | null | undefined): T | null {
  return Array.isArray(relation) ? relation[0] ?? null : relation ?? null
}

export function ProfileDashboard({
  profile,
  tickets,
  transactions,
  activeDraws,
  availableFreeTickets,
  walletRequests,
  paymentAccounts,
  ticketPayments,
}: {
  profile: Profile
  tickets: UserTicket[]
  transactions: WalletTransaction[]
  activeDraws: Draw[]
  availableFreeTickets: number
  walletRequests: WalletRequest[]
  paymentAccounts: PaymentAccount[]
  ticketPayments: TicketPaymentSubmission[]
}) {
  const [name, setName] = useState(profile.name)
  const [referralUrl, setReferralUrl] = useState("")
  const [selectedDraw, setSelectedDraw] = useState(activeDraws[0]?.id ?? "")
  const [saving, setSaving] = useState(false)
  const [claiming, setClaiming] = useState(false)
  const [feedback, setFeedback] = useState("")
  const [feedbackKind, setFeedbackKind] = useState<"success" | "error">("success")
  const [copied, setCopied] = useState(false)
  const router = useRouter()
  const supabase = createClient()
  const referralsTowardNext = profile.referral_count % 10
  const referralsRemaining = 10 - referralsTowardNext

  useEffect(() => {
    setReferralUrl(`${window.location.origin}/auth/sign-up?ref=${encodeURIComponent(profile.id)}`)
  }, [profile.id])

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!name.trim() || name.trim().length > 80) {
      setFeedback("Enter a name between 1 and 80 characters.")
      setFeedbackKind("error")
      return
    }
    setSaving(true)
    setFeedback("")
    const { error } = await supabase
      .from("profiles")
      .update({ name: name.trim() })
      .eq("id", profile.id)
    setSaving(false)

    if (error) {
      setFeedback("Profile details could not be saved. Please try again.")
      setFeedbackKind("error")
      return
    }
    setFeedback("Profile details saved.")
    setFeedbackKind("success")
    router.refresh()
  }

  async function copyReferralLink() {
    if (!referralUrl) return
    try {
      await navigator.clipboard.writeText(referralUrl)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1800)
    } catch {
      setFeedback("Your browser could not copy the link. Select and copy it manually.")
      setFeedbackKind("error")
    }
  }

  async function claimFreeTicket() {
    if (!selectedDraw || claiming || availableFreeTickets <= 0) return
    setClaiming(true)
    setFeedback("")
    const { data, error } = await supabase.rpc("claim_referral_ticket", { p_draw_id: selectedDraw })
    setClaiming(false)

    if (error) {
      const text = error.message.toLowerCase()
      setFeedback(text.includes("remaining") ? "That draw has no tickets left. Choose another active draw." : "The free ticket could not be claimed. Refresh and try again.")
      setFeedbackKind("error")
      return
    }

    const result = data as { ticket_number?: number } | null
    setFeedback(`Free ticket claimed${result?.ticket_number ? `: #${result.ticket_number}` : ""}.`)
    setFeedbackKind("success")
    router.refresh()
  }

  return (
    <div className="space-y-6">
      <section className="grid gap-4 sm:grid-cols-2">
        <article className="surface-card flex items-center gap-4 p-5 sm:p-6">
          <span className="grid size-12 place-items-center rounded-2xl bg-green-pale text-green"><Wallet size={22} aria-hidden="true" /></span>
          <div>
            <p className="text-xs font-semibold text-muted">Wallet balance</p>
            <p className="mt-1 text-2xl font-extrabold tracking-tight text-ink-strong">{formatPkr(profile.balance)}</p>
            <p className="mt-1 text-[0.68rem] leading-4 text-muted">JazzCash and Easypaisa requests are reviewed manually.</p>
          </div>
        </article>
        <article className="surface-card flex items-center gap-4 p-5 sm:p-6">
          <span className="grid size-12 place-items-center rounded-2xl bg-gold-soft text-[#765c22]"><Gift size={22} aria-hidden="true" /></span>
          <div>
            <p className="text-xs font-semibold text-muted">Verified referrals</p>
            <p className="mt-1 text-2xl font-extrabold tracking-tight text-ink-strong">{profile.referral_count.toLocaleString()}</p>
          </div>
        </article>
      </section>

      {feedback && <p className={`rounded-xl px-4 py-3 text-sm ${feedbackKind === "error" ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role={feedbackKind === "error" ? "alert" : "status"}>{feedback}</p>}

      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <section className="surface-card p-5 sm:p-6" aria-labelledby="profile-details-heading">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="section-eyebrow">Account</p>
              <h2 id="profile-details-heading" className="mt-1 text-lg font-bold text-ink-strong">Profile details</h2>
            </div>
            <span className="grid size-9 place-items-center rounded-xl bg-surface-soft text-muted"><Pencil size={16} aria-hidden="true" /></span>
          </div>
          <form onSubmit={saveProfile} className="mt-5 space-y-4">
            <div>
              <label className="field-label" htmlFor="profile-name">Name</label>
              <input className="field-control" id="profile-name" maxLength={80} value={name} onChange={(event) => setName(event.target.value)} required />
            </div>
            <p className="text-xs leading-5 text-muted">Email confirmation: {profile.email_verified_at ? "verified" : "not yet verified"}. Contact details are changed through the verification flow.</p>
            <button type="submit" className="secondary-button w-full" disabled={saving}>{saving ? "Saving…" : "Save name"}</button>
          </form>
        </section>

        <div className="space-y-4">
          <PhoneVerification phone={profile.phone} verifiedAt={profile.phone_verified_at} emailVerified={Boolean(profile.email_verified_at)} />
          <EligibilityForm accepted={Boolean(profile.age_confirmed_at && profile.terms_accepted_at && profile.terms_version === "2026-11-04")} emailVerified={Boolean(profile.email_verified_at)} />
        </div>

        <section className="surface-card p-5 sm:p-6" aria-labelledby="referral-heading">
          <p className="section-eyebrow">Invite & earn</p>
          <h2 id="referral-heading" className="mt-1 text-lg font-bold text-ink-strong">Your referral link</h2>
          <p className="mt-2 text-sm leading-6 text-muted">One free ticket is earned for every 10 referrals who confirm their email.</p>
          <div className="mt-4 flex gap-2">
            <input className="field-control min-w-0 flex-1 text-xs" aria-label="Your referral link" readOnly value={referralUrl || "Preparing referral link…"} onFocus={(event) => event.currentTarget.select()} />
            <button type="button" className="secondary-button shrink-0 px-3" onClick={copyReferralLink} disabled={!referralUrl} aria-label="Copy referral link">
              {copied ? <Check size={17} aria-hidden="true" /> : <Copy size={17} aria-hidden="true" />}
            </button>
          </div>
          <div className="mt-5 flex items-center justify-between gap-3 text-xs">
            <span className="font-semibold text-ink">{referralsTowardNext} / 10 toward your next ticket</span>
            <span className="text-muted">{referralsRemaining} to go</span>
          </div>
          <div className="progress-track mt-2" role="progressbar" aria-label="Referral progress to next free ticket" aria-valuemin={0} aria-valuemax={10} aria-valuenow={referralsTowardNext}>
            <div className="progress-fill" style={{ width: `${referralsTowardNext * 10}%` }} />
          </div>
          <div className="mt-4 flex flex-col gap-3 sm:flex-row">
            {activeDraws.length > 0 && (
              <select className="field-control min-w-0 flex-1" aria-label="Choose a draw for your free ticket" value={selectedDraw} onChange={(event) => setSelectedDraw(event.target.value)}>
                {activeDraws.map((draw) => <option key={draw.id} value={draw.id}>Draw {String(draw.draw_number).padStart(2, "0")} · {draw.title}</option>)}
              </select>
            )}
            <button type="button" className="primary-button shrink-0" onClick={claimFreeTicket} disabled={claiming || availableFreeTickets <= 0 || activeDraws.length === 0 || !profile.phone_verified_at || !profile.age_confirmed_at || profile.terms_version !== "2026-11-04" || profile.account_status !== "active"}>
              <Gift size={16} aria-hidden="true" /> {claiming ? "Claiming…" : availableFreeTickets > 0 ? `Claim free ticket (${availableFreeTickets})` : "Claim free ticket"}
            </button>
          </div>
          {availableFreeTickets <= 0 && <p className="mt-2 text-xs text-muted">Free tickets unlock at 10 verified referrals.</p>}
          {activeDraws.length === 0 && <p className="mt-2 text-xs text-muted">A free ticket can be claimed when an active draw is available.</p>}
        </section>
      </div>

      <WalletPanel balance={profile.balance} phone={profile.phone} requests={walletRequests} accounts={paymentAccounts} />

      {ticketPayments.length > 0 && (
        <section className="surface-card overflow-hidden" aria-labelledby="ticket-payments-heading">
          <div className="border-b border-line px-5 py-4 sm:px-6">
            <p className="section-eyebrow">Manual ticket payments</p>
            <h2 id="ticket-payments-heading" className="mt-1 text-lg font-bold text-ink-strong">Transfer review status</h2>
          </div>
          <div className="divide-y divide-line">
            {ticketPayments.map((payment) => {
              const draw = one(payment.draws)
              return (
                <article key={payment.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 sm:px-6">
                  <div>
                    <p className="font-semibold text-ink-strong">{draw?.title ?? "Draw"} · {payment.quantity} ticket{payment.quantity === 1 ? "" : "s"}</p>
                    <p className="mt-1 text-xs text-muted">{payment.provider === "jazzcash" ? "JazzCash" : "Easypaisa"} · {formatDate(payment.created_at)} · {formatPkr(payment.amount)}</p>
                    {payment.admin_note && <p className="mt-2 text-xs text-muted">Admin note: {payment.admin_note}</p>}
                  </div>
                  <span className={`status-pill ${payment.status === "approved" ? "status-pill--won" : payment.status === "rejected" ? "status-pill--lost" : "status-pill--pending"}`}>{payment.status === "pending" ? "Awaiting review" : payment.status}</span>
                </article>
              )
            })}
          </div>
        </section>
      )}

      <section className="surface-card overflow-hidden" aria-labelledby="tickets-heading">
        <div className="flex items-center justify-between border-b border-line px-5 py-4 sm:px-6">
          <div>
            <p className="section-eyebrow">Your entries</p>
            <h2 id="tickets-heading" className="mt-1 text-lg font-bold text-ink-strong">My tickets</h2>
          </div>
          <span className="status-pill"><Ticket size={13} aria-hidden="true" />{tickets.length}</span>
        </div>
        {tickets.length ? (
          <div className="divide-y divide-line">
            {tickets.map((ticket) => (
              <div key={ticket.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 sm:px-6">
                <div>
                  <p className="font-bold text-ink-strong">{ticket.draws?.title ?? "Draw"}</p>
                  <p className="mt-1 text-xs text-muted">Draw {String(ticket.draws?.draw_number ?? "—").padStart(2, "0")} · Ticket #{ticket.ticket_number} · {ticket.source === "referral" ? "Referral reward" : ticket.source === "manual_payment" ? "Transfer payment" : "Wallet purchase"}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`status-pill ${ticket.status === "won" ? "status-pill--won" : ticket.status === "lost" ? "status-pill--lost" : ""}`}>{ticket.status}</span>
                  <span className="text-xs text-muted">{formatDate(ticket.purchase_date)}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="px-5 py-8 text-sm text-muted sm:px-6">You have not entered a draw yet.</p>
        )}
      </section>

      <section className="surface-card overflow-hidden" aria-labelledby="wallet-heading">
        <div className="border-b border-line px-5 py-4 sm:px-6">
          <p className="section-eyebrow">Wallet</p>
          <h2 id="wallet-heading" className="mt-1 text-lg font-bold text-ink-strong">Recent activity</h2>
        </div>
        {transactions.length ? (
          <div className="divide-y divide-line">
            {transactions.map((transaction) => (
              <div key={transaction.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5 sm:px-6">
                <div>
                  <p className="text-sm font-semibold text-ink">{transaction.description || transaction.transaction_type.replaceAll("_", " ")}</p>
                  <p className="mt-1 text-xs text-muted">{formatDate(transaction.created_at)}</p>
                </div>
                <span className={`text-sm font-extrabold ${Number(transaction.amount) < 0 ? "text-ink-strong" : "text-green"}`}>
                  {Number(transaction.amount) > 0 ? "+" : ""}{formatPkr(transaction.amount)}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p className="px-5 py-8 text-sm text-muted sm:px-6">Wallet activity will appear here.</p>
        )}
      </section>
    </div>
  )
}
