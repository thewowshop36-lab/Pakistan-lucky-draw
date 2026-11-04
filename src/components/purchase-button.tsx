"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Check, LockKeyhole, Ticket } from "lucide-react"
import { formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"

export function PurchaseButton({
  drawId,
  ticketPrice,
  remaining,
  authenticated,
  eligible,
}: {
  drawId: string
  ticketPrice: number | string
  remaining: number
  authenticated: boolean
  eligible: boolean
}) {
  const [isOpen, setIsOpen] = useState(false)
  const [quantity, setQuantity] = useState(1)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [ticketNumbers, setTicketNumbers] = useState<number[]>([])
  const router = useRouter()
  const supabase = createClient()
  const maxQuantity = Math.min(5, remaining)
  const total = Number(ticketPrice) * quantity

  async function handlePurchase(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (quantity < 1 || quantity > maxQuantity || busy) return
    setBusy(true)
    setMessage("")
    const { data, error } = await supabase.rpc("purchase_tickets", {
      p_draw_id: drawId,
      p_quantity: quantity,
    })
    setBusy(false)

    if (error) {
      const text = error.message.toLowerCase()
      if (text.includes("balance")) setMessage("Your wallet balance is too low. Ask an administrator about wallet credits.")
      else if (text.includes("confirm your email") || text.includes("verification")) setMessage("Complete account verification before entering a draw.")
      else if (text.includes("remaining") || text.includes("not open")) setMessage("Availability changed. Refresh the page and try again.")
      else setMessage("The purchase could not be completed. Please try again.")
      return
    }

    const result = data as { ticket_numbers?: number[] } | null
    const numbers = result?.ticket_numbers ?? []
    setTicketNumbers(numbers)
    setMessage(numbers.length ? `Purchase confirmed. Ticket${numbers.length === 1 ? "" : "s"}: ${numbers.join(", ")}.` : "Purchase confirmed.")
    router.refresh()
  }

  if (!authenticated) {
    return (
      <Link href={`/auth/login?next=${encodeURIComponent("/draws")}`} className="primary-button w-full">
        <LockKeyhole size={16} aria-hidden="true" /> Sign in to buy
      </Link>
    )
  }

  if (!eligible) {
    return <Link href="/profile#eligibility" className="primary-button w-full">Verify account to enter</Link>
  }

  return (
    <>
      <button type="button" className="primary-button w-full" onClick={() => { setMessage(""); setTicketNumbers([]); setIsOpen(true) }} disabled={remaining <= 0}>
        <Ticket size={16} aria-hidden="true" /> {remaining > 0 ? "Buy ticket" : "Sold out"}
      </button>
      {isOpen && (
        <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setIsOpen(false) }}>
          <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby={`purchase-title-${drawId}`}>
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="section-eyebrow">Ticket purchase</p>
                <h2 id={`purchase-title-${drawId}`} className="mt-2 text-xl font-bold tracking-tight text-ink-strong">Confirm your entry</h2>
              </div>
              <button type="button" className="ghost-button min-h-9 px-3" onClick={() => setIsOpen(false)} aria-label="Close purchase dialog">Close</button>
            </div>
            <div className="subtle-card mt-5 flex items-center justify-between gap-3 p-4">
              <span className="text-sm font-medium text-muted">Price per ticket</span>
              <span className="font-bold text-ink-strong">{formatPkr(ticketPrice)}</span>
            </div>
            <form onSubmit={handlePurchase} className="mt-5 space-y-4">
              <div>
                <label htmlFor={`quantity-${drawId}`} className="field-label">Number of tickets</label>
                <input
                  id={`quantity-${drawId}`}
                  className="field-control"
                  type="number"
                  min={1}
                  max={maxQuantity}
                  value={quantity}
                  onChange={(event) => setQuantity(Math.max(1, Math.min(maxQuantity, Number(event.target.value) || 1)))}
                  required
                />
                <p className="mt-1.5 text-xs text-muted">Up to 5 tickets per purchase. {remaining.toLocaleString()} remain.</p>
              </div>
              <div className="flex items-center justify-between border-t border-line pt-4">
                <span className="text-sm font-semibold text-muted">Total</span>
                <span className="text-lg font-extrabold text-ink-strong">{formatPkr(total)}</span>
              </div>
              {message && (
                <p className={`rounded-xl px-3.5 py-3 text-sm leading-5 ${ticketNumbers.length ? "bg-green-pale text-green" : "bg-[#fbefef] text-[#8d4545]"}`} role={ticketNumbers.length ? "status" : "alert"}>
                  {ticketNumbers.length > 0 && <Check className="mr-1 inline" size={16} aria-hidden="true" />}{message}
                </p>
              )}
              <p className="text-xs leading-5 text-muted">Purchases are charged to your wallet balance. See the <Link href="/terms" className="font-semibold text-green underline">draw terms</Link> before confirming.</p>
              <button type="submit" className="primary-button w-full" disabled={busy || remaining <= 0}>
                {busy ? "Confirming…" : ticketNumbers.length ? "Buy more" : "Confirm purchase"}
              </button>
            </form>
          </section>
        </div>
      )}
    </>
  )
}
