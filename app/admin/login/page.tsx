import type { Metadata } from "next"
import { redirect } from "next/navigation"
import { AdminLoginForm } from "@/components/admin/admin-login-form"
import { createClient } from "@/lib/supabase/server"

export const metadata: Metadata = {
  title: "Admin sign-in",
  description: "Private sign-in for authorized Lucky Draw administrators.",
  robots: { index: false, follow: false },
}

export default async function AdminLoginPage() {
  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()

  if (user) {
    const { data: membership } = await supabase
      .from("admin_users")
      .select("user_id")
      .eq("user_id", user.id)
      .maybeSingle()

    if (membership) redirect("/admin")
  }

  return (
    <div className="page-shell grid min-h-[70vh] place-items-center py-10">
      <AdminLoginForm />
    </div>
  )
}
