"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Check, ShieldCheck } from "lucide-react"
import { createClient } from "@/lib/supabase/client"
import { CURRENT_TERMS_VERSION } from "@/lib/terms"

function maximumEligibleBirthDate() {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 18)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

export function EligibilityForm({
  accepted,
  emailVerified,
}: {
  accepted: boolean
  emailVerified: boolean
}) {
  const [birthDate, setBirthDate] = useState("")
  const [termsAccepted, setTermsAccepted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const router = useRouter()
  const supabase = createClient()

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!emailVerified) {
      setMessage("Confirm your email before completing eligibility.")
      setIsError(true)
      return
    }
    if (!birthDate || birthDate > maximumEligibleBirthDate()) {
      setMessage("You must be at least 18 years old to enter a draw.")
      setIsError(true)
      return
    }
    if (!termsAccepted) {
      setMessage("Accept the current terms and privacy policy to continue.")
      setIsError(true)
      return
    }

    setBusy(true)
    setMessage("")
    const { error } = await supabase.rpc("complete_user_eligibility", {
      p_birth_date: birthDate,
      p_terms_version: CURRENT_TERMS_VERSION,
    })
    setBusy(false)

    if (error) {
      const text = error.message.toLowerCase()
      setMessage(text.includes("18") ? "You must be at least 18 years old to enter a draw." : "Eligibility could not be saved. Review the terms and try again.")
      setIsError(true)
      return
    }

    setMessage("Eligibility details saved. Verify your phone to finish account setup.")
    setIsError(false)
    router.refresh()
  }

  if (accepted) {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-green-pale px-4 py-3 text-sm font-semibold text-green">
        <Check size={16} aria-hidden="true" />
        Age and current terms confirmed
      </div>
    )
  }

  return (
    <section id="eligibility" className="rounded-2xl border border-line bg-surface-soft p-4 sm:p-5" aria-labelledby="eligibility-heading">
      <div className="flex items-start gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-white text-green"><ShieldCheck size={18} aria-hidden="true" /></span>
        <div>
          <h3 id="eligibility-heading" className="text-sm font-bold text-ink-strong">Confirm your eligibility</h3>
          <p className="mt-1 text-xs leading-5 text-muted">Draw entries are for adults 18 and over. We keep the confirmation timestamp, not your date of birth.</p>
        </div>
      </div>
      <form onSubmit={submit} className="mt-4 space-y-4">
        <div>
          <label className="field-label" htmlFor="birth-date">Date of birth</label>
          <input className="field-control" id="birth-date" type="date" max={maximumEligibleBirthDate()} value={birthDate} onChange={(event) => setBirthDate(event.target.value)} required />
        </div>
        <label className="flex items-start gap-3 text-xs leading-5 text-muted">
          <input className="mt-1 size-4 shrink-0 accent-[#246345]" type="checkbox" checked={termsAccepted} onChange={(event) => setTermsAccepted(event.target.checked)} required />
          <span>I confirm I am at least 18 and agree to the <Link className="font-bold text-green underline" href="/terms" target="_blank">Terms</Link> and <Link className="font-bold text-green underline" href="/privacy" target="_blank">Privacy Policy</Link>.</span>
        </label>
        {!emailVerified && <p className="text-xs text-[#765c22]">Email confirmation is required before eligibility can be saved.</p>}
        {message && <p className={`rounded-xl px-3.5 py-2.5 text-xs ${isError ? "bg-[#fbefef] text-[#8d4545]" : "bg-white text-green"}`} role={isError ? "alert" : "status"}>{message}</p>}
        <button type="submit" className="secondary-button" disabled={busy || !emailVerified}>{busy ? "Saving…" : "Save eligibility"}</button>
      </form>
    </section>
  )
}
