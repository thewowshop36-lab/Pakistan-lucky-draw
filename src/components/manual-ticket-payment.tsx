"use client"

import { useRef, useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { ArrowUpRight, FileCheck2, UploadCloud } from "lucide-react"
import { formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"
import type { PaymentAccount } from "@/lib/types"

const MAX_PROOF_SIZE = 5 * 1024 * 1024
const ALLOWED_PROOF_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "application/pdf"])

type UploadAttempt = {
  fingerprint: string
  idempotencyKey: string
  proofPath: string
  uploaded: boolean
}

function extensionFor(file: File) {
  if (file.type === "image/jpeg") return "jpg"
  if (file.type === "image/png") return "png"
  if (file.type === "image/webp") return "webp"
  return "pdf"
}

export function ManualTicketPayment({
  drawId,
  ticketPrice,
  remaining,
  authenticated,
  eligible,
  accounts,
}: {
  drawId: string
  ticketPrice: number | string
  remaining: number
  authenticated: boolean
  eligible: boolean
  accounts: PaymentAccount[]
}) {
  const [isOpen, setIsOpen] = useState(false)
  const [provider, setProvider] = useState(accounts[0]?.provider ?? "jazzcash")
  const [quantity, setQuantity] = useState(1)
  const [reference, setReference] = useState("")
  const [proof, setProof] = useState<File | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const attemptRef = useRef<UploadAttempt | null>(null)
  const router = useRouter()
  const supabase = createClient()
  const maxQuantity = Math.min(5, remaining)
  const selectedAccount = accounts.find((account) => account.provider === provider) ?? accounts[0]
  const total = Number(ticketPrice) * quantity

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    if (!eligible) {
      setMessage("Complete account verification before requesting draw tickets.")
      setIsError(true)
      return
    }
    if (!proof || !ALLOWED_PROOF_TYPES.has(proof.type) || proof.size > MAX_PROOF_SIZE) {
      setMessage("Upload a JPG, PNG, WebP, or PDF payment proof no larger than 5 MB.")
      setIsError(true)
      return
    }
    if (reference.trim().length < 4 || reference.trim().length > 120) {
      setMessage("Enter the transaction reference shown on your transfer receipt.")
      setIsError(true)
      return
    }
    if (quantity < 1 || quantity > maxQuantity) return

    setBusy(true)
    setMessage("")
    const { data: authData, error: authError } = await supabase.auth.getUser()
    const user = authData.user
    if (authError || !user) {
      setBusy(false)
      setMessage("Sign in again before submitting a payment.")
      setIsError(true)
      return
    }

    const fingerprint = JSON.stringify([drawId, provider, quantity, reference.trim(), proof.name, proof.size, proof.lastModified])
    let attempt = attemptRef.current
    if (attempt?.uploaded && attempt.fingerprint !== fingerprint) {
      await supabase.storage.from("ticket-payment-proofs").remove([attempt.proofPath]).catch(() => undefined)
    }
    if (!attempt || attempt.fingerprint !== fingerprint) {
      attempt = {
        fingerprint,
        idempotencyKey: crypto.randomUUID(),
        proofPath: `${user.id}/${crypto.randomUUID()}.${extensionFor(proof)}`,
        uploaded: false,
      }
      attemptRef.current = attempt
    }

    try {
      if (!attempt.uploaded) {
        const { error: uploadError } = await supabase.storage
          .from("ticket-payment-proofs")
          .upload(attempt.proofPath, proof, { contentType: proof.type, upsert: false })
        if (uploadError) {
          setMessage("The payment proof could not be uploaded. Check the file and try again.")
          setIsError(true)
          return
        }
        attempt.uploaded = true
      }

      const { error } = await supabase.rpc("submit_ticket_payment", {
        p_draw_id: drawId,
        p_provider: provider,
        p_quantity: quantity,
        p_transaction_reference: reference.trim(),
        p_proof_path: attempt.proofPath,
        p_idempotency_key: attempt.idempotencyKey,
      })

      if (error) {
        if (error.code === "23505") {
          await supabase.storage.from("ticket-payment-proofs").remove([attempt.proofPath])
          attemptRef.current = null
          setMessage("That transfer reference has already been submitted. Check the reference and try again.")
        } else if (error.message.toLowerCase().includes("remaining")) {
          setMessage("Ticket availability changed. Refresh and choose a smaller quantity.")
        } else if (error.message.toLowerCase().includes("verification")) {
          setMessage("Complete email, phone, age, and terms verification before entering a draw.")
        } else {
          setMessage("The payment request could not be submitted. Check the details and try again.")
        }
        setIsError(true)
        return
      }

      setMessage("Payment proof received. Your ticket numbers will be assigned after review.")
      setIsError(false)
      setReference("")
      setProof(null)
      attemptRef.current = null
      if (inputRef.current) inputRef.current.value = ""
      router.refresh()
    } catch {
      setMessage("We could not confirm the request. Retry the same details to avoid a duplicate submission.")
      setIsError(true)
    } finally {
      setBusy(false)
    }
  }

  if (!authenticated) {
    return (
      <Link href={`/auth/login?next=${encodeURIComponent("/draws")}`} className="secondary-button w-full">
        <FileCheck2 size={16} aria-hidden="true" /> Sign in to pay by transfer
      </Link>
    )
  }

  if (!eligible) {
    return <Link href="/profile#eligibility" className="secondary-button w-full">Verify account to enter</Link>
  }

  if (!accounts.length) {
    return <p className="rounded-xl bg-surface-soft px-3 py-2.5 text-center text-xs text-muted">Manual transfer payments are not available yet.</p>
  }

  return (
    <>
      <button type="button" className="secondary-button w-full" onClick={() => { setMessage(""); setIsOpen(true) }} disabled={remaining <= 0}>
        <FileCheck2 size={16} aria-hidden="true" /> Pay by transfer
      </button>
      {isOpen && (
        <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) setIsOpen(false) }}>
          <section className="modal-card max-h-[90vh] overflow-y-auto" role="dialog" aria-modal="true" aria-labelledby={`manual-payment-title-${drawId}`}>
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="section-eyebrow">Manual payment</p>
                <h2 id={`manual-payment-title-${drawId}`} className="mt-2 text-xl font-bold tracking-tight text-ink-strong">Reserve draw tickets</h2>
              </div>
              <button type="button" className="ghost-button min-h-9 px-3" onClick={() => setIsOpen(false)} disabled={busy}>Close</button>
            </div>

            <p className="mt-3 text-sm leading-6 text-muted">Transfer the exact amount to the official account, then send the receipt for review. Ticket numbers are assigned only after an administrator confirms the payment.</p>

            <form onSubmit={(event) => void submit(event)} className="mt-5 space-y-4">
              <div>
                <label className="field-label" htmlFor={`manual-provider-${drawId}`}>Transfer method</label>
                <select className="field-control" id={`manual-provider-${drawId}`} value={provider} onChange={(event) => setProvider(event.target.value as PaymentAccount["provider"])}>
                  {accounts.map((account) => <option key={account.provider} value={account.provider}>{account.provider === "jazzcash" ? "JazzCash" : "Easypaisa"}</option>)}
                </select>
              </div>

              {selectedAccount && (
                <div className="subtle-card p-4">
                  <p className="text-[0.65rem] font-bold uppercase tracking-wide text-muted">Official payment account</p>
                  <p className="mt-2 text-sm font-bold text-ink-strong">{selectedAccount.account_title}</p>
                  <p className="mt-1 break-all font-mono text-base font-bold tracking-wide text-green">{selectedAccount.account_identifier}</p>
                  {selectedAccount.instructions && <p className="mt-2 whitespace-pre-line text-xs leading-5 text-muted">{selectedAccount.instructions}</p>}
                </div>
              )}

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="field-label" htmlFor={`manual-quantity-${drawId}`}>Number of tickets</label>
                  <input className="field-control" id={`manual-quantity-${drawId}`} type="number" min={1} max={maxQuantity} value={quantity} onChange={(event) => setQuantity(Math.max(1, Math.min(maxQuantity, Number(event.target.value) || 1)))} required />
                  <p className="mt-1.5 text-xs text-muted">{remaining.toLocaleString()} available · up to 5 per request</p>
                </div>
                <div className="flex items-end justify-between rounded-xl bg-surface-soft px-4 py-3">
                  <span className="text-xs font-semibold text-muted">Total</span>
                  <span className="text-lg font-extrabold text-ink-strong">{formatPkr(total)}</span>
                </div>
              </div>

              <div>
                <label className="field-label" htmlFor={`manual-reference-${drawId}`}>Transfer reference</label>
                <input className="field-control" id={`manual-reference-${drawId}`} autoComplete="off" maxLength={120} value={reference} onChange={(event) => setReference(event.target.value)} required />
              </div>

              <div>
                <label className="field-label" htmlFor={`manual-proof-${drawId}`}>Payment proof</label>
                <input ref={inputRef} className="field-control" id={`manual-proof-${drawId}`} type="file" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={(event) => setProof(event.target.files?.[0] ?? null)} required />
                <p className="mt-1.5 flex items-center gap-1.5 text-xs text-muted"><UploadCloud size={13} aria-hidden="true" />JPG, PNG, WebP, or PDF · 5 MB maximum</p>
              </div>

              {message && <p className={`rounded-xl px-3.5 py-3 text-sm leading-5 ${isError ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role={isError ? "alert" : "status"}>{message}</p>}
              <p className="text-xs leading-5 text-muted">Never share a wallet PIN or one-time password. Reserved tickets remain pending until review.</p>
              <button type="submit" className="primary-button w-full" disabled={busy || remaining <= 0}>
                <ArrowUpRight size={16} aria-hidden="true" /> {busy ? "Submitting…" : "Submit payment proof"}
              </button>
            </form>
          </section>
        </div>
      )}
    </>
  )
}
