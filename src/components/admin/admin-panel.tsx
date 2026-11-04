"use client"

import { useState } from "react"
import { BarChart3, CreditCard, ImagePlus, Ticket, UsersRound, Wallet } from "lucide-react"
import { formatDate, formatPkr } from "@/lib/format"
import type { AdminAuditLog, Draw, DrawFinancialSummary, DrawPrize, PaymentAccount, Profile, SiteContent, TicketPaymentSubmission, UserTicket, WalletRequest } from "@/lib/types"
import { ContentManager } from "@/components/admin/content-manager"
import { DrawManager } from "@/components/admin/draw-manager"
import { PaymentAccountManager } from "@/components/admin/payment-account-manager"
import { TicketPaymentManager } from "@/components/admin/ticket-payment-manager"
import { UserMonitor } from "@/components/admin/user-monitor"
import { WalletRequestManager } from "@/components/admin/wallet-request-manager"

type AdminTicket = UserTicket & { user_id: string }
type Tab = "overview" | "draws" | "payments" | "content" | "users" | "wallet"

const tabs: { id: Tab; label: string; icon: typeof BarChart3 }[] = [
  { id: "overview", label: "Overview", icon: BarChart3 },
  { id: "draws", label: "Draws", icon: Ticket },
  { id: "payments", label: "Payments", icon: CreditCard },
  { id: "content", label: "Content", icon: ImagePlus },
  { id: "users", label: "Users", icon: UsersRound },
  { id: "wallet", label: "Wallet", icon: Wallet },
]

export function AdminPanel({
  draws,
  profiles,
  currentUserId,
  tickets,
  content,
  walletRequests,
  ticketPayments,
  paymentAccounts,
  prizes,
  financials,
  auditLogs,
}: {
  draws: Draw[]
  profiles: Profile[]
  currentUserId: string
  tickets: AdminTicket[]
  content: SiteContent[]
  walletRequests: WalletRequest[]
  ticketPayments: TicketPaymentSubmission[]
  paymentAccounts: PaymentAccount[]
  prizes: DrawPrize[]
  financials: DrawFinancialSummary[]
  auditLogs: AdminAuditLog[]
}) {
  const [activeTab, setActiveTab] = useState<Tab>("overview")
  const activeDraws = draws.filter((draw) => draw.status === "active")
  const completedDraws = draws.filter((draw) => draw.status === "completed")
  const ticketsSold = draws.reduce((sum, draw) => sum + draw.sold_tickets, 0)
  const totalWalletBalance = profiles.reduce((sum, profile) => sum + Number(profile.balance), 0)
  const grossRevenue = financials.reduce((sum, draw) => sum + Number(draw.gross_revenue), 0)
  const platformProfit = financials.reduce((sum, draw) => sum + Number(draw.platform_profit), 0)

  return (
    <div className="space-y-6">
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {[
          { label: "Active draws", value: activeDraws.length.toLocaleString(), icon: Ticket },
          { label: "Completed draws", value: completedDraws.length.toLocaleString(), icon: BarChart3 },
          { label: "Registered users", value: profiles.length.toLocaleString(), icon: UsersRound },
          { label: "Tickets sold", value: ticketsSold.toLocaleString(), icon: ImagePlus },
        ].map(({ label, value, icon: Icon }) => (
          <article key={label} className="surface-card flex items-center gap-3 p-4 sm:p-5">
            <span className="grid size-10 place-items-center rounded-xl bg-green-pale text-green"><Icon size={18} aria-hidden="true" /></span>
            <div><p className="text-xs font-semibold text-muted">{label}</p><p className="mt-0.5 text-xl font-extrabold text-ink-strong">{value}</p></div>
          </article>
        ))}
      </section>

      <nav className="flex gap-1 overflow-x-auto rounded-2xl border border-line bg-white p-1.5" aria-label="Admin sections">
        {tabs.map(({ id, label, icon: Icon }) => (
          <button key={id} type="button" onClick={() => setActiveTab(id)} aria-current={activeTab === id ? "page" : undefined} className={`inline-flex min-h-11 shrink-0 items-center gap-2 rounded-xl px-4 text-sm font-bold transition ${activeTab === id ? "bg-green text-white shadow-sm" : "text-muted hover:bg-surface-soft hover:text-ink"}`}>
            <Icon size={16} aria-hidden="true" />{label}
          </button>
        ))}
      </nav>

      {activeTab === "overview" && (
        <div className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
            <section className="surface-card p-5 sm:p-6">
              <p className="section-eyebrow">At a glance</p>
              <h2 className="mt-1 text-xl font-bold text-ink-strong">Platform overview</h2>
              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <div className="subtle-card p-4"><p className="text-xs font-semibold text-muted">Wallet balances</p><p className="mt-1 text-lg font-extrabold text-ink-strong">{formatPkr(totalWalletBalance)}</p><p className="mt-1 text-xs text-muted">Across {profiles.length} profiles</p></div>
                <div className="subtle-card p-4"><p className="text-xs font-semibold text-muted">Gross ticket revenue</p><p className="mt-1 text-lg font-extrabold text-ink-strong">{formatPkr(grossRevenue)}</p><p className="mt-1 text-xs text-muted">Recorded ticket sales</p></div>
                <div className="subtle-card p-4"><p className="text-xs font-semibold text-muted">Platform profit</p><p className="mt-1 text-lg font-extrabold text-green">{formatPkr(platformProfit)}</p><p className="mt-1 text-xs text-muted">Based on each draw&apos;s configured margin</p></div>
                <div className="subtle-card p-4"><p className="text-xs font-semibold text-muted">Site content items</p><p className="mt-1 text-lg font-extrabold text-ink-strong">{content.length.toLocaleString()}</p><p className="mt-1 text-xs text-muted">Banners, guidelines, trust text, and videos</p></div>
              </div>
            </section>
            <section className="surface-card p-5 sm:p-6">
              <p className="section-eyebrow">Quick actions</p>
              <h2 className="mt-1 text-xl font-bold text-ink-strong">Manage the platform</h2>
              <div className="mt-5 grid gap-3">
                <button type="button" onClick={() => setActiveTab("draws")} className="secondary-button justify-start"><Ticket size={16} aria-hidden="true" /> Create or manage draws</button>
                <button type="button" onClick={() => setActiveTab("payments")} className="secondary-button justify-start"><CreditCard size={16} aria-hidden="true" /> Review ticket payments</button>
                <button type="button" onClick={() => setActiveTab("content")} className="secondary-button justify-start"><ImagePlus size={16} aria-hidden="true" /> Update homepage content</button>
                <button type="button" onClick={() => setActiveTab("users")} className="secondary-button justify-start"><UsersRound size={16} aria-hidden="true" /> Review users and referrals</button>
                <button type="button" onClick={() => setActiveTab("wallet")} className="secondary-button justify-start"><Wallet size={16} aria-hidden="true" /> Review deposits and withdrawals</button>
              </div>
            </section>
          </div>

          <section className="surface-card overflow-hidden" aria-labelledby="audit-heading">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line px-5 py-4 sm:px-6">
              <div><p className="section-eyebrow">Immutable activity trail</p><h2 id="audit-heading" className="mt-1 text-lg font-bold text-ink-strong">Recent administrative and ledger events</h2></div>
              <span className="text-xs text-muted">{auditLogs.length} recent events</span>
            </div>
            {auditLogs.length ? (
              <div className="divide-y divide-line">
                {auditLogs.slice(0, 12).map((entry) => (
                  <div key={entry.id} className="flex flex-wrap items-center justify-between gap-2 px-5 py-3.5 sm:px-6">
                    <div className="min-w-0"><p className="text-sm font-semibold text-ink-strong">{entry.action.replaceAll("_", " ")}</p><p className="mt-1 text-xs text-muted">{entry.actor_name ?? "System"} · {entry.entity_type}{entry.entity_id ? ` · ${entry.entity_id.slice(0, 8)}` : ""}</p></div>
                    <span className="text-xs text-muted">{formatDate(entry.created_at)}</span>
                  </div>
                ))}
              </div>
            ) : <p className="px-5 py-6 text-sm text-muted sm:px-6">Administrative and financial events will appear here.</p>}
          </section>
        </div>
      )}
      {activeTab === "draws" && <DrawManager draws={draws} prizes={prizes} financials={financials} />}
      {activeTab === "payments" && <div className="space-y-6"><TicketPaymentManager submissions={ticketPayments} /><PaymentAccountManager accounts={paymentAccounts} /></div>}
      {activeTab === "content" && <ContentManager content={content} />}
      {activeTab === "users" && <UserMonitor profiles={profiles} tickets={tickets} currentUserId={currentUserId} />}
      {activeTab === "wallet" && <WalletRequestManager requests={walletRequests} profiles={profiles} />}
    </div>
  )
}
