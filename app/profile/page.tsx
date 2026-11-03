import type { Metadata } from "next"
import { AuthGate } from "@/components/auth-gate"
import { ProfileDashboard } from "@/components/profile-dashboard"
import { createClient } from "@/lib/supabase/server"
import type { Draw, Profile, UserTicket, WalletRequest, WalletTransaction } from "@/lib/types"

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

  const [profileResult, ticketsResult, claimsResult, drawsResult, transactionsResult, walletRequestsResult] = await Promise.all([
    supabase.from("profiles").select("id,name,phone,balance,referral_count,free_tickets_earned,created_at").eq("id", user.id).maybeSingle(),
    supabase.from("user_tickets").select("id,ticket_number,status,source,purchase_date,draws(draw_number,title)").eq("user_id", user.id).order("purchase_date", { ascending: false }),
    supabase.from("free_ticket_claims").select("id").eq("user_id", user.id),
    supabase.from("draws").select("id,draw_number,title,description,prize_details,ticket_price,total_tickets,sold_tickets,profit_margin,banner_image_url,status,winner_ticket,winner_user_id,created_at,updated_at,completed_at").eq("status", "active").order("draw_number", { ascending: true }),
    supabase.from("wallet_transactions").select("id,amount,transaction_type,description,created_at").eq("user_id", user.id).order("created_at", { ascending: false }).limit(8),
    supabase.from("wallet_requests").select("id,user_id,request_type,provider,amount,account_phone,transaction_reference,status,admin_note,created_at,updated_at").eq("user_id", user.id).order("created_at", { ascending: false }).limit(20),
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
  const activeDraws = (drawsResult.data ?? []) as unknown as Draw[]
  const transactions = (transactionsResult.data ?? []) as unknown as WalletTransaction[]
  const walletRequests = (walletRequestsResult.data ?? []) as unknown as WalletRequest[]
  const availableFreeTickets = Math.max(0, profile.free_tickets_earned - (claimsResult.data?.length ?? 0))

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-7 max-w-2xl">
        <p className="section-eyebrow">Your account</p>
        <h1 className="section-title mt-2">Welcome, {profile.name}</h1>
        <p className="mt-3 text-sm leading-6 text-muted">Manage your wallet, review entries, and keep track of verified referrals.</p>
      </div>
      <ProfileDashboard
        profile={profile}
        tickets={tickets}
        transactions={transactions}
        activeDraws={activeDraws}
        availableFreeTickets={availableFreeTickets}
        walletRequests={walletRequests}
      />
    </div>
  )
}
