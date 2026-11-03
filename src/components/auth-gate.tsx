import Link from "next/link"
import { CircleUserRound, LockKeyhole } from "lucide-react"

export function AuthGate({ admin = false }: { admin?: boolean }) {
  const nextPath = admin ? "/admin" : "/profile"
  return (
    <section className="surface-card mx-auto max-w-xl p-7 text-center sm:p-10">
      <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-green-pale text-green">
        {admin ? <LockKeyhole size={24} aria-hidden="true" /> : <CircleUserRound size={24} aria-hidden="true" />}
      </span>
      <p className="section-eyebrow mt-5">{admin ? "Admin access" : "Your account"}</p>
      <h1 className="mt-2 text-2xl font-bold tracking-tight text-ink-strong">{admin ? "Sign in to continue" : "Your tickets live here"}</h1>
      <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-muted">
        {admin ? "Administrator tools are restricted to approved accounts." : "Sign in to view your wallet balance, purchased tickets, and referral progress."}
      </p>
      <div className="mt-6 flex flex-wrap justify-center gap-3">
        <Link href={`/auth/login?next=${encodeURIComponent(nextPath)}`} className="primary-button">Sign in</Link>
        {!admin && <Link href="/auth/sign-up" className="secondary-button">Create account</Link>}
      </div>
    </section>
  )
}
