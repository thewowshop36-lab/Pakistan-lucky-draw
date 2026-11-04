"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Pencil, Plus, UploadCloud } from "lucide-react"
import { formatPkr, getProgress } from "@/lib/format"
import { uploadAdminMedia } from "@/lib/admin-media"
import { createClient } from "@/lib/supabase/client"
import type { Draw, DrawFinancialSummary, DrawPrize } from "@/lib/types"
import { FinalizeDrawButton } from "@/components/admin/finalize-draw-button"
import { PrizeTierEditor, type PrizeTierInput } from "@/components/admin/prize-tier-editor"

type DrawForm = {
  id: string
  drawNumber: string
  title: string
  description: string
  prizeDetails: string
  ticketPrice: string
  totalTickets: string
  profitMargin: string
  bannerUrl: string
  endCondition: Draw["end_condition"]
  prizes: PrizeTierInput[]
}

function newDrawForm(draws: Draw[]): DrawForm {
  const nextNumber = Array.from({ length: 50 }, (_, index) => index + 1).find((number) => !draws.some((draw) => draw.draw_number === number)) ?? 1
  return {
    id: "", drawNumber: String(nextNumber), title: "", description: "", prizeDetails: "",
    ticketPrice: "", totalTickets: "100", profitMargin: "0", bannerUrl: "",
    endCondition: "sold_out",
    prizes: [{ prize_name: "Grand prize", prize_description: "", prize_amount: "0" }],
  }
}

function existingDrawForm(draw: Draw, prizes: DrawPrize[]): DrawForm {
  const tiers = prizes.filter((prize) => prize.draw_id === draw.id).sort((left, right) => left.tier_order - right.tier_order)
  return {
    id: draw.id,
    drawNumber: String(draw.draw_number),
    title: draw.title,
    description: draw.description,
    prizeDetails: draw.prize_details,
    ticketPrice: String(draw.ticket_price),
    totalTickets: String(draw.total_tickets),
    profitMargin: String(draw.profit_margin),
    bannerUrl: draw.banner_image_url ?? "",
    endCondition: draw.end_condition,
    prizes: tiers.length ? tiers.map((prize) => ({ prize_name: prize.prize_name, prize_description: prize.prize_description, prize_amount: String(prize.prize_amount) })) : [{ prize_name: "Grand prize", prize_description: "", prize_amount: "0" }],
  }
}

function messageFor(errorMessage: string): string {
  const message = errorMessage.toLowerCase()
  if (message.includes("unique")) return "That draw number is already in use."
  if (message.includes("locked")) return "Draw terms and prizes are locked after the first ticket is sold or reserved."
  if (message.includes("ticket limit") || message.includes("capacity_with_reservations")) return "The ticket limit cannot be lower than tickets already sold or reserved."
  return "The draw could not be saved. Check the details and try again."
}

export function DrawManager({
  draws,
  prizes,
  financials,
}: {
  draws: Draw[]
  prizes: DrawPrize[]
  financials: DrawFinancialSummary[]
}) {
  const [form, setForm] = useState<DrawForm>(() => newDrawForm(draws))
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [notice, setNotice] = useState("")
  const router = useRouter()
  const supabase = createClient()
  const availableNumbers = Array.from({ length: 50 }, (_, index) => index + 1).find((number) => !draws.some((draw) => draw.draw_number === number && draw.id !== form.id))
  const availableNumberList = Array.from({ length: 50 }, (_, index) => index + 1).filter((number) => !draws.some((draw) => draw.draw_number === number && draw.id !== form.id))
  const projectedPool = Number(form.ticketPrice || 0) * Number(form.totalTickets || 0) * (100 - Number(form.profitMargin || 0)) / 100

  function startNewDraw() {
    setForm(newDrawForm(draws))
    setEditing(false)
    setNotice("")
  }

  function startEdit(draw: Draw) {
    setForm(existingDrawForm(draw, prizes))
    setEditing(true)
    setNotice("")
  }

  async function handleBanner(file: File | undefined) {
    if (!file) return
    setUploading(true)
    setNotice("")
    try {
      const url = await uploadAdminMedia(file)
      setForm((current) => ({ ...current, bannerUrl: url }))
      setNotice("Banner uploaded. Save the draw to publish it.")
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Upload failed.")
    } finally {
      setUploading(false)
    }
  }

  async function saveDraw(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setNotice("")
    try {
      const { data: drawId, error } = await supabase.rpc("admin_save_draw", {
        p_draw_id: form.id || null,
        p_draw_number: Number(form.drawNumber),
        p_title: form.title.trim(),
        p_description: form.description.trim(),
        p_prize_details: form.prizeDetails.trim(),
        p_ticket_price: Number(form.ticketPrice),
        p_total_tickets: Number(form.totalTickets),
        p_profit_margin: Number(form.profitMargin),
        p_banner_image_url: form.bannerUrl.trim() || null,
      })
      if (error) {
        setNotice(messageFor(error.message))
        return
      }

      const savedDrawId = typeof drawId === "string" ? drawId : form.id
      if (!savedDrawId) {
        setNotice("The draw was saved, but its identifier could not be confirmed. Refresh and review it.")
        return
      }

      const { error: prizeError } = await supabase.rpc("admin_save_draw_prizes", {
        p_draw_id: savedDrawId,
        p_prizes: form.prizes.map((prize, index) => ({
          tier_order: index + 1,
          prize_name: prize.prize_name.trim(),
          prize_description: prize.prize_description.trim(),
          prize_amount: Number(prize.prize_amount),
        })),
      })
      if (prizeError) {
        setForm((current) => ({ ...current, id: savedDrawId }))
        setEditing(true)
        setNotice(prizeError.message.toLowerCase().includes("prize pool") ? "Prize amounts exceed the maximum pool for this draw." : prizeError.message.toLowerCase().includes("locked") ? "Prize tiers are locked after the first ticket is sold or reserved." : "The draw was saved, but prize tiers were not. Edit the draw and retry.")
        return
      }

      const { error: endConditionError } = await supabase.rpc("admin_set_draw_end_condition", {
        p_draw_id: savedDrawId,
        p_end_condition: form.endCondition,
      })
      if (endConditionError) {
        setForm((current) => ({ ...current, id: savedDrawId }))
        setEditing(true)
        setNotice("The draw and prize tiers were saved, but the closing rule was not. Edit the draw and retry.")
        return
      }

      setNotice(editing ? "Draw, prize tiers, and closing rule updated." : "Draw, prize tiers, and closing rule created.")
      setForm(newDrawForm(draws))
      setEditing(false)
      router.refresh()
    } catch {
      setNotice("The draw could not be saved. Check the connection and try again.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-6">
      <section className="surface-card p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="section-eyebrow">Draw controller</p>
            <h2 className="mt-1 text-xl font-bold text-ink-strong">{editing ? "Edit active draw" : "Create a draw"}</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">Configure prize tiers, the margin-backed prize pool, and whether the draw closes on sellout or administrator action.</p>
          </div>
          {editing && <button type="button" className="secondary-button" onClick={startNewDraw}><Plus size={16} aria-hidden="true" /> New draw</button>}
        </div>
        <form onSubmit={saveDraw} className="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label className="field-label" htmlFor="draw-number">Draw number</label>
            <select className="field-control" id="draw-number" value={form.drawNumber} onChange={(event) => setForm({ ...form, drawNumber: event.target.value })} required>
              {availableNumberList.map((number) => <option key={number} value={number}>Draw {String(number).padStart(2, "0")}</option>)}
            </select>
          </div>
          <div>
            <label className="field-label" htmlFor="draw-title">Title</label>
            <input className="field-control" id="draw-title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={100} required />
          </div>
          <div className="md:col-span-2">
            <label className="field-label" htmlFor="draw-description">Description</label>
            <textarea className="field-control" id="draw-description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} maxLength={4000} />
          </div>
          <div className="md:col-span-2">
            <label className="field-label" htmlFor="draw-prize-details">Prize details</label>
            <textarea className="field-control" id="draw-prize-details" value={form.prizeDetails} onChange={(event) => setForm({ ...form, prizeDetails: event.target.value })} maxLength={2000} placeholder="Describe the prize and any relevant terms." />
          </div>
          <div>
            <label className="field-label" htmlFor="draw-price">Ticket price (PKR)</label>
            <input className="field-control" id="draw-price" type="number" min="0.01" step="0.01" value={form.ticketPrice} onChange={(event) => setForm({ ...form, ticketPrice: event.target.value })} required />
          </div>
          <div>
            <label className="field-label" htmlFor="draw-limit">Ticket limit</label>
            <input className="field-control" id="draw-limit" type="number" min="1" max="1000000" step="1" value={form.totalTickets} onChange={(event) => setForm({ ...form, totalTickets: event.target.value })} required />
          </div>
          <div>
            <label className="field-label" htmlFor="draw-margin">Platform margin (%)</label>
            <input className="field-control" id="draw-margin" type="number" min="0" max="100" step="0.01" value={form.profitMargin} onChange={(event) => setForm({ ...form, profitMargin: event.target.value })} required />
          </div>
          <div>
            <label className="field-label" htmlFor="draw-end-condition">Closing rule</label>
            <select className="field-control" id="draw-end-condition" value={form.endCondition} onChange={(event) => setForm({ ...form, endCondition: event.target.value as Draw["end_condition"] })}>
              <option value="sold_out">Close when all tickets are sold</option>
              <option value="admin_close">Administrator may close early</option>
            </select>
          </div>
          <div className="md:col-span-2 rounded-xl bg-green-pale px-4 py-3 text-xs leading-5 text-green">
            Projected maximum prize pool: <strong>{formatPkr(projectedPool)}</strong>. The result cannot be published if committed cash prizes exceed revenue-backed funds.
          </div>
          <PrizeTierEditor items={form.prizes} onChange={(prizes) => setForm({ ...form, prizes })} />
          <div className="md:col-span-2">
            <label className="field-label" htmlFor="draw-banner-url">Banner URL</label>
            <input className="field-control" id="draw-banner-url" type="url" value={form.bannerUrl} onChange={(event) => setForm({ ...form, bannerUrl: event.target.value })} placeholder="https://…" />
          </div>
          <div className="md:col-span-2">
            <label className="field-label" htmlFor="draw-banner-file">Upload banner</label>
            <div className="flex flex-wrap items-center gap-3">
              <input className="field-control min-w-0 flex-1" id="draw-banner-file" type="file" accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm" onChange={(event) => { void handleBanner(event.target.files?.[0]); event.currentTarget.value = "" }} />
              <span className="inline-flex items-center gap-1.5 text-xs text-muted"><UploadCloud size={15} aria-hidden="true" />{uploading ? "Uploading…" : "15 MB max"}</span>
            </div>
          </div>
          {notice && <p className="md:col-span-2 rounded-xl bg-surface-soft px-4 py-3 text-sm text-ink" role="status">{notice}</p>}
          <div className="flex flex-wrap gap-3 md:col-span-2">
            <button type="submit" className="primary-button" disabled={busy || uploading}>{busy ? "Saving…" : editing ? "Save changes" : "Create draw"}</button>
            {editing && <button type="button" className="secondary-button" onClick={startNewDraw}>Cancel edit</button>}
          </div>
        </form>
      </section>

      <section aria-labelledby="draw-list-heading">
        <div className="mb-4 flex items-end justify-between gap-3">
          <div><p className="section-eyebrow">Numbered 1–50</p><h2 id="draw-list-heading" className="mt-1 text-lg font-bold text-ink-strong">Managed draws <span className="text-sm font-semibold text-muted">({draws.length})</span></h2></div>
          <button type="button" className="secondary-button" onClick={startNewDraw}><Plus size={16} aria-hidden="true" /> Add draw</button>
        </div>
        {draws.length ? (
          <div className="grid gap-3 lg:grid-cols-2">
            {draws.map((draw) => {
              const financial = financials.find((item) => item.draw_id === draw.id)
              const occupied = draw.sold_tickets + draw.reserved_tickets
              const remaining = Math.max(0, draw.total_tickets - occupied)
              return (
                <article key={draw.id} className="surface-card p-4 sm:p-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="flex flex-wrap items-center gap-2"><span className="status-pill">Draw {String(draw.draw_number).padStart(2, "0")}</span><span className={`status-pill ${draw.status === "completed" ? "status-pill--neutral" : draw.closed_at ? "status-pill--pending" : ""}`}>{draw.status === "active" && draw.closed_at ? "Closed · result pending" : draw.status}</span></div>
                      <h3 className="mt-3 text-base font-bold text-ink-strong">{draw.title}</h3>
                      <p className="mt-1 text-sm text-muted">{formatPkr(draw.ticket_price)} / ticket · {draw.total_tickets.toLocaleString()} total</p>
                    </div>
                    {draw.status === "active" && !draw.closed_at && draw.sold_tickets === 0 && draw.reserved_tickets === 0 ? <button type="button" className="secondary-button min-h-9 px-3" onClick={() => startEdit(draw)} aria-label={`Edit ${draw.title}`}><Pencil size={15} aria-hidden="true" /> Edit</button> : draw.status === "active" && !draw.closed_at ? <span className="rounded-lg bg-surface-soft px-3 py-2 text-xs font-semibold text-muted">Terms locked after entries start</span> : null}
                  </div>
                  <div className="mt-4">
                    <div className="mb-2 flex justify-between gap-3 text-xs text-muted"><span>{draw.sold_tickets.toLocaleString()} sold · {draw.reserved_tickets.toLocaleString()} pending</span><span>{remaining.toLocaleString()} available</span></div>
                    <div className="progress-track"><div className="progress-fill" style={{ width: `${getProgress(occupied, draw.total_tickets)}%` }} /></div>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                    <div className="rounded-xl bg-surface-soft p-3"><p className="text-muted">Gross revenue</p><p className="mt-1 font-bold text-ink-strong">{formatPkr(financial?.gross_revenue ?? 0)}</p></div>
                    <div className="rounded-xl bg-surface-soft p-3"><p className="text-muted">Platform profit</p><p className="mt-1 font-bold text-green">{formatPkr(financial?.platform_profit ?? 0)}</p></div>
                    <div className="rounded-xl bg-surface-soft p-3"><p className="text-muted">Prize pool</p><p className="mt-1 font-bold text-ink-strong">{formatPkr(financial?.prize_pool ?? 0)}</p></div>
                    <div className="rounded-xl bg-surface-soft p-3"><p className="text-muted">Cash prizes committed</p><p className="mt-1 font-bold text-ink-strong">{formatPkr(financial?.committed_prizes ?? 0)}</p></div>
                  </div>
                  <p className="mt-3 text-xs text-muted">Closing rule: {draw.end_condition === "sold_out" ? "all tickets sold" : "administrator close"}</p>
                  {draw.status === "active" && draw.sold_tickets > 0 && (draw.end_condition === "admin_close" || draw.sold_tickets === draw.total_tickets) && <FinalizeDrawButton draw={draw} />}
                  {draw.status === "completed" && (
                    <div className="mt-4 rounded-xl bg-green-pale p-3.5">
                      <p className="text-sm font-semibold text-green">Winning ticket: #{draw.winner_ticket}</p>
                      <p className="mt-1 break-all font-mono text-[0.68rem] text-muted">Seed commitment: {draw.server_seed_commitment ?? "—"}</p>
                      <Link href={`/history#draw-${draw.id}`} className="mt-2 inline-flex text-xs font-bold text-green underline">Open public verification</Link>
                    </div>
                  )}
                </article>
              )
            })}
          </div>
        ) : (
          <div className="surface-card p-6 text-sm text-muted">No draws have been created yet.</div>
        )}
      </section>
    </div>
  )
}
