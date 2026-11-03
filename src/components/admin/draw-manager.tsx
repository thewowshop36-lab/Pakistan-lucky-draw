"use client"

import { useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { Pencil, Plus, Trophy, UploadCloud } from "lucide-react"
import { formatPkr, getProgress } from "@/lib/format"
import { uploadAdminMedia } from "@/lib/admin-media"
import { createClient } from "@/lib/supabase/client"
import type { Draw } from "@/lib/types"

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
}

function newDrawForm(draws: Draw[]): DrawForm {
  const nextNumber = Array.from({ length: 50 }, (_, index) => index + 1).find((number) => !draws.some((draw) => draw.draw_number === number)) ?? 1
  return { id: "", drawNumber: String(nextNumber), title: "", description: "", prizeDetails: "", ticketPrice: "", totalTickets: "100", profitMargin: "0", bannerUrl: "" }
}

function existingDrawForm(draw: Draw): DrawForm {
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
  }
}

function messageFor(errorMessage: string): string {
  if (errorMessage.toLowerCase().includes("unique")) return "That draw number is already in use."
  if (errorMessage.toLowerCase().includes("ticket limit")) return "The ticket limit cannot be lower than tickets already sold."
  return "The draw could not be saved. Check the details and try again."
}

function WinnerDialog({ draw, onClose, onSaved }: { draw: Draw; onClose: () => void; onSaved: () => void }) {
  const [mode, setMode] = useState<"random" | "manual">("random")
  const [ticketNumber, setTicketNumber] = useState("")
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const supabase = createClient()

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setMessage("")
    const { data, error } = await supabase.rpc("admin_complete_draw", {
      p_draw_id: draw.id,
      p_winner_ticket: mode === "manual" ? Number(ticketNumber) : null,
    })
    setBusy(false)

    if (error) {
      setMessage(error.message.toLowerCase().includes("sold ticket") ? "Enter a ticket number that was sold in this draw." : "This draw could not be completed. Check the ticket sales and try again.")
      return
    }

    const result = data as { winner_ticket?: number } | null
    setMessage(`Draw completed. Winning ticket: #${result?.winner_ticket ?? "—"}.`)
    onSaved()
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="winner-dialog-title">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="section-eyebrow">Draw {String(draw.draw_number).padStart(2, "0")}</p>
            <h3 id="winner-dialog-title" className="mt-1 text-xl font-bold text-ink-strong">Select a winner</h3>
          </div>
          <button type="button" className="ghost-button min-h-9 px-3" onClick={onClose}>Close</button>
        </div>
        <p className="mt-3 text-sm leading-6 text-muted">The result will be published to the public history and all tickets will be marked won or lost.</p>
        <form onSubmit={submit} className="mt-5 space-y-4">
          <div className="grid grid-cols-2 gap-2 rounded-xl bg-surface-soft p-1">
            <button type="button" className={`rounded-lg px-3 py-2 text-sm font-bold ${mode === "random" ? "bg-white text-green shadow-sm" : "text-muted"}`} aria-pressed={mode === "random"} onClick={() => setMode("random")}>Random ticket</button>
            <button type="button" className={`rounded-lg px-3 py-2 text-sm font-bold ${mode === "manual" ? "bg-white text-green shadow-sm" : "text-muted"}`} aria-pressed={mode === "manual"} onClick={() => setMode("manual")}>Manual ticket</button>
          </div>
          {mode === "random" ? (
            <p className="rounded-xl bg-green-pale px-4 py-3 text-sm leading-5 text-green">The database will select one sold ticket using server-side randomness.</p>
          ) : (
            <div>
              <label htmlFor="winner-ticket-number" className="field-label">Winning ticket number</label>
              <input id="winner-ticket-number" className="field-control" type="number" min={1} max={draw.sold_tickets} value={ticketNumber} onChange={(event) => setTicketNumber(event.target.value)} required />
              <p className="mt-1.5 text-xs text-muted">Only a ticket already sold in this draw can be selected.</p>
            </div>
          )}
          {message && <p className="rounded-xl bg-green-pale px-4 py-3 text-sm text-green" role="status">{message}</p>}
          <button type="submit" className="primary-button w-full" disabled={busy || draw.sold_tickets === 0}>{busy ? "Publishing result…" : "Complete draw"}</button>
        </form>
      </section>
    </div>
  )
}

export function DrawManager({ draws }: { draws: Draw[] }) {
  const [form, setForm] = useState<DrawForm>(() => newDrawForm(draws))
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [notice, setNotice] = useState("")
  const [winnerTarget, setWinnerTarget] = useState<Draw | null>(null)
  const router = useRouter()
  const supabase = createClient()
  const availableNumbers = Array.from({ length: 50 }, (_, index) => index + 1).filter((number) => !draws.some((draw) => draw.draw_number === number && draw.id !== form.id))

  function startNewDraw() {
    setForm(newDrawForm(draws))
    setEditing(false)
    setNotice("")
  }

  function startEdit(draw: Draw) {
    setForm(existingDrawForm(draw))
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
    setBusy(true)
    setNotice("")
    const { error } = await supabase.rpc("admin_save_draw", {
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
    setBusy(false)

    if (error) {
      setNotice(messageFor(error.message))
      return
    }

    setNotice(editing ? "Draw updated." : "Draw created.")
    setForm(newDrawForm(draws))
    setEditing(false)
    router.refresh()
  }

  return (
    <div className="space-y-6">
      <section className="surface-card p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="section-eyebrow">Draw controller</p>
            <h2 className="mt-1 text-xl font-bold text-ink-strong">{editing ? "Edit active draw" : "Create a draw"}</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">Each draw number can be used once. Draws become read-only after publishing a result.</p>
          </div>
          {editing && <button type="button" className="secondary-button" onClick={startNewDraw}><Plus size={16} aria-hidden="true" /> New draw</button>}
        </div>
        <form onSubmit={saveDraw} className="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label className="field-label" htmlFor="draw-number">Draw number</label>
            <select className="field-control" id="draw-number" value={form.drawNumber} onChange={(event) => setForm({ ...form, drawNumber: event.target.value })} required>
              {availableNumbers.map((number) => <option key={number} value={number}>Draw {String(number).padStart(2, "0")}</option>)}
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
            <label className="field-label" htmlFor="draw-margin">Profit margin (%)</label>
            <input className="field-control" id="draw-margin" type="number" min="0" max="100" step="0.01" value={form.profitMargin} onChange={(event) => setForm({ ...form, profitMargin: event.target.value })} required />
          </div>
          <div>
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
          {notice && <p className="md:col-span-2 rounded-xl bg-green-pale px-4 py-3 text-sm text-green" role="status">{notice}</p>}
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
            {draws.map((draw) => (
              <article key={draw.id} className="surface-card p-4 sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex flex-wrap items-center gap-2"><span className="status-pill">Draw {String(draw.draw_number).padStart(2, "0")}</span><span className={`status-pill ${draw.status === "completed" ? "status-pill--neutral" : ""}`}>{draw.status}</span></div>
                    <h3 className="mt-3 text-base font-bold text-ink-strong">{draw.title}</h3>
                    <p className="mt-1 text-sm text-muted">{formatPkr(draw.ticket_price)} / ticket · {draw.total_tickets.toLocaleString()} total</p>
                  </div>
                  <div className="flex gap-2">
                    {draw.status === "active" && <button type="button" className="secondary-button min-h-9 px-3" onClick={() => startEdit(draw)} aria-label={`Edit ${draw.title}`}><Pencil size={15} aria-hidden="true" /> Edit</button>}
                    {draw.status === "active" && <button type="button" className="secondary-button min-h-9 px-3" onClick={() => setWinnerTarget(draw)}><Trophy size={15} aria-hidden="true" /> Winner</button>}
                  </div>
                </div>
                <div className="mt-4">
                  <div className="mb-2 flex justify-between text-xs text-muted"><span>{draw.sold_tickets.toLocaleString()} sold</span><span>{Math.max(0, draw.total_tickets - draw.sold_tickets).toLocaleString()} remaining</span></div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: `${getProgress(draw.sold_tickets, draw.total_tickets)}%` }} /></div>
                </div>
                {draw.status === "completed" && <p className="mt-3 text-sm font-semibold text-green">Winning ticket: #{draw.winner_ticket}</p>}
              </article>
            ))}
          </div>
        ) : (
          <div className="surface-card p-6 text-sm text-muted">No draws have been created yet.</div>
        )}
      </section>
      {winnerTarget && <WinnerDialog draw={winnerTarget} onClose={() => setWinnerTarget(null)} onSaved={() => router.refresh()} />}
    </div>
  )
}
