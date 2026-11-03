import type { Metadata } from "next"
import { Award, CalendarDays, TicketCheck } from "lucide-react"
import { getWinnerHistory } from "@/lib/queries"
import { formatDate } from "@/lib/format"

export const metadata: Metadata = {
  title: "Draw history",
  description: "Review completed draws, winning ticket numbers, and published winner names.",
}

export default async function HistoryPage() {
  const winners = await getWinnerHistory()

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-8 max-w-2xl">
        <p className="section-eyebrow">Public archive</p>
        <h1 className="section-title mt-2">Recent winners</h1>
        <p className="mt-3 text-sm leading-6 text-muted">Completed results are published here with the winning ticket number and winner name.</p>
      </div>
      {winners.length ? (
        <div className="grid gap-4 md:grid-cols-2">
          {winners.map((winner) => (
            <article key={winner.id} className="surface-card flex items-center gap-4 p-5 sm:p-6">
              <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-gold-soft text-[#7a6128]"><Award size={22} aria-hidden="true" /></span>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="status-pill status-pill--won">Draw {String(winner.draw_number).padStart(2, "0")}</span>
                  <span className="flex items-center gap-1 text-xs text-muted"><CalendarDays size={13} aria-hidden="true" />{formatDate(winner.completed_at)}</span>
                </div>
                <h2 className="mt-2 truncate text-base font-bold text-ink-strong">{winner.title}</h2>
                <p className="mt-1 text-sm text-muted">Winner: <span className="font-semibold text-ink">{winner.winner_name}</span></p>
                {winner.prize_details && <p className="mt-1 line-clamp-2 text-xs text-muted">{winner.prize_details}</p>}
              </div>
              <div className="shrink-0 rounded-xl bg-green-pale px-3 py-2 text-center">
                <TicketCheck size={15} className="mx-auto text-green" aria-hidden="true" />
                <span className="mt-1 block text-sm font-extrabold text-green">#{winner.winner_ticket}</span>
                <span className="text-[0.6rem] font-bold uppercase tracking-wide text-muted">Ticket</span>
              </div>
            </article>
          ))}
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
