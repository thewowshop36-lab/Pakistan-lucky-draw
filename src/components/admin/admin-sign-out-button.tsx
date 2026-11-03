"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { LogOut } from "lucide-react"
import { createClient } from "@/lib/supabase/client"

export function AdminSignOutButton() {
  const [busy, setBusy] = useState(false)
  const router = useRouter()
  const supabase = createClient()

  async function handleSignOut() {
    if (busy) return
    setBusy(true)
    await supabase.auth.signOut()
    router.replace("/admin/login")
    router.refresh()
  }

  return (
    <button type="button" className="ghost-button min-h-10 px-3" onClick={handleSignOut} disabled={busy} aria-label="Sign out of admin">
      <LogOut size={17} aria-hidden="true" />
      <span className="hidden sm:inline">{busy ? "Signing out…" : "Sign out"}</span>
    </button>
  )
}
