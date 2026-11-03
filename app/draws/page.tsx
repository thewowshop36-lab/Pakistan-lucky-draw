import type { Metadata } from "next"
import { DrawCard } from "@/components/draw-card"
import { createClient } from "@/lib/supabase/server"
import type { Draw } from "@/lib/types"

export const metadata: Metadata = {
  title: "Active draws",
  description: "Browse open draws, check ticket availability, and review the entry price before you participate.",
}

export default async function DrawsPage() {
  const supabase = await createClient()
  const [drawResult, authResult] = await Promise.all([
    supabase
      .from("draws")
      .select("id,draw_number,title,description,prize_details,ticket_price,total_tickets,sold_tickets,profit_margin,banner_image_url,status,winner_ticket,winner_user_id,created_at,updated_at,completed_at")
      .eq("status", "active")
      .order("draw_number", { ascending: true }),
    supabase.auth.getUser(),
  ])
  const draws = (drawResult.data ?? []) as unknown as Draw[]

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-8 max-w-2xl">
        <p className="section-eyebrow">Draws 01–50</p>
        <h1 className="section-title mt-2">Choose an active draw</h1>
        <p className="mt-3 text-sm leading-6 text-muted">Compare entry prices, prize details, and remaining tickets. Purchases are confirmed from your wallet balance.</p>
      </div>
      {draws.length ? (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {draws.map((draw) => <DrawCard key={draw.id} draw={draw} authenticated={Boolean(authResult.data.user)} />)}
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
