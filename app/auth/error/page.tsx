import Link from "next/link"
import { CircleAlert } from "lucide-react"

export default function AuthErrorPage() {
  return (
    <div className="page-shell grid min-h-[70vh] place-items-center py-10">
      <section className="surface-card w-full max-w-lg p-7 text-center sm:p-10">
        <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-[#fbefef] text-[#8d4545]"><CircleAlert size={24} aria-hidden="true" /></span>
        <p className="section-eyebrow mt-5">Authentication</p>
        <h1 className="mt-2 text-2xl font-bold tracking-tight text-ink-strong">That link could not be used</h1>
        <p className="mt-3 text-sm leading-6 text-muted">It may have expired or already been used. Request a new confirmation email or sign in to try again.</p>
        <Link href="/auth/login" className="primary-button mt-6">Go to sign in</Link>
      </section>
    </div>
  )
}
