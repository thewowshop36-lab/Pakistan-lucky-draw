"use client"

import { useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { Check, Phone } from "lucide-react"
import { createClient } from "@/lib/supabase/client"

function normalizePhone(value: string) {
  return value.trim().replace(/[\s().-]/g, "")
}

export function PhoneVerification({
  phone,
  verifiedAt,
  emailVerified,
}: {
  phone: string | null
  verifiedAt: string | null
  emailVerified: boolean
}) {
  const [phoneValue, setPhoneValue] = useState(phone ?? "")
  const [code, setCode] = useState("")
  const [codeSent, setCodeSent] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const router = useRouter()
  const supabase = createClient()

  async function sendCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedPhone = normalizePhone(phoneValue)
    if (!/^\+[1-9]\d{6,14}$/.test(normalizedPhone)) {
      setMessage("Enter a valid international phone number, including its country code.")
      setIsError(true)
      return
    }

    setBusy(true)
    setMessage("")
    const { error } = await supabase.auth.updateUser({ phone: normalizedPhone })
    setBusy(false)

    if (error) {
      setMessage("We couldn’t send a verification code. Check the number and try again.")
      setIsError(true)
      return
    }

    setPhoneValue(normalizedPhone)
    setCodeSent(true)
    setMessage("A verification code was sent to your phone.")
    setIsError(false)
  }

  async function verifyCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedPhone = normalizePhone(phoneValue)
    if (!/^\d{6,8}$/.test(code.trim())) {
      setMessage("Enter the verification code from the SMS.")
      setIsError(true)
      return
    }

    setBusy(true)
    setMessage("")
    const { error } = await supabase.auth.verifyOtp({
      phone: normalizedPhone,
      token: code.trim(),
      type: "phone_change",
    })
    setBusy(false)

    if (error) {
      setMessage("That code could not be verified. Check it and try again.")
      setIsError(true)
      return
    }

    setMessage("Phone number verified.")
    setIsError(false)
    router.refresh()
  }

  if (verifiedAt) {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-green-pale px-4 py-3 text-sm font-semibold text-green">
        <Check size={16} aria-hidden="true" />
        Verified phone · {phone || "number on file"}
      </div>
    )
  }

  return (
    <section className="rounded-2xl border border-line bg-surface-soft p-4 sm:p-5" aria-labelledby="phone-verification-heading">
      <div className="flex items-start gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-white text-green"><Phone size={18} aria-hidden="true" /></span>
        <div>
          <h3 id="phone-verification-heading" className="text-sm font-bold text-ink-strong">Verify your phone</h3>
          <p className="mt-1 text-xs leading-5 text-muted">A verified phone is required before you can enter a draw or request a withdrawal.</p>
        </div>
      </div>

      {!emailVerified ? (
        <p className="mt-4 rounded-xl bg-white px-3.5 py-3 text-xs leading-5 text-muted">Confirm your email first. Phone verification will be available here after you sign in again.</p>
      ) : !codeSent ? (
        <form onSubmit={sendCode} className="mt-4 flex flex-col gap-3 sm:flex-row">
          <div className="min-w-0 flex-1">
            <label className="field-label" htmlFor="verified-phone">Phone number</label>
            <input className="field-control" id="verified-phone" type="tel" inputMode="tel" autoComplete="tel" placeholder="+92 300 1234567" value={phoneValue} onChange={(event) => setPhoneValue(event.target.value)} required />
          </div>
          <button type="submit" className="secondary-button self-end" disabled={busy}>{busy ? "Sending…" : "Send code"}</button>
        </form>
      ) : (
        <form onSubmit={verifyCode} className="mt-4 flex flex-col gap-3 sm:flex-row">
          <div className="min-w-0 flex-1">
            <label className="field-label" htmlFor="phone-code">SMS verification code</label>
            <input className="field-control" id="phone-code" inputMode="numeric" autoComplete="one-time-code" maxLength={8} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))} required />
          </div>
          <button type="submit" className="primary-button self-end" disabled={busy}>{busy ? "Verifying…" : "Verify number"}</button>
          <button type="button" className="ghost-button self-end" onClick={() => { setCodeSent(false); setCode(""); setMessage("") }}>Change number</button>
        </form>
      )}

      {message && <p className={`mt-3 rounded-xl px-3.5 py-2.5 text-xs ${isError ? "bg-[#fbefef] text-[#8d4545]" : "bg-white text-green"}`} role={isError ? "alert" : "status"}>{message}</p>}
    </section>
  )
}
