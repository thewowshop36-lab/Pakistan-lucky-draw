"use client"

import { useMemo, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { Search, UserRound, Wallet } from "lucide-react"
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

export function UserMonitor({ profiles, tickets }: { profiles: Profile[]; tickets: AdminTicket[] }) {
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
                      <h3 className="truncate font-bold text-ink-strong">{profile.name}</h3>
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
                <p className="mt-3 text-xs text-muted">Joined {formatDate(profile.created_at)}</p>
                <UserWalletAdjustment profile={profile} />
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
