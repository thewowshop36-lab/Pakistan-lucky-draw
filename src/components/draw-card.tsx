import { CalendarDays, CircleDollarSign, Ticket, TicketCheck } from "lucide-react"
import { formatPkr, getProgress } from "@/lib/format"
import type { Draw, DrawPrize, PaymentAccount } from "@/lib/types"
import { ManualTicketPayment } from "@/components/manual-ticket-payment"
import { PurchaseButton } from "@/components/purchase-button"
import { SoldOutDrawFinalizer } from "@/components/sold-out-finalizer"

export function DrawCard({
  draw,
  authenticated,
  eligible,
  accounts,
  prizes,
}: {
  draw: Draw
  authenticated: boolean
  eligible: boolean
  accounts: PaymentAccount[]
  prizes: DrawPrize[]
}) {
  const remaining = Math.max(0, draw.total_tickets - draw.sold_tickets - draw.reserved_tickets)
  const occupied = draw.sold_tickets + draw.reserved_tickets
  const progress = getProgress(occupied, draw.total_tickets)
  const soldOut = draw.sold_tickets === draw.total_tickets && draw.reserved_tickets === 0

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
        {prizes.length > 0 && (
          <div className="mt-4 rounded-xl bg-surface-soft p-3.5">
            <p className="text-[0.65rem] font-bold uppercase tracking-wide text-muted">Prize tiers</p>
            <ul className="mt-2 space-y-2">
              {prizes.slice(0, 3).map((prize) => (
                <li key={`${prize.draw_id}-${prize.tier_order}`} className="flex items-center justify-between gap-3 text-xs">
                  <span className="min-w-0 truncate font-semibold text-ink-strong">{prize.tier_order}. {prize.prize_name}</span>
                  {Number(prize.prize_amount) > 0 && <span className="shrink-0 font-bold text-green">{formatPkr(prize.prize_amount)}</span>}
                </li>
              ))}
              {prizes.length > 3 && <li className="text-[0.68rem] text-muted">+{prizes.length - 3} more prize tiers</li>}
            </ul>
          </div>
        )}
        <div className="mt-5">
          <div className="mb-2 flex items-center justify-between gap-2 text-xs">
            <span className="font-semibold text-ink">{remaining.toLocaleString()} tickets available</span>
            <span className="text-right text-muted">{draw.sold_tickets.toLocaleString()} sold{draw.reserved_tickets > 0 ? ` · ${draw.reserved_tickets.toLocaleString()} pending` : ` / ${draw.total_tickets.toLocaleString()}`}</span>
          </div>
          <div className="progress-track" role="progressbar" aria-label={`${draw.title} tickets sold or reserved`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
        <div className="mt-auto pt-5">
          {!soldOut && remaining > 0 ? (
            <div className="grid gap-2 sm:grid-cols-2">
              <PurchaseButton drawId={draw.id} ticketPrice={draw.ticket_price} remaining={remaining} authenticated={authenticated} eligible={eligible} />
              <ManualTicketPayment drawId={draw.id} ticketPrice={draw.ticket_price} remaining={remaining} authenticated={authenticated} eligible={eligible} accounts={accounts} />
            </div>
          ) : draw.reserved_tickets > 0 ? (
            <p className="rounded-xl bg-gold-soft px-3.5 py-3 text-center text-xs font-semibold text-[#765c22]">All remaining tickets are reserved while payments are reviewed.</p>
          ) : (
            <p className="rounded-xl bg-surface-soft px-3.5 py-3 text-center text-xs font-semibold text-muted">All tickets are sold.</p>
          )}
          {soldOut && draw.end_condition === "sold_out" && <SoldOutDrawFinalizer drawId={draw.id} />}
          <p className="mt-3 flex items-center justify-center gap-1.5 text-[0.68rem] text-muted">
            {remaining > 0 ? <Ticket size={13} aria-hidden="true" /> : <CalendarDays size={13} aria-hidden="true" />}
            {remaining > 0 ? "Ticket numbers are assigned after confirmation" : "Results are published with public verification"}
          </p>
        </div>
      </div>
    </article>
  )
}
