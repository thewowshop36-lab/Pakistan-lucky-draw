import Link from "next/link"
import { ArrowDownToLine, ArrowRight, ArrowUpFromLine, Check, CircleHelp, ShieldCheck, TicketCheck } from "lucide-react"
import { DrawCard } from "@/components/draw-card"
import { HeroCarousel } from "@/components/hero-carousel"
import { createClient } from "@/lib/supabase/server"
import { DRAW_COLUMNS } from "@/lib/queries"
import type { Draw, DrawPrize, PaymentAccount, SiteContent } from "@/lib/types"

const trustPoints = [
  { icon: TicketCheck, title: "Clear ticket limits", copy: "See the price and available ticket count before entering." },
  { icon: ShieldCheck, title: "Recorded entries", copy: "Confirmed purchases are attached to your account and draw." },
  { icon: CircleHelp, title: "Public results", copy: "Completed results show winning ticket numbers and masked winner names." },
]

export default async function HomePage() {
  const supabase = await createClient()
  const [drawResult, contentResult, authResult, accountResult] = await Promise.all([
    supabase
      .from("draws")
      .select(DRAW_COLUMNS)
      .eq("status", "active")
      .or("closed_at.is.null,end_condition.eq.sold_out")
      .order("draw_number", { ascending: true })
      .limit(3),
    supabase
      .from("site_content")
      .select("id,section_name,content_text,media_url,display_order,updated_at")
      .order("display_order", { ascending: true })
      .order("updated_at", { ascending: false }),
    supabase.auth.getUser(),
    supabase.from("payment_accounts").select("provider,account_title,account_identifier,instructions,is_active,updated_at").eq("is_active", true).order("provider"),
  ])

  const draws = (drawResult.data ?? []) as unknown as Draw[]
  const content = (contentResult.data ?? []) as unknown as SiteContent[]
  const accounts = (accountResult.data ?? []) as unknown as PaymentAccount[]
  const eligibilityResult = authResult.data.user ? await supabase.rpc("current_user_can_enter_draw") : { data: false }
  const eligible = Boolean(eligibilityResult.data)
  const prizeResult = draws.length ? await supabase.from("draw_prizes_public").select("draw_id,tier_order,prize_name,prize_description,prize_amount,winner_ticket_number,winner_public_name").in("draw_id", draws.map((draw) => draw.id)).order("tier_order") : { data: [] }
  const prizesByDraw = new Map<string, DrawPrize[]>()
  for (const prize of (prizeResult.data ?? []) as unknown as DrawPrize[]) {
    prizesByDraw.set(prize.draw_id, [...(prizesByDraw.get(prize.draw_id) ?? []), prize])
  }
  const heroItems = content.filter((item) => item.section_name === "hero_banner" || item.section_name === "videos")
  const guidelines = content.find((item) => item.section_name === "guidelines")
  const trustText = content.find((item) => item.section_name === "trust")
  const authenticated = Boolean(authResult.data.user)

  return (
    <div className="page-shell py-7 sm:py-10">
      <HeroCarousel items={heroItems} />

      <section className="surface-card mt-5 flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center sm:p-6" aria-labelledby="wallet-actions-heading">
        <div>
          <p className="section-eyebrow">Wallet</p>
          <h2 id="wallet-actions-heading" className="mt-1 text-lg font-bold text-ink-strong">Deposit or withdraw</h2>
          <p className="mt-1 max-w-xl text-sm leading-6 text-muted">JazzCash and Easypaisa requests are reviewed manually. {authenticated ? "Open your profile to manage a request." : "Sign in to submit a request."}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link href="/profile#wallet" className="primary-button"><ArrowDownToLine size={16} aria-hidden="true" />Deposit</Link>
          <Link href="/profile#wallet" className="secondary-button"><ArrowUpFromLine size={16} aria-hidden="true" />Withdraw</Link>
        </div>
      </section>

      <section className="py-12 sm:py-16" aria-labelledby="draws-heading">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="section-eyebrow">Open for entries</p>
            <h2 id="draws-heading" className="section-title mt-2">Active draws</h2>
          </div>
          <Link href="/draws" className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-bold text-green transition hover:bg-green-pale">
            Browse all draws <ArrowRight size={16} aria-hidden="true" />
          </Link>
        </div>
        {draws.length ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {draws.map((draw) => <DrawCard key={draw.id} draw={draw} authenticated={authenticated} eligible={eligible} accounts={accounts} prizes={prizesByDraw.get(draw.id) ?? []} />)}
          </div>
        ) : (
          <div className="surface-card flex flex-col items-start gap-4 p-6 sm:flex-row sm:items-center sm:justify-between sm:p-8">
            <div>
              <span className="status-pill"><span className="size-1.5 rounded-full bg-green" />No draws are open</span>
              <h3 className="mt-4 text-lg font-bold text-ink-strong">Check back for the next draw</h3>
              <p className="mt-1 max-w-xl text-sm leading-6 text-muted">When a draw opens, its ticket price, limit, and terms will appear here.</p>
            </div>
            <Link href="/draws" className="secondary-button shrink-0">View draw list <ArrowRight size={15} aria-hidden="true" /></Link>
          </div>
        )}
      </section>

      <section id="guidelines" className="grid gap-6 border-t border-line py-12 sm:py-16 lg:grid-cols-[0.8fr_1.2fr]" aria-labelledby="guidelines-heading">
        <div>
          <p className="section-eyebrow">Before you enter</p>
          <h2 id="guidelines-heading" className="section-title mt-2">A simple, transparent process.</h2>
          <p className="mt-4 max-w-md text-sm leading-7 text-muted">
            {guidelines?.content_text || "Review each draw’s price, ticket limit, and prize tiers before entering. Ticket numbers appear after a wallet purchase or transfer is confirmed. Completed results include public verification data."}
          </p>
          <Link href="/history" className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-green hover:underline">
            Review past results <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          {trustPoints.map(({ icon: Icon, title, copy }) => (
            <article key={title} className="subtle-card p-5">
              <span className="grid size-10 place-items-center rounded-xl bg-white text-green shadow-sm"><Icon size={19} aria-hidden="true" /></span>
              <h3 className="mt-4 text-sm font-bold text-ink-strong">{title}</h3>
              <p className="mt-2 text-xs leading-5 text-muted">{copy}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="surface-card grid gap-5 p-6 sm:p-8 lg:grid-cols-[1fr_auto] lg:items-center" aria-labelledby="trust-heading">
        <div>
          <p className="section-eyebrow">Trust & verification</p>
          <h2 id="trust-heading" className="mt-2 text-xl font-bold tracking-tight text-ink-strong">Know what happens after you enter.</h2>
          <p className="mt-2 max-w-3xl whitespace-pre-line text-sm leading-6 text-muted">
            {trustText?.content_text || "Ticket purchases are confirmed against the draw’s remaining capacity. Completed results show the winning ticket number and winner name. Keep your account details current and review the published terms for each draw."}
          </p>
        </div>
        <div className="flex items-center gap-2 rounded-xl bg-green-pale px-4 py-3 text-xs font-semibold text-green">
          <Check size={15} aria-hidden="true" /> Results remain available in history
        </div>
      </section>
    </div>
  )
}
