import type { Metadata } from "next"

export const metadata: Metadata = {
  title: "Privacy policy",
  description: "How account, payment, verification, and draw data is used and protected.",
}

export default function PrivacyPage() {
  return (
    <article className="page-shell max-w-3xl py-10 sm:py-14">
      <p className="section-eyebrow">Your information</p>
      <h1 className="section-title mt-2">Privacy policy</h1>
      <p className="mt-3 text-sm text-muted">Effective November 4, 2026 · Version 2026-11-04</p>
      <div className="prose-copy mt-8 space-y-7 text-sm leading-7 text-muted">
        <section><h2>Information we use</h2><p>We use your name, email address, phone number and verification status to operate your account, prevent duplicate entries, and provide support. The date of birth is checked to confirm the minimum age; the date itself is not retained by the application.</p></section>
        <section><h2>Payments and proof</h2><p>For manual transfers, we retain the provider, amount, transaction reference, review status, and receipt file. Receipt files are stored privately and can be accessed only by the submitting account and authorized administrators. Do not upload a receipt that shows a PIN, password, or one-time passcode.</p></section>
        <section><h2>Draw records</h2><p>Ticket assignments, wallet ledger entries, prize awards, and published verification data are retained to operate and audit draws. Public results show ticket numbers and a masked winner name; account identifiers and private payment proof are not included in public results.</p></section>
        <section><h2>Security and retention</h2><p>Access is restricted by account and administrator permissions. Administrative changes are logged. We retain records for operational, dispute-resolution, and audit purposes and remove or anonymize personal data when it is no longer needed, subject to applicable requirements.</p></section>
        <section><h2>Your choices</h2><p>Keep your contact information current and contact the site administrator to request help with your account or personal information. Some records may need to remain available to preserve a financial ledger or completed draw audit.</p></section>
      </div>
    </article>
  )
}
