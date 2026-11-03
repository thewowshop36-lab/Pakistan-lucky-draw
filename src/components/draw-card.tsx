import { CalendarDays, CircleDollarSign, Ticket, TicketCheck } from "lucide-react"
import { formatPkr, getProgress } from "@/lib/format"
import type { Draw } from "@/lib/types"
import { PurchaseButton } from "@/components/purchase-button"

export function DrawCard({ draw, authenticated }: { draw: Draw; authenticated: boolean }) {
  const remaining = Math.max(0, draw.total_tickets - draw.sold_tickets)
  const progress = getProgress(draw.sold_tickets, draw.total_tickets)

  return (
    <article className="surface-card flex h-full flex-col overflow-hidden">
      <div className="relative h-44 overflow-hidden bg-[#dbe8dc]">
        {draw.banner_image_url ? (
          <img src={draw.banner_image_url} alt={`${draw.title} banner`} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center bg-[radial-gradient(circle_at_70%_20%,#e1c77c_0,transparent_25%),linear-gradient(135deg,#173f30,#2d7853)]">
            <TicketCheck size={54} className="text-white/85" strokeWidth={1.4} aria-hidden="true" />
          </div>
        )}
        <span className="absolute left-4 top-4 rounded-full bg-white/95 px-3 py-1.5 text-[0.65rem] font-extrabold uppercase tracking-[0.12em] text-green shadow-sm">
          Draw {String(draw.draw_number).padStart(2, "0")}
        </span>
      </div>
      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="section-eyebrow">{formatPkr(draw.ticket_price)} / entry</p>
            <h2 className="mt-2 text-lg font-bold leading-tight tracking-tight text-ink-strong">{draw.title}</h2>
          </div>
          <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-gold-soft text-[#79622d]" aria-hidden="true">
            <CircleDollarSign size={19} />
          </span>
        </div>
        {draw.prize_details && <p className="mt-3 line-clamp-2 text-sm leading-6 text-muted">{draw.prize_details}</p>}
        {draw.description && <p className="mt-2 line-clamp-2 text-xs leading-5 text-muted">{draw.description}</p>}
        <div className="mt-5">
          <div className="mb-2 flex items-center justify-between gap-2 text-xs">
            <span className="font-semibold text-ink">{remaining.toLocaleString()} tickets left</span>
            <span className="text-muted">{draw.sold_tickets.toLocaleString()} / {draw.total_tickets.toLocaleString()} sold</span>
          </div>
          <div className="progress-track" role="progressbar" aria-label={`${draw.title} tickets sold`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
        <div className="mt-auto pt-5">
          <PurchaseButton drawId={draw.id} ticketPrice={draw.ticket_price} remaining={remaining} authenticated={authenticated} />
          <p className="mt-3 flex items-center justify-center gap-1.5 text-[0.68rem] text-muted">
            {remaining > 0 ? <Ticket size={13} aria-hidden="true" /> : <CalendarDays size={13} aria-hidden="true" />}
            {remaining > 0 ? "Ticket number assigned after confirmation" : "No tickets remain"}
          </p>
        </div>
      </div>
    </article>
  )
}
