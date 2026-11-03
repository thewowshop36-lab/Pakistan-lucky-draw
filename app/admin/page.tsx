import type { Metadata } from "next"
import Link from "next/link"
import { redirect } from "next/navigation"
import { AdminPanel } from "@/components/admin/admin-panel"
import { AdminSignOutButton } from "@/components/admin/admin-sign-out-button"
import { createClient } from "@/lib/supabase/server"
import type { Draw, Profile, SiteContent, UserTicket, WalletRequest } from "@/lib/types"

export const metadata: Metadata = {
  title: "Admin panel",
  description: "Manage draws, public content, users, and wallet requests.",
  robots: { index: false, follow: false },
}

type AdminTicket = UserTicket & { user_id: string }

export default async function AdminPage() {
  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()

  if (!user) redirect("/admin/login")

  const { data: membership } = await supabase
    .from("admin_users")
    .select("user_id")
    .eq("user_id", user.id)
    .maybeSingle()

  if (!membership) {
    return (
      <div className="page-shell grid min-h-[65vh] place-items-center py-10">
        <section className="surface-card max-w-xl p-7 text-center sm:p-10">
          <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-[#fbefef] text-[#8d4545]"><span className="text-xl font-bold">!</span></span>
          <p className="section-eyebrow mt-5">Restricted area</p>
          <h1 className="mt-2 text-2xl font-bold tracking-tight text-ink-strong">Administrator access has not been assigned</h1>
          <p className="mt-3 text-sm leading-6 text-muted">This account is not on the administrator allowlist. Sign in with the authorized admin account to manage draws, posts, users, and wallet requests.</p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <Link href="/admin/login" className="primary-button">Use admin login</Link>
            <AdminSignOutButton />
          </div>
        </section>
      </div>
    )
  }

  const [drawResult, profileResult, ticketResult, contentResult, walletRequestsResult] = await Promise.all([
    supabase.from("draws").select("id,draw_number,title,description,prize_details,ticket_price,total_tickets,sold_tickets,profit_margin,banner_image_url,status,winner_ticket,winner_user_id,created_at,updated_at,completed_at").order("draw_number", { ascending: true }),
    supabase.from("profiles").select("id,name,phone,balance,referral_count,free_tickets_earned,created_at").order("created_at", { ascending: false }).limit(1000),
    supabase.from("user_tickets").select("id,user_id,ticket_number,status,source,purchase_date,draws(draw_number,title)").order("purchase_date", { ascending: false }).limit(100),
    supabase.from("site_content").select("id,section_name,content_text,media_url,display_order,updated_at").order("display_order", { ascending: true }).order("updated_at", { ascending: false }),
    supabase.from("wallet_requests").select("id,user_id,request_type,provider,amount,account_phone,transaction_reference,status,admin_note,created_at,updated_at").order("created_at", { ascending: false }).limit(200),
  ])

  return (
    <div className="page-shell py-9 sm:py-12">
      <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="section-eyebrow">Operations</p>
          <h1 className="section-title mt-2">Admin panel</h1>
          <p className="mt-2 text-sm text-muted">Manage draws, site content, and user activity.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <span className="status-pill">Signed in as {user.email}</span>
          <AdminSignOutButton />
        </div>
      </div>
      <AdminPanel
        draws={(drawResult.data ?? []) as unknown as Draw[]}
        profiles={(profileResult.data ?? []) as unknown as Profile[]}
        tickets={(ticketResult.data ?? []) as unknown as AdminTicket[]}
        content={(contentResult.data ?? []) as unknown as SiteContent[]}
        walletRequests={(walletRequestsResult.data ?? []) as unknown as WalletRequest[]}
      />
    </div>
  )
}
