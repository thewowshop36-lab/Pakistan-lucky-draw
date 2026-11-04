import type { Metadata } from "next"
import { Award } from "lucide-react"
import { DrawVerificationCard } from "@/components/draw-verification"
import { getDrawVerifications, getPublicDrawPrizes } from "@/lib/queries"

export const metadata: Metadata = {
  title: "Draw history",
  description: "Review completed draws, winning ticket numbers, and published winner names.",
}

export default async function HistoryPage() {
  const draws = await getDrawVerifications()
  const prizes = await getPublicDrawPrizes(draws.map((draw) => draw.id))
  const prizesByDraw = new Map<string, typeof prizes>()
  for (const prize of prizes) {
    prizesByDraw.set(prize.draw_id, [...(prizesByDraw.get(prize.draw_id) ?? []), prize])
  }

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-8 max-w-2xl">
        <p className="section-eyebrow">Public archive</p>
        <h1 className="section-title mt-2">Recent winners</h1>
        <p className="mt-3 text-sm leading-6 text-muted">New draws include masked winners, prize tiers, a committed server seed, and a public randomness pulse. Older archived results are labeled when proof is unavailable.</p>
      </div>
      {draws.length ? (
        <div className="space-y-4">
          {draws.map((draw) => <DrawVerificationCard key={draw.id} draw={draw} prizes={prizesByDraw.get(draw.id) ?? []} />)}
        </div>
      ) : (
        <div className="surface-card grid min-h-64 place-items-center p-8 text-center">
          <div className="max-w-md">
            <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-gold-soft text-[#7a6128]"><Award size={24} aria-hidden="true" /></span>
            <h2 className="mt-4 text-lg font-bold text-ink-strong">No completed draws yet</h2>
            <p className="mt-2 text-sm leading-6 text-muted">When a draw is completed, its published result will appear in this archive.</p>
          </div>
        </div>
      )}
    </div>
  )
}
