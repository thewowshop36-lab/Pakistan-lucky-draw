import Link from "next/link"
import { MailCheck } from "lucide-react"

export default function SignUpSuccessPage() {
  return (
    <div className="page-shell grid min-h-[70vh] place-items-center py-10">
      <section className="surface-card w-full max-w-lg p-7 text-center sm:p-10">
        <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-green-pale text-green"><MailCheck size={25} aria-hidden="true" /></span>
        <p className="section-eyebrow mt-5">One more step</p>
        <h1 className="mt-2 text-2xl font-bold tracking-tight text-ink-strong">Check your email</h1>
        <p className="mt-3 text-sm leading-6 text-muted">If the address can receive messages, a confirmation link has been sent. Confirm your email before buying tickets or earning referral rewards.</p>
        <Link href="/auth/login" className="primary-button mt-6">Continue to sign in</Link>
      </section>
    </div>
  )
}
