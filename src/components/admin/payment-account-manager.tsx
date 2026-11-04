"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Check, Save } from "lucide-react"
import { createClient } from "@/lib/supabase/client"
import type { PaymentAccount } from "@/lib/types"

const providers: PaymentAccount["provider"][] = ["jazzcash", "easypaisa"]

type AccountDraft = {
  account_title: string
  account_identifier: string
  instructions: string
  is_active: boolean
}

function AccountEditor({ account, provider }: { account?: PaymentAccount; provider: PaymentAccount["provider"] }) {
  const [draft, setDraft] = useState<AccountDraft>({
    account_title: account?.account_title ?? "",
    account_identifier: account?.account_identifier ?? "",
    instructions: account?.instructions ?? "",
    is_active: account?.is_active ?? true,
  })
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const router = useRouter()
  const supabase = createClient()

  useEffect(() => {
    setDraft({
      account_title: account?.account_title ?? "",
      account_identifier: account?.account_identifier ?? "",
      instructions: account?.instructions ?? "",
      is_active: account?.is_active ?? true,
    })
  }, [account])

  async function save() {
    if (busy) return
    setBusy(true)
    setMessage("")
    const { error } = await supabase.rpc("admin_save_payment_account", {
      p_provider: provider,
      p_account_title: draft.account_title.trim(),
      p_account_identifier: draft.account_identifier.trim(),
      p_instructions: draft.instructions.trim(),
      p_is_active: draft.is_active,
    })
    setBusy(false)

    if (error) {
      setMessage("The payment account could not be saved. Check the account details.")
      setIsError(true)
      return
    }
    setMessage("Payment account saved.")
    setIsError(false)
    router.refresh()
  }

  const label = provider === "jazzcash" ? "JazzCash" : "Easypaisa"

  return (
    <article className="rounded-2xl border border-line bg-white p-4 sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="section-eyebrow">{label}</p>
          <h3 className="mt-1 text-base font-bold text-ink-strong">Official transfer account</h3>
        </div>
        <label className="inline-flex items-center gap-2 text-xs font-semibold text-muted">
          <input className="size-4 accent-[#246345]" type="checkbox" checked={draft.is_active} onChange={(event) => setDraft({ ...draft, is_active: event.target.checked })} />
          Show to users
        </label>
      </div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div>
          <label className="field-label" htmlFor={`${provider}-title`}>Account title</label>
          <input className="field-control" id={`${provider}-title`} maxLength={80} value={draft.account_title} onChange={(event) => setDraft({ ...draft, account_title: event.target.value })} required />
        </div>
        <div>
          <label className="field-label" htmlFor={`${provider}-identifier`}>Phone or account number</label>
          <input className="field-control" id={`${provider}-identifier`} maxLength={48} value={draft.account_identifier} onChange={(event) => setDraft({ ...draft, account_identifier: event.target.value })} required />
        </div>
        <div className="sm:col-span-2">
          <label className="field-label" htmlFor={`${provider}-instructions`}>Transfer instructions</label>
          <textarea className="field-control min-h-20" id={`${provider}-instructions`} maxLength={500} value={draft.instructions} onChange={(event) => setDraft({ ...draft, instructions: event.target.value })} placeholder="Optional instructions shown to customers" />
        </div>
      </div>
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
        {message ? <p className={`text-xs ${isError ? "text-[#8d4545]" : "text-green"}`} role={isError ? "alert" : "status"}>{message}</p> : <p className="text-xs text-muted">Changes are recorded in the admin audit log.</p>}
        <button type="button" className="primary-button min-h-10" onClick={() => void save()} disabled={busy}>
          {message && !isError ? <Check size={15} aria-hidden="true" /> : <Save size={15} aria-hidden="true" />}{busy ? "Saving…" : "Save account"}
        </button>
      </div>
    </article>
  )
}

export function PaymentAccountManager({ accounts }: { accounts: PaymentAccount[] }) {
  return (
    <section className="surface-card p-5 sm:p-6" aria-labelledby="payment-account-heading">
      <p className="section-eyebrow">Payment setup</p>
      <h2 id="payment-account-heading" className="mt-1 text-xl font-bold text-ink-strong">Official payment accounts</h2>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">These public account details are used for wallet deposits and ticket payments. Confirm the account owner before enabling a method.</p>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        {providers.map((provider) => <AccountEditor key={provider} provider={provider} account={accounts.find((account) => account.provider === provider)} />)}
      </div>
    </section>
  )
}
