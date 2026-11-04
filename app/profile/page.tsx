import type { Metadata } from "next"
import { AuthGate } from "@/components/auth-gate"
import { ProfileDashboard } from "@/components/profile-dashboard"
import { createClient } from "@/lib/supabase/server"
import { DRAW_COLUMNS } from "@/lib/queries"
import type { Draw, PaymentAccount, Profile, TicketPaymentSubmission, UserTicket, WalletRequest, WalletTransaction } from "@/lib/types"

export const metadata: Metadata = {
  title: "My profile",
  description: "Manage your profile, wallet, tickets, and referral rewards.",
}

export default async function ProfilePage() {
  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()

  if (!user) {
    return <div className="page-shell grid min-h-[65vh] place-items-center py-10"><AuthGate /></div>
  }

  const [profileResult, ticketsResult, claimsResult, drawsResult, transactionsResult, walletRequestsResult, ticketPaymentsResult, accountsResult] = await Promise.all([
    supabase.from("profiles").select("id,name,phone,email_verified_at,phone_verified_at,age_confirmed_at,terms_accepted_at,terms_version,account_status,suspended_at,suspension_reason,balance,referral_count,free_tickets_earned,created_at").eq("id", user.id).maybeSingle(),
    supabase.from("user_tickets").select("id,ticket_number,status,source,purchase_date,draws(draw_number,title)").eq("user_id", user.id).order("purchase_date", { ascending: false }),
    supabase.from("free_ticket_claims").select("id").eq("user_id", user.id),
    supabase.from("draws").select(DRAW_COLUMNS).eq("status", "active").is("closed_at", null).order("draw_number", { ascending: true }),
    supabase.from("wallet_transactions").select("id,amount,transaction_type,description,created_at").eq("user_id", user.id).order("created_at", { ascending: false }).limit(8),
    supabase.from("wallet_requests").select("id,user_id,request_type,provider,amount,account_phone,transaction_reference,status,admin_note,created_at,updated_at").eq("user_id", user.id).order("created_at", { ascending: false }).limit(20),
    supabase.from("ticket_payment_submissions").select("id,user_id,draw_id,provider,quantity,ticket_price,amount,transaction_reference,proof_path,status,admin_note,reviewed_at,created_at,draws(draw_number,title)").eq("user_id", user.id).order("created_at", { ascending: false }).limit(20),
    supabase.from("payment_accounts").select("provider,account_title,account_identifier,instructions,is_active,updated_at").eq("is_active", true).order("provider", { ascending: true }),
  ])

  const profile = profileResult.data as unknown as Profile | null
  if (!profile) {
    return (
      <div className="page-shell grid min-h-[65vh] place-items-center py-10">
        <section className="surface-card max-w-xl p-8 text-center">
          <h1 className="text-xl font-bold text-ink-strong">Profile is not ready</h1>
          <p className="mt-2 text-sm leading-6 text-muted">Your account profile could not be loaded. Sign out and sign in again, or contact the site administrator.</p>
        </section>
      </div>
    )
  }

  const tickets = (ticketsResult.data ?? []) as unknown as UserTicket[]
  const activeDraws = ((drawsResult.data ?? []) as unknown as Draw[]).filter((draw) => draw.sold_tickets < draw.total_tickets)
  const transactions = (transactionsResult.data ?? []) as unknown as WalletTransaction[]
  const walletRequests = (walletRequestsResult.data ?? []) as unknown as WalletRequest[]
  const ticketPayments = (ticketPaymentsResult.data ?? []) as unknown as TicketPaymentSubmission[]
  const paymentAccounts = (accountsResult.data ?? []) as unknown as PaymentAccount[]
  const availableFreeTickets = Math.max(0, profile.free_tickets_earned - (claimsResult.data?.length ?? 0))

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-7 max-w-2xl">
        <p className="section-eyebrow">Your account</p>
        <h1 className="section-title mt-2">Welcome, {profile.name}</h1>
        <p className="mt-3 text-sm leading-6 text-muted">Manage your wallet, verify your account, review entries, and track referrals.</p>
        {profile.account_status === "suspended" && <p className="mt-4 rounded-xl bg-[#fbefef] px-4 py-3 text-sm text-[#8d4545]" role="alert">Account access is paused while an administrator reviews it. {profile.suspension_reason ? `Reason: ${profile.suspension_reason}` : "Contact the administrator for details."}</p>}
      </div>
      <ProfileDashboard
        profile={profile}
        tickets={tickets}
        transactions={transactions}
        activeDraws={activeDraws}
        availableFreeTickets={availableFreeTickets}
        walletRequests={walletRequests}
        paymentAccounts={paymentAccounts}
        ticketPayments={ticketPayments}
      />
    </div>
  )
}
