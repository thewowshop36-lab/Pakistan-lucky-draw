"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { ArrowRight, KeyRound, Mail, ShieldCheck } from "lucide-react"
import { createClient } from "@/lib/supabase/client"

export function AdminLoginForm() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const router = useRouter()
  const supabase = createClient()

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return

    setBusy(true)
    setMessage("")
    setIsError(false)

    try {
      const { error } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password,
      })

      if (error) {
        const authError = error as { code?: string; status?: number }
        if (authError.code === "email_not_confirmed") {
          setMessage("Confirm your email address, then sign in.")
        } else if (authError.code === "over_request_rate_limit" || authError.status === 429) {
          setMessage("Too many attempts. Wait a little before trying again.")
        } else if (authError.code === "invalid_credentials") {
          setMessage("Invalid email or password.")
        } else {
          setMessage("Admin sign-in is temporarily unavailable. Try again later.")
        }
        setIsError(true)
        return
      }

      router.replace("/admin")
      router.refresh()
    } catch {
      setMessage("Admin sign-in is temporarily unavailable. Try again later.")
      setIsError(true)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="surface-card mx-auto w-full max-w-md p-6 sm:p-8" aria-labelledby="admin-login-heading">
      <div className="grid size-12 place-items-center rounded-2xl bg-green-pale text-green">
        <ShieldCheck size={22} aria-hidden="true" />
      </div>
      <p className="section-eyebrow mt-5">Authorized administrators only</p>
      <h1 id="admin-login-heading" className="mt-2 text-2xl font-bold tracking-tight text-ink-strong">Admin sign-in</h1>
      <p className="mt-2 text-sm leading-6 text-muted">Use the verified email and password for the account authorized to manage this platform.</p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label className="field-label" htmlFor="admin-email">Admin email</label>
          <div className="relative">
            <Mail size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
            <input
              className="field-control pl-10"
              id="admin-email"
              name="email"
              type="email"
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
        </div>
        <div>
          <label className="field-label" htmlFor="admin-password">Password</label>
          <div className="relative">
            <KeyRound size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
            <input
              className="field-control pl-10"
              id="admin-password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>
        </div>
        {message && <p className={`rounded-xl px-3.5 py-3 text-sm leading-5 ${isError ? "bg-[#fbefef] text-[#8d4545]" : "bg-green-pale text-green"}`} role="alert">{message}</p>}
        <button className="primary-button w-full" type="submit" disabled={busy}>
          {busy ? "Please wait…" : "Sign in to admin"}
          {!busy && <ArrowRight size={16} aria-hidden="true" />}
        </button>
      </form>

      <Link href="/" className="mt-5 inline-flex text-sm font-bold text-green hover:underline">Return to public site</Link>
    </section>
  )
}
