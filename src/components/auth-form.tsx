"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { ArrowRight, KeyRound, Mail, UserRound } from "lucide-react"
import { safeInternalPath } from "@/lib/format"
import { createClient } from "@/lib/supabase/client"

export function AuthForm({
  mode,
  nextPath,
  referralId,
}: {
  mode: "login" | "signup"
  nextPath?: string
  referralId?: string
}) {
  const [name, setName] = useState("")
  const [phone, setPhone] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const router = useRouter()
  const supabase = createClient()
  const isSignup = mode === "signup"

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    setMessage("")
    setIsError(false)

    if (isSignup) {
      const phoneDigits = phone.replace(/\D/g, "")
      if (!name.trim() || name.trim().length > 80) {
        setMessage("Enter a name between 1 and 80 characters.")
        setIsError(true)
        return
      }
      if (phoneDigits.length < 7 || phoneDigits.length > 15) {
        setMessage("Enter a phone number with 7 to 15 digits.")
        setIsError(true)
        return
      }
      if (password.length < 8) {
        setMessage("Use a password with at least 8 characters.")
        setIsError(true)
        return
      }

      setBusy(true)
      const { error } = await supabase.auth.signUp({
        email: email.trim(),
        password,
        options: {
          emailRedirectTo: process.env.NEXT_PUBLIC_DEV_SUPABASE_REDIRECT_URL || `${window.location.origin}/auth/callback`,
          data: {
            name: name.trim(),
            phone: phone.trim(),
            referred_by: referralId || null,
          },
        },
      })
      setBusy(false)

      if (error) {
        const code = String((error as { code?: string }).code ?? "")
        if (code === "weak_password") setMessage("Choose a stronger password and try again.")
        else if (code === "over_email_send_rate_limit") setMessage("Too many attempts. Wait a little before requesting another email.")
        else setMessage("We couldn’t create an account with those details. Check the form and try again.")
        setIsError(true)
        return
      }

      router.push("/auth/sign-up-success")
      router.refresh()
      return
    }

    setBusy(true)
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password })
    setBusy(false)

    if (error) {
      const code = String((error as { code?: string }).code ?? "")
      if (code === "email_not_confirmed") setMessage("Confirm your email address, then sign in.")
      else if (code === "over_request_rate_limit") setMessage("Too many attempts. Wait a little before trying again.")
      else setMessage("Invalid email or password.")
      setIsError(true)
      return
    }

    router.replace(safeInternalPath(nextPath))
    router.refresh()
  }

  return (
    <div className="surface-card mx-auto w-full max-w-md p-6 sm:p-8">
      <div className="grid size-12 place-items-center rounded-2xl bg-green-pale text-green">
        {isSignup ? <UserRound size={22} aria-hidden="true" /> : <KeyRound size={22} aria-hidden="true" />}
      </div>
      <p className="section-eyebrow mt-5">{isSignup ? "Create your account" : "Welcome back"}</p>
      <h1 className="mt-2 text-2xl font-bold tracking-tight text-ink-strong">{isSignup ? "Join Lucky Draw" : "Sign in"}</h1>
      <p className="mt-2 text-sm leading-6 text-muted">{isSignup ? "Use a verified email to manage your tickets and referrals." : "Access your wallet, tickets, and referral progress."}</p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        {isSignup && (
          <>
            <div>
              <label className="field-label" htmlFor="name">Full name</label>
              <input className="field-control" id="name" name="name" autoComplete="name" maxLength={80} value={name} onChange={(event) => setName(event.target.value)} required />
            </div>
            <div>
              <label className="field-label" htmlFor="phone">Phone number</label>
              <input className="field-control" id="phone" name="phone" type="tel" autoComplete="tel" inputMode="tel" placeholder="+92 300 1234567" value={phone} onChange={(event) => setPhone(event.target.value)} required />
            </div>
          </>
        )}
        <div>
          <label className="field-label" htmlFor="email">Email address</label>
          <div className="relative">
            <Mail size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
            <input className="field-control pl-10" id="email" name="email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </div>
        </div>
        <div>
          <label className="field-label" htmlFor="password">Password</label>
          <input className="field-control" id="password" name="password" type="password" autoComplete={isSignup ? "new-password" : "current-password"} minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} required />
          {isSignup && <p className="mt-1.5 text-xs text-muted">At least 8 characters.</p>}
        </div>
        {message && <p className={`rounded-xl px-3.5 py-3 text-sm leading-5 ${isError ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role={isError ? "alert" : "status"}>{message}</p>}
        <button className="primary-button w-full" type="submit" disabled={busy}>
          {busy ? "Please wait…" : isSignup ? "Create account" : "Sign in"}
          {!busy && <ArrowRight size={16} aria-hidden="true" />}
        </button>
      </form>

      <p className="mt-5 text-center text-sm text-muted">
        {isSignup ? "Already registered?" : "New to Lucky Draw?"}{" "}
        <Link className="font-bold text-green hover:underline" href={isSignup ? "/auth/login" : `/auth/sign-up${referralId ? `?ref=${encodeURIComponent(referralId)}` : ""}`}>
          {isSignup ? "Sign in" : "Create an account"}
        </Link>
      </p>
    </div>
  )
}
