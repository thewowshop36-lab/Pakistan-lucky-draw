import type { Metadata } from "next"
import { DrawCard } from "@/components/draw-card"
import { createClient } from "@/lib/supabase/server"
import { DRAW_COLUMNS } from "@/lib/queries"
import type { Draw, DrawPrize, PaymentAccount } from "@/lib/types"

export const metadata: Metadata = {
  title: "Active draws",
  description: "Browse open draws, check ticket availability, and review the entry price before you participate.",
}

export default async function DrawsPage() {
  const supabase = await createClient()
  const [drawResult, authResult, accountResult] = await Promise.all([
    supabase
      .from("draws")
      .select(DRAW_COLUMNS)
      .eq("status", "active")
      .or("closed_at.is.null,end_condition.eq.sold_out")
      .order("draw_number", { ascending: true }),
    supabase.auth.getUser(),
    supabase.from("payment_accounts").select("provider,account_title,account_identifier,instructions,is_active,updated_at").eq("is_active", true).order("provider"),
  ])
  const draws = (drawResult.data ?? []) as unknown as Draw[]
  const accounts = (accountResult.data ?? []) as unknown as PaymentAccount[]
  const eligibilityResult = authResult.data.user ? await supabase.rpc("current_user_can_enter_draw") : { data: false }
  const eligible = Boolean(eligibilityResult.data)
  const prizeResult = draws.length ? await supabase.from("draw_prizes_public").select("draw_id,tier_order,prize_name,prize_description,prize_amount,winner_ticket_number,winner_public_name").in("draw_id", draws.map((draw) => draw.id)).order("tier_order") : { data: [] }
  const prizesByDraw = new Map<string, DrawPrize[]>()
  for (const prize of (prizeResult.data ?? []) as unknown as DrawPrize[]) {
    prizesByDraw.set(prize.draw_id, [...(prizesByDraw.get(prize.draw_id) ?? []), prize])
  }

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-8 max-w-2xl">
        <p className="section-eyebrow">Draws 01–50</p>
        <h1 className="section-title mt-2">Choose an active draw</h1>
        <p className="mt-3 text-sm leading-6 text-muted">Compare entry prices, prize tiers, and available tickets. Enter from your wallet or submit a JazzCash or Easypaisa transfer for review.</p>
      </div>
      {draws.length ? (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {draws.map((draw) => <DrawCard key={draw.id} draw={draw} authenticated={Boolean(authResult.data.user)} eligible={eligible} accounts={accounts} prizes={prizesByDraw.get(draw.id) ?? []} />)}
        </div>
      ) : (
        <div className="surface-card grid min-h-64 place-items-center p-8 text-center">
          <div className="max-w-md">
            <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-green-pale text-green"><span className="text-xl font-bold">—</span></span>
            <h2 className="mt-4 text-lg font-bold text-ink-strong">No active draws yet</h2>
            <p className="mt-2 text-sm leading-6 text-muted">New draws will appear here after they are published by an administrator.</p>
          </div>
        </div>
      )}
    </div>
  )
}
