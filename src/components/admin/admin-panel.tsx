"use client"

import { useState } from "react"
import { BarChart3, ImagePlus, Ticket, UsersRound, Wallet } from "lucide-react"
import { formatPkr } from "@/lib/format"
import type { Draw, Profile, SiteContent, UserTicket, WalletRequest } from "@/lib/types"
import { ContentManager } from "@/components/admin/content-manager"
import { DrawManager } from "@/components/admin/draw-manager"
import { UserMonitor } from "@/components/admin/user-monitor"
import { WalletRequestManager } from "@/components/admin/wallet-request-manager"

type AdminTicket = UserTicket & { user_id: string }
type Tab = "overview" | "draws" | "content" | "users" | "wallet"

const tabs: { id: Tab; label: string; icon: typeof BarChart3 }[] = [
  { id: "overview", label: "Overview", icon: BarChart3 },
  { id: "draws", label: "Draws", icon: Ticket },
  { id: "content", label: "Content", icon: ImagePlus },
  { id: "users", label: "Users", icon: UsersRound },
  { id: "wallet", label: "Wallet", icon: Wallet },
]

export function AdminPanel({
  draws,
  profiles,
  tickets,
  content,
  walletRequests,
}: {
  draws: Draw[]
  profiles: Profile[]
  tickets: AdminTicket[]
  content: SiteContent[]
  walletRequests: WalletRequest[]
}) {
  const [activeTab, setActiveTab] = useState<Tab>("overview")
  const activeDraws = draws.filter((draw) => draw.status === "active")
  const completedDraws = draws.filter((draw) => draw.status === "completed")
  const ticketsSold = draws.reduce((sum, draw) => sum + draw.sold_tickets, 0)
  const totalWalletBalance = profiles.reduce((sum, profile) => sum + Number(profile.balance), 0)

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
        <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="surface-card p-5 sm:p-6">
            <p className="section-eyebrow">At a glance</p>
            <h2 className="mt-1 text-xl font-bold text-ink-strong">Platform overview</h2>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="subtle-card p-4"><p className="text-xs font-semibold text-muted">Wallet balances</p><p className="mt-1 text-lg font-extrabold text-ink-strong">{formatPkr(totalWalletBalance)}</p><p className="mt-1 text-xs text-muted">Across {profiles.length} profiles</p></div>
              <div className="subtle-card p-4"><p className="text-xs font-semibold text-muted">Site content items</p><p className="mt-1 text-lg font-extrabold text-ink-strong">{content.length.toLocaleString()}</p><p className="mt-1 text-xs text-muted">Banners, guidelines, trust text, and videos</p></div>
            </div>
          </section>
          <section className="surface-card p-5 sm:p-6">
            <p className="section-eyebrow">Quick actions</p>
            <h2 className="mt-1 text-xl font-bold text-ink-strong">Manage the platform</h2>
            <div className="mt-5 grid gap-3">
              <button type="button" onClick={() => setActiveTab("draws")} className="secondary-button justify-start"><Ticket size={16} aria-hidden="true" /> Create or manage draws</button>
              <button type="button" onClick={() => setActiveTab("content")} className="secondary-button justify-start"><ImagePlus size={16} aria-hidden="true" /> Update homepage content</button>
              <button type="button" onClick={() => setActiveTab("users")} className="secondary-button justify-start"><UsersRound size={16} aria-hidden="true" /> Review users and referrals</button>
              <button type="button" onClick={() => setActiveTab("wallet")} className="secondary-button justify-start"><Wallet size={16} aria-hidden="true" /> Review deposits and withdrawals</button>
            </div>
          </section>
        </div>
      )}
      {activeTab === "draws" && <DrawManager draws={draws} />}
      {activeTab === "content" && <ContentManager content={content} />}
      {activeTab === "users" && <UserMonitor profiles={profiles} tickets={tickets} />}
      {activeTab === "wallet" && <WalletRequestManager requests={walletRequests} profiles={profiles} />}
    </div>
  )
}
