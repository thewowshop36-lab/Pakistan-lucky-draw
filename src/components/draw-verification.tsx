"use client"

import { useState } from "react"
import { Check, ExternalLink, ShieldCheck, TicketCheck } from "lucide-react"
import { formatDate, formatPkr } from "@/lib/format"
import type { DrawPrize, DrawVerification } from "@/lib/types"

function toHex(bytes: Uint8Array) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("")
}

async function sha256(value: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))
  return new Uint8Array(digest)
}

function bytesToBigInt(bytes: Uint8Array) {
  return bytes.reduce((value, byte) => (value << BigInt(8)) + BigInt(byte), BigInt(0))
}

export function DrawVerificationCard({
  draw,
  prizes,
}: {
  draw: DrawVerification
  prizes: DrawPrize[]
}) {
  const [state, setState] = useState<"idle" | "checking" | "verified" | "failed">("idle")
  const hasVerificationEvidence = Boolean(draw.server_seed_commitment && draw.revealed_server_seed && draw.beacon_output_value && draw.beacon_pulse_index && draw.beacon_timestamp && draw.beacon_pulse_uri && draw.winner_ticket !== null && draw.draw_algorithm === "sha256-rejection-v1")

  async function verify() {
    setState("checking")
    try {
      if (!hasVerificationEvidence || !draw.id || !draw.revealed_server_seed || !draw.beacon_output_value || !draw.server_seed_commitment) {
        throw new Error("Required verification data is missing.")
      }

      const commitment = toHex(await sha256(draw.revealed_server_seed))
      if (commitment !== draw.server_seed_commitment.toLowerCase()) throw new Error("The server seed does not match its published commitment.")

      const ticketCount = BigInt(draw.sold_tickets)
      if (ticketCount < BigInt(1) || draw.draw_algorithm !== "sha256-rejection-v1") throw new Error("This draw uses an unsupported verification method.")
      const universe = BigInt(1) << BigInt(256)
      const limit = universe - (universe % ticketCount)
      const selected = new Set<number>()
      const orderedPrizes = [...prizes].sort((left, right) => left.tier_order - right.tier_order)

      if (!orderedPrizes.length) throw new Error("No prize tiers were published.")
      for (const prize of orderedPrizes) {
        let expectedTicket = 0
        for (let counter = 0; counter <= 10000; counter += 1) {
          const input = `${draw.revealed_server_seed}:${draw.beacon_output_value.toLowerCase()}:${draw.id}:${prize.tier_order}:${counter}`
          const value = bytesToBigInt(await sha256(input))
          if (value >= limit) continue
          const candidate = Number((value % ticketCount) + BigInt(1))
          if (!selected.has(candidate)) {
            expectedTicket = candidate
            break
          }
        }
        if (!expectedTicket || expectedTicket !== prize.winner_ticket_number) {
          throw new Error(`Prize tier ${prize.tier_order} does not match the published ticket.`)
        }
        selected.add(expectedTicket)
      }
      if (orderedPrizes[0]?.winner_ticket_number !== draw.winner_ticket) {
        throw new Error("The headline winning ticket does not match the first prize tier.")
      }
      setState("verified")
    } catch {
      setState("failed")
    }
  }

  return (
    <article id={`draw-${draw.id}`} className="surface-card overflow-hidden">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-line px-5 py-5 sm:px-6">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="status-pill status-pill--won">Draw {String(draw.draw_number).padStart(2, "0")}</span>
            <span className="text-xs text-muted">{formatDate(draw.completed_at)}</span>
          </div>
          <h2 className="mt-2 text-lg font-bold text-ink-strong">{draw.title}</h2>
          {draw.prize_details && <p className="mt-1 text-sm leading-5 text-muted">{draw.prize_details}</p>}
        </div>
        <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-green-pale text-green"><TicketCheck size={20} aria-hidden="true" /></span>
      </div>

      <div className="grid gap-4 p-5 sm:grid-cols-2 sm:p-6">
        {prizes.map((prize) => (
          <div key={`${prize.draw_id}-${prize.tier_order}`} className="rounded-xl border border-line bg-white p-4">
            <p className="text-[0.65rem] font-bold uppercase tracking-wide text-muted">Prize {prize.tier_order}</p>
            <p className="mt-1 font-bold text-ink-strong">{prize.prize_name}</p>
            {prize.prize_description && <p className="mt-1 text-xs leading-5 text-muted">{prize.prize_description}</p>}
            <div className="mt-3 flex items-center justify-between gap-3 border-t border-line pt-3">
              <span className="text-xs text-muted">{prize.winner_public_name ?? "Winner"}</span>
              <span className="text-sm font-extrabold text-green">Ticket #{prize.winner_ticket_number}</span>
            </div>
            {Number(prize.prize_amount) > 0 && <p className="mt-1 text-right text-xs text-muted">{formatPkr(prize.prize_amount)} cash award</p>}
          </div>
        ))}
      </div>

      <div className="border-t border-line bg-surface-soft px-5 py-4 sm:px-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-bold text-ink-strong">Public draw verification</p>
            <p className="mt-1 text-xs text-muted">The committed server seed and a post-close NIST pulse determine each winning ticket.</p>
          </div>
          {hasVerificationEvidence ? (
            <button type="button" className="secondary-button min-h-10" onClick={() => void verify()} disabled={state === "checking"}>
              <ShieldCheck size={16} aria-hidden="true" /> {state === "checking" ? "Verifying…" : state === "verified" ? "Verify again" : "Verify result"}
            </button>
          ) : (
            <span className="status-pill status-pill--neutral">Legacy result · no proof published</span>
          )}
        </div>

        {state !== "idle" && (
          <p className={`mt-3 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-xs font-semibold ${state === "verified" ? "bg-green-pale text-green" : state === "failed" ? "bg-[#fbefef] text-[#8d4545]" : "bg-white text-muted"}`} role="status">
            {state === "verified" && <Check size={14} aria-hidden="true" />}
            {state === "verified" ? "Seed commitment and all winning tickets verified in your browser." : state === "failed" ? "Verification failed. The published values do not match the draw algorithm." : "Checking SHA-256 commitment and winner selection…"}
          </p>
        )}

        {hasVerificationEvidence ? (
          <details className="mt-4 rounded-xl border border-line bg-white p-3.5">
            <summary className="cursor-pointer text-xs font-bold text-ink-strong">View seed and beacon evidence</summary>
            <dl className="mt-3 space-y-3 text-xs">
              <div><dt className="font-semibold text-muted">Seed commitment (SHA-256)</dt><dd className="mt-1 break-all font-mono text-ink">{draw.server_seed_commitment}</dd></div>
              <div><dt className="font-semibold text-muted">Revealed server seed</dt><dd className="mt-1 break-all font-mono text-ink">{draw.revealed_server_seed}</dd></div>
              <div><dt className="font-semibold text-muted">NIST pulse</dt><dd className="mt-1 flex flex-wrap items-center gap-1.5 text-ink">#{draw.beacon_pulse_index} · {draw.beacon_timestamp ? formatDate(draw.beacon_timestamp) : "—"}{draw.beacon_pulse_uri && <a href={draw.beacon_pulse_uri} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 font-bold text-green underline">Open source <ExternalLink size={12} aria-hidden="true" /></a>}</dd></div>
              <div><dt className="font-semibold text-muted">Beacon output</dt><dd className="mt-1 break-all font-mono text-ink">{draw.beacon_output_value}</dd></div>
              <div><dt className="font-semibold text-muted">Algorithm</dt><dd className="mt-1 font-mono text-ink">{draw.draw_algorithm}</dd></div>
            </dl>
          </details>
        ) : (
          <p className="mt-4 rounded-xl border border-line bg-white p-3.5 text-xs leading-5 text-muted">This result was recorded before cryptographic draw verification was enabled. It remains in the public archive, but independent verification data was not captured.</p>
        )}
      </div>
    </article>
  )
}
