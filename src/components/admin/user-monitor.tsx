"use client"

import { useMemo, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { Search, ShieldAlert, ShieldCheck, UserRound, Wallet } from "lucide-react"
import { formatDate, formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"
import type { Profile, UserTicket } from "@/lib/types"

type AdminTicket = UserTicket & { user_id: string }

function UserWalletAdjustment({ profile }: { profile: Profile }) {
  const [open, setOpen] = useState(false)
  const [kind, setKind] = useState<"credit" | "debit">("credit")
  const [amount, setAmount] = useState("")
  const [reason, setReason] = useState("")
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const router = useRouter()
  const supabase = createClient()

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const numericAmount = Number(amount)
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setMessage("Enter an amount greater than zero.")
      return
    }
    setBusy(true)
    setMessage("")
    const { error } = await supabase.rpc("admin_adjust_wallet", {
      p_user_id: profile.id,
      p_amount: kind === "credit" ? numericAmount : -numericAmount,
      p_description: reason.trim(),
    })
    setBusy(false)

    if (error) {
      setMessage(error.message.toLowerCase().includes("negative") ? "The adjustment would make the wallet negative." : "Wallet adjustment failed. Check the amount and try again.")
      return
    }

    setMessage("Wallet adjusted and recorded in the ledger.")
    setAmount("")
    setReason("")
    router.refresh()
  }

  return (
    <div className="mt-4 border-t border-line pt-4">
      {!open ? (
        <button type="button" className="secondary-button min-h-9 px-3" onClick={() => setOpen(true)}><Wallet size={15} aria-hidden="true" /> Adjust wallet</button>
      ) : (
        <form className="grid gap-3 sm:grid-cols-2" onSubmit={submit}>
          <div>
            <label className="field-label" htmlFor={`wallet-kind-${profile.id}`}>Adjustment</label>
            <select className="field-control" id={`wallet-kind-${profile.id}`} value={kind} onChange={(event) => setKind(event.target.value as "credit" | "debit")}>
              <option value="credit">Credit balance</option>
              <option value="debit">Debit balance</option>
            </select>
          </div>
          <div>
            <label className="field-label" htmlFor={`wallet-amount-${profile.id}`}>Amount (PKR)</label>
            <input className="field-control" id={`wallet-amount-${profile.id}`} type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required />
          </div>
          <div className="sm:col-span-2">
            <label className="field-label" htmlFor={`wallet-reason-${profile.id}`}>Reason</label>
            <input className="field-control" id={`wallet-reason-${profile.id}`} maxLength={240} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Required for audit history" required />
          </div>
          {message && <p className="sm:col-span-2 rounded-lg bg-green-pale px-3 py-2 text-xs text-green" role="status">{message}</p>}
          <div className="flex gap-2 sm:col-span-2">
            <button type="submit" className="primary-button min-h-9 px-3" disabled={busy}>{busy ? "Saving…" : "Record adjustment"}</button>
            <button type="button" className="secondary-button min-h-9 px-3" onClick={() => { setOpen(false); setMessage("") }}>Cancel</button>
          </div>
        </form>
      )}
    </div>
  )
}

function UserStatusControl({ profile, currentUserId }: { profile: Profile; currentUserId: string }) {
  const [reason, setReason] = useState("")
  const [suspensionFormOpen, setSuspensionFormOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const router = useRouter()
  const supabase = createClient()

  async function updateStatus(status: "active" | "suspended") {
    const trimmedReason = reason.trim()
    if (busy) return
    if (status === "suspended" && trimmedReason.length < 3) {
      setMessage("Add a short reason before suspending this account.")
      return
    }

    setBusy(true)
    setMessage("")
    try {
      const { error } = await supabase.rpc("admin_set_user_status", {
        p_user_id: profile.id,
        p_status: status,
        p_reason: trimmedReason,
      })

      if (error) {
        const normalizedError = error.message.toLowerCase()
        if (normalizedError.includes("cannot suspend your own")) {
          setMessage("You cannot suspend your own administrator account.")
        } else if (normalizedError.includes("administrator accounts cannot be suspended")) {
          setMessage("Administrator accounts cannot be suspended here.")
        } else if (normalizedError.includes("administrator access")) {
          setMessage("Administrator access could not be verified. Refresh and sign in again.")
        } else {
          setMessage("Account status was not changed. Try again.")
        }
        return
      }

      setSuspensionFormOpen(false)
      setReason("")
      setMessage(status === "suspended" ? "Account suspended and recorded in the audit log." : "Account reactivated and recorded in the audit log.")
      router.refresh()
    } catch {
      setMessage("Account status could not be updated. Check the connection and try again.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mt-4 border-t border-line pt-4">
      {profile.account_status === "suspended" ? (
        <button type="button" className="secondary-button min-h-9 px-3" disabled={busy} onClick={() => void updateStatus("active")}>
          <ShieldCheck size={15} aria-hidden="true" />{busy ? "Updating…" : "Reactivate account"}
        </button>
      ) : profile.id === currentUserId ? (
        <p className="text-xs leading-5 text-muted">Your administrator account is protected from suspension here.</p>
      ) : suspensionFormOpen ? (
        <form className="grid gap-3" onSubmit={(event) => { event.preventDefault(); void updateStatus("suspended") }}>
          <div>
            <label className="field-label" htmlFor={`suspension-reason-${profile.id}`}>Reason for suspension</label>
            <textarea
              className="field-control"
              id={`suspension-reason-${profile.id}`}
              value={reason}
              onChange={(event) => { setReason(event.target.value); setMessage("") }}
              minLength={3}
              maxLength={500}
              rows={3}
              required
            />
            <p className="mt-1 text-xs text-muted">This reason is saved to the administrator audit log.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="submit" className="danger-button min-h-9 px-3" disabled={busy || reason.trim().length < 3}>
              <ShieldAlert size={15} aria-hidden="true" />{busy ? "Suspending…" : "Confirm suspension"}
            </button>
            <button type="button" className="secondary-button min-h-9 px-3" disabled={busy} onClick={() => { setSuspensionFormOpen(false); setReason(""); setMessage("") }}>Cancel</button>
          </div>
        </form>
      ) : (
        <button type="button" className="danger-button min-h-9 px-3" onClick={() => { setSuspensionFormOpen(true); setMessage("") }}>
          <ShieldAlert size={15} aria-hidden="true" />Suspend account
        </button>
      )}
      {message && <p className="mt-3 rounded-xl bg-surface-soft px-3 py-2 text-xs leading-5 text-muted" role="status">{message}</p>}
    </div>
  )
}

export function UserMonitor({ profiles, tickets, currentUserId }: { profiles: Profile[]; tickets: AdminTicket[]; currentUserId: string }) {
  const [search, setSearch] = useState("")
  const ticketCounts = useMemo(() => {
    const counts = new Map<string, number>()
    tickets.forEach((ticket) => counts.set(ticket.user_id, (counts.get(ticket.user_id) ?? 0) + 1))
    return counts
  }, [tickets])
  const filteredProfiles = profiles.filter((profile) => {
    const query = search.trim().toLowerCase()
    return !query || profile.name.toLowerCase().includes(query) || (profile.phone ?? "").toLowerCase().includes(query) || profile.id.toLowerCase().includes(query)
  })
  const nameById = new Map(profiles.map((profile) => [profile.id, profile.name]))

  return (
    <div className="space-y-6">
      <section className="surface-card p-5 sm:p-6">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div><p className="section-eyebrow">User & referral monitor</p><h2 className="mt-1 text-xl font-bold text-ink-strong">Registered users <span className="text-base font-semibold text-muted">({profiles.length})</span></h2><p className="mt-2 text-sm text-muted">Balances are changed only through audited admin adjustments.</p></div>
          <div className="relative w-full sm:max-w-xs">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
            <input className="field-control pl-9" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, phone, or ID" aria-label="Search users" />
          </div>
        </div>
        {filteredProfiles.length ? (
          <div className="mt-5 grid gap-3 xl:grid-cols-2">
            {filteredProfiles.map((profile) => (
              <article key={profile.id} className="rounded-2xl border border-line bg-white p-4 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-green-pale text-green"><UserRound size={18} aria-hidden="true" /></span>
                    <div className="min-w-0">
                      <div className="flex min-w-0 flex-wrap items-center gap-2">
                        <h3 className="truncate font-bold text-ink-strong">{profile.name}</h3>
                        <span className={`status-pill ${profile.account_status === "suspended" ? "status-pill--lost" : ""}`}>{profile.account_status === "suspended" ? "Suspended" : "Active"}</span>
                      </div>
                      <p className="truncate text-xs text-muted">{profile.phone || "No phone number"} · ID {profile.id.slice(0, 8)}</p>
                    </div>
                  </div>
                  <p className="shrink-0 text-sm font-extrabold text-ink-strong">{formatPkr(profile.balance)}</p>
                </div>
                <div className="mt-4 grid grid-cols-3 gap-2 text-center">
                  <div className="rounded-xl bg-surface-soft px-2 py-2"><p className="text-base font-bold text-ink-strong">{profile.referral_count}</p><p className="text-[0.65rem] text-muted">Referrals</p></div>
                  <div className="rounded-xl bg-surface-soft px-2 py-2"><p className="text-base font-bold text-ink-strong">{profile.free_tickets_earned}</p><p className="text-[0.65rem] text-muted">Free earned</p></div>
                  <div className="rounded-xl bg-surface-soft px-2 py-2"><p className="text-base font-bold text-ink-strong">{ticketCounts.get(profile.id) ?? 0}</p><p className="text-[0.65rem] text-muted">Recent tickets</p></div>
                </div>
                <div className="mt-3 flex flex-wrap gap-1.5" role="group" aria-label="Account verification status">
                  {[
                    { label: "Email", verified: Boolean(profile.email_verified_at) },
                    { label: "Phone", verified: Boolean(profile.phone_verified_at) },
                    { label: "Age", verified: Boolean(profile.age_confirmed_at) },
                    { label: "Terms", verified: Boolean(profile.terms_accepted_at) },
                  ].map(({ label, verified }) => (
                    <span key={label} className={`status-pill ${verified ? "" : "status-pill--neutral"}`}>
                      {label} {verified ? "verified" : "pending"}
                    </span>
                  ))}
                </div>
                {profile.account_status === "suspended" && (
                  <p className="mt-3 rounded-xl bg-surface-soft px-3 py-2 text-xs leading-5 text-muted">
                    <span className="font-semibold text-ink-strong">Suspension reason:</span> {profile.suspension_reason || "No reason recorded"}
                    {profile.suspended_at ? ` · ${formatDate(profile.suspended_at)}` : ""}
                  </p>
                )}
                <p className="mt-3 text-xs text-muted">Joined {formatDate(profile.created_at)}</p>
                <UserWalletAdjustment profile={profile} />
                <UserStatusControl profile={profile} currentUserId={currentUserId} />
              </article>
            ))}
          </div>
        ) : (
          <p className="mt-6 rounded-xl bg-surface-soft p-5 text-sm text-muted">{search ? "No users match this search." : "No registered users yet."}</p>
        )}
      </section>

      <section className="surface-card overflow-hidden" aria-labelledby="recent-tickets-heading">
        <div className="border-b border-line px-5 py-4 sm:px-6"><p className="section-eyebrow">Entries</p><h2 id="recent-tickets-heading" className="mt-1 text-lg font-bold text-ink-strong">Recent ticket activity</h2></div>
        {tickets.length ? (
          <div className="divide-y divide-line">
            {tickets.slice(0, 30).map((ticket) => (
              <div key={ticket.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5 sm:px-6">
                <div><p className="text-sm font-semibold text-ink-strong">{nameById.get(ticket.user_id) ?? "Account"} · #{ticket.ticket_number}</p><p className="mt-1 text-xs text-muted">{ticket.draws?.title ?? "Draw"} · {formatDate(ticket.purchase_date)}</p></div>
                <span className={`status-pill ${ticket.status === "won" ? "status-pill--won" : ticket.status === "lost" ? "status-pill--lost" : ""}`}>{ticket.status} · {ticket.source}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="px-5 py-8 text-sm text-muted sm:px-6">Ticket activity will appear here after users enter a draw.</p>
        )}
      </section>
    </div>
  )
}
