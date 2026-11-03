"use client"

import { useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { ArrowDownToLine, ArrowUpFromLine, Check, Copy, Wallet } from "lucide-react"
import { formatDate, formatPkr } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"
import type { WalletRequest } from "@/lib/types"

const paymentDestination = "+923134876720"
type RequestMode = "deposit" | "withdrawal"
type PaymentProvider = "jazzcash" | "easypaisa"
type Feedback = { kind: "success" | "error"; text: string }

const statusLabels: Record<WalletRequest["status"], string> = {
  pending: "Awaiting review",
  approved: "Credited",
  paid: "Paid",
  rejected: "Rejected",
}

export function WalletPanel({
  balance,
  phone,
  requests,
}: {
  balance: number | string
  phone: string | null
  requests: WalletRequest[]
}) {
  const [mode, setMode] = useState<RequestMode | null>(null)
  const [provider, setProvider] = useState<PaymentProvider>("jazzcash")
  const [amount, setAmount] = useState("")
  const [accountPhone, setAccountPhone] = useState(phone ?? "")
  const [reference, setReference] = useState("")
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [attempt, setAttempt] = useState<{ fingerprint: string; key: string } | null>(null)
  const router = useRouter()
  const supabase = createClient()

  async function copyPaymentNumber() {
    try {
      await navigator.clipboard.writeText(paymentDestination)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1800)
    } catch {
      setFeedback({ kind: "error", text: "Your browser could not copy the number. Select and copy it manually." })
    }
  }

  async function submitRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!mode || busy) return

    const parsedAmount = Number(amount)
    const digits = accountPhone.replace(/\D/g, "")
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0 || parsedAmount > 1_000_000 || Math.abs(parsedAmount * 100 - Math.round(parsedAmount * 100)) > 0.00001) {
      setFeedback({ kind: "error", text: "Enter an amount from PKR 0.01 to PKR 1,000,000 with up to two decimals." })
      return
    }
    if (digits.length < 7 || digits.length > 15) {
      setFeedback({ kind: "error", text: "Enter a transfer phone number with 7 to 15 digits." })
      return
    }
    if (mode === "deposit" && (reference.trim().length < 4 || reference.trim().length > 120)) {
      setFeedback({ kind: "error", text: "Enter the transfer reference from your receipt." })
      return
    }

    const requestFingerprint = JSON.stringify({
      mode,
      provider,
      amount: amount.trim(),
      accountPhone: accountPhone.trim(),
      reference: mode === "deposit" ? reference.trim() : null,
    })
    const idempotencyKey = attempt?.fingerprint === requestFingerprint ? attempt.key : crypto.randomUUID()
    setAttempt({ fingerprint: requestFingerprint, key: idempotencyKey })
    setBusy(true)
    setFeedback(null)

    try {
      const { error } = await supabase.rpc("create_wallet_request", {
        p_request_type: mode,
        p_provider: provider,
        p_amount: parsedAmount,
        p_account_phone: accountPhone.trim(),
        p_transaction_reference: mode === "deposit" ? reference.trim() : null,
        p_idempotency_key: idempotencyKey,
      })

      if (error) {
        const text = error.code === "23505"
          ? "That transfer reference has already been submitted. Check the reference and try again."
          : error.message.toLowerCase().includes("insufficient")
            ? "Your available wallet balance is too low for this withdrawal."
            : error.message.toLowerCase().includes("confirm your email")
              ? "Confirm your email before submitting a wallet request."
              : "The request could not be submitted. Check the details and try again."
        setFeedback({ kind: "error", text })
        return
      }

      setFeedback({
        kind: "success",
        text: mode === "deposit"
          ? "Deposit request submitted. Your wallet will be credited after the transfer is verified."
          : "Withdrawal request submitted. The amount is reserved until the admin pays or rejects the request.",
      })
      setAmount("")
      setReference("")
      setAttempt(null)
      router.refresh()
    } catch {
      setFeedback({ kind: "error", text: "The request could not be submitted. Please retry the same details." })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section id="wallet" className="surface-card scroll-mt-24 overflow-hidden" aria-labelledby="wallet-actions-heading">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-line px-5 py-4 sm:px-6">
        <div>
          <p className="section-eyebrow">JazzCash & Easypaisa</p>
          <h2 id="wallet-actions-heading" className="mt-1 text-lg font-bold text-ink-strong">Deposit or withdraw</h2>
          <p className="mt-1 max-w-xl text-sm leading-6 text-muted">Requests are reviewed manually. Never share your wallet PIN or one-time password.</p>
        </div>
        <div className="rounded-xl bg-surface-soft px-3.5 py-2.5">
          <p className="text-[0.65rem] font-bold uppercase tracking-wide text-muted">Available balance</p>
          <p className="mt-0.5 text-sm font-extrabold text-ink-strong">{formatPkr(balance)}</p>
        </div>
      </div>

      <div className="p-5 sm:p-6">
        <div className="flex flex-wrap gap-2" role="group" aria-label="Choose a wallet action">
          <button type="button" aria-pressed={mode === "deposit"} onClick={() => { setMode("deposit"); setFeedback(null) }} className={mode === "deposit" ? "primary-button" : "secondary-button"}>
            <ArrowDownToLine size={16} aria-hidden="true" />Deposit
          </button>
          <button type="button" aria-pressed={mode === "withdrawal"} onClick={() => { setMode("withdrawal"); setFeedback(null) }} className={mode === "withdrawal" ? "primary-button" : "secondary-button"}>
            <ArrowUpFromLine size={16} aria-hidden="true" />Withdraw
          </button>
        </div>

        {mode ? (
          <form className="mt-5 space-y-4" onSubmit={(event) => void submitRequest(event)}>
            {mode === "deposit" ? (
              <div className="rounded-2xl border border-line bg-surface-soft p-4">
                <p className="text-xs font-bold uppercase tracking-wide text-muted">Send your deposit to</p>
                <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-bold text-ink-strong">JazzCash / Easypaisa</p>
                    <p className="mt-1 font-mono text-base font-bold tracking-wide text-green">{paymentDestination}</p>
                  </div>
                  <button type="button" className="secondary-button min-h-9 px-3" onClick={() => void copyPaymentNumber()}>
                    {copied ? <Check size={15} aria-hidden="true" /> : <Copy size={15} aria-hidden="true" />}
                    {copied ? "Copied" : "Copy number"}
                  </button>
                </div>
                <p className="mt-3 text-xs leading-5 text-muted">Complete the transfer separately, then enter its reference below. Funds are not credited until an admin verifies the transfer.</p>
              </div>
            ) : (
              <p className="rounded-xl bg-gold-soft px-4 py-3 text-xs leading-5 text-[#765c22]">The requested amount is held from your wallet when submitted. An admin sends the transfer manually; a rejected request returns the amount to your wallet.</p>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="field-label" htmlFor="wallet-provider">Transfer method</label>
                <select id="wallet-provider" className="field-control" value={provider} onChange={(event) => setProvider(event.target.value as PaymentProvider)}>
                  <option value="jazzcash">JazzCash</option>
                  <option value="easypaisa">Easypaisa</option>
                </select>
              </div>
              <div>
                <label className="field-label" htmlFor="wallet-amount">Amount (PKR)</label>
                <input id="wallet-amount" className="field-control" type="number" inputMode="decimal" min="0.01" max="1000000" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required />
              </div>
              <div>
                <label className="field-label" htmlFor="wallet-account-phone">{mode === "deposit" ? "Sender phone number" : "Withdrawal destination number"}</label>
                <input id="wallet-account-phone" className="field-control" type="tel" inputMode="tel" maxLength={24} value={accountPhone} onChange={(event) => setAccountPhone(event.target.value)} required />
              </div>
              {mode === "deposit" && (
                <div>
                  <label className="field-label" htmlFor="wallet-reference">Transfer reference</label>
                  <input id="wallet-reference" className="field-control" autoComplete="off" maxLength={120} value={reference} onChange={(event) => setReference(event.target.value)} required />
                </div>
              )}
            </div>

            {feedback && <p className={`rounded-xl px-4 py-3 text-sm ${feedback.kind === "error" ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role={feedback.kind === "error" ? "alert" : "status"}>{feedback.text}</p>}
            <button type="submit" className="primary-button" disabled={busy}>
              <Wallet size={16} aria-hidden="true" />
              {busy ? "Submitting…" : mode === "deposit" ? "Submit deposit request" : "Submit withdrawal request"}
            </button>
          </form>
        ) : (
          <p className="mt-4 text-sm text-muted">Choose Deposit or Withdraw to open a manual wallet request.</p>
        )}

        <div className="mt-6 border-t border-line pt-5">
          <div className="mb-3 flex items-center justify-between gap-3">
            <h3 className="text-sm font-bold text-ink-strong">Your wallet requests</h3>
            <span className="text-xs text-muted">{requests.length} recent</span>
          </div>
          {requests.length ? (
            <div className="divide-y divide-line">
              {requests.map((request) => (
                <article key={request.id} className="py-3.5 first:pt-0 last:pb-0">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-ink-strong">{request.request_type === "deposit" ? "Deposit" : "Withdrawal"} · {request.provider === "jazzcash" ? "JazzCash" : "Easypaisa"}</span>
                      <span className={`rounded-full px-2.5 py-1 text-[0.65rem] font-bold ${request.status === "rejected" ? "bg-[#fbefef] text-[#8d4545]" : request.status === "pending" ? "bg-gold-soft text-[#765c22]" : "bg-green-pale text-green"}`}>{statusLabels[request.status]}</span>
                    </div>
                    <span className="text-sm font-extrabold text-ink-strong">{formatPkr(request.amount)}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted">{formatDate(request.created_at)} · {request.account_phone}</p>
                  {request.transaction_reference && <p className="mt-1 text-xs text-muted">Reference: {request.transaction_reference}</p>}
                  {request.admin_note && <p className="mt-2 rounded-lg bg-surface-soft px-3 py-2 text-xs leading-5 text-muted">Admin note: {request.admin_note}</p>}
                </article>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted">Deposit and withdrawal requests will appear here.</p>
          )}
        </div>
      </div>
    </section>
  )
}
