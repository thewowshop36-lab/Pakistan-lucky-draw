import type { Metadata } from "next"

export const metadata: Metadata = {
  title: "Terms of participation",
  description: "Terms for account eligibility, entries, payments, draws, and published results.",
}

export default function TermsPage() {
  return (
    <article className="page-shell max-w-3xl py-10 sm:py-14">
      <p className="section-eyebrow">Participation rules</p>
      <h1 className="section-title mt-2">Terms of participation</h1>
      <p className="mt-3 text-sm text-muted">Effective November 4, 2026 · Version 2026-11-04</p>
      <div className="prose-copy mt-8 space-y-7 text-sm leading-7 text-muted">
        <section><h2>Eligibility</h2><p>You must be at least 18 years old, use a verified email address and phone number, and provide accurate account information. One person may not use multiple accounts or another person&apos;s payment details to gain an unfair advantage.</p></section>
        <section><h2>Draw entries and payments</h2><p>Each draw displays its ticket price, capacity, prize tiers, and closing condition. Wallet purchases are confirmed immediately when accepted. JazzCash and Easypaisa transfers remain pending until an administrator verifies the transfer reference and proof. A ticket number is assigned only after approval. Duplicate, incomplete, or unverifiable transfer references may be rejected.</p><p>Never send or disclose a wallet PIN, password, or one-time passcode. Use only the payment account currently displayed by this site.</p></section>
        <section><h2>Draw selection and results</h2><p>When a draw closes, the winning tickets are selected from the recorded sold-ticket ledger using a precommitted server seed and a public NIST Randomness Beacon pulse generated after closure. Results include the commitment, revealed seed, beacon pulse, algorithm, and prize-tier winners so they can be checked with the public verification tool.</p></section>
        <section><h2>Wallet and prize handling</h2><p>Wallet deposits, withdrawals, manual ticket payments, and cash prize credits are recorded in a transaction ledger. Cash prizes are limited to the available draw prize pool after the disclosed platform margin. Withdrawals and manual transfers may require administrator review.</p></section>
        <section><h2>Account integrity</h2><p>We may pause an account or a draw while reviewing suspected fraud, duplicate transfers, payment disputes, or security issues. We record administrative actions and preserve payment review history. Where possible, we will provide a reason and a way to resolve account verification problems.</p></section>
        <section><h2>Questions and changes</h2><p>Contact the site administrator for support. Material changes to these terms require a new acceptance before you can enter a draw.</p></section>
      </div>
    </article>
  )
}
