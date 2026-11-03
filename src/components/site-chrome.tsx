"use client"

import { useEffect, useState, type ReactNode } from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { CircleUserRound, House, LogOut, Ticket, TicketCheck, Trophy } from "lucide-react"
import type { AuthChangeEvent, Session } from "@supabase/supabase-js"
import { createClient } from "@/lib/supabase/client"

export type InitialUser = {
  id: string
  email: string
  name: string
} | null

const navigation = [
  { href: "/", label: "Home", icon: House },
  { href: "/draws", label: "Draws", icon: Ticket },
  { href: "/history", label: "Winners", icon: Trophy },
  { href: "/profile", label: "Profile", icon: CircleUserRound },
]

export function SiteChrome({ children, initialUser }: { children: ReactNode; initialUser: InitialUser }) {
  const [user, setUser] = useState(initialUser)
  const pathname = usePathname()
  const router = useRouter()
  const supabase = createClient()

  useEffect(() => {
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event: AuthChangeEvent, session: Session | null) => {
      const nextUser = session?.user
      setUser(nextUser ? {
        id: nextUser.id,
        email: nextUser.email ?? "",
        name: String(nextUser.user_metadata?.name ?? "My profile"),
      } : null)
    })

    return () => subscription.unsubscribe()
  }, [supabase])

  async function handleSignOut() {
    await supabase.auth.signOut()
    setUser(null)
    router.push("/")
    router.refresh()
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-40 border-b border-line/80 bg-white/95 backdrop-blur-xl">
        <div className="page-shell flex h-[4.4rem] items-center justify-between gap-4">
          <Link href="/" className="flex shrink-0 items-center gap-2.5" aria-label="Lucky Draw home">
            <span className="grid size-10 place-items-center rounded-2xl bg-green text-white shadow-sm">
              <TicketCheck size={20} strokeWidth={2.3} aria-hidden="true" />
            </span>
            <span className="leading-tight">
              <span className="block text-[0.92rem] font-extrabold tracking-tight text-ink-strong">Lucky Draw</span>
              <span className="block text-[0.58rem] font-bold tracking-[0.16em] text-muted">PAKISTAN</span>
            </span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex" aria-label="Main navigation">
            {navigation.map((item) => {
              const active = pathname === item.href || (item.href !== "/" && pathname.startsWith(item.href))
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={`rounded-xl px-3.5 py-2 text-sm font-semibold transition-colors ${active ? "bg-green-pale text-green" : "text-muted hover:bg-surface-soft hover:text-ink"}`}
                >
                  {item.label}
                </Link>
              )
            })}
          </nav>

          <div className="flex shrink-0 items-center gap-2">
            {user ? (
              <>
                <Link href="/profile" className="hidden items-center rounded-xl border border-line bg-white px-3 py-2 text-xs font-bold text-ink sm:flex">
                  <span className="max-w-36 truncate">{user.name || "My profile"}</span>
                </Link>
                <button type="button" onClick={handleSignOut} className="ghost-button min-h-10 px-3" aria-label="Sign out" title="Sign out">
                  <LogOut size={17} aria-hidden="true" />
                  <span className="hidden sm:inline">Sign out</span>
                </button>
              </>
            ) : (
              <>
                <Link href="/auth/login" className="hidden rounded-xl px-3 py-2 text-sm font-semibold text-ink hover:bg-surface-soft sm:inline-flex">Sign in</Link>
                <Link href="/auth/sign-up" className="primary-button min-h-10 px-4">Create account</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="flex-1 pb-24 md:pb-0">{children}</main>

      <footer className="mt-12 border-t border-line bg-white pb-6 pt-8 md:pb-8">
        <div className="page-shell flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-bold text-ink-strong">Lucky Draw</p>
            <p className="mt-1 max-w-xl text-xs leading-5 text-muted">18+ only. Participation may be restricted by local law. Review each draw&apos;s terms before entering.</p>
          </div>
          <p className="text-xs font-medium text-muted">Transparent entries. Published results. Play responsibly.</p>
        </div>
      </footer>

      <nav className="mobile-bottom-nav md:hidden" aria-label="Mobile navigation">
        <div className="mx-auto grid max-w-lg grid-cols-4 gap-1">
          {navigation.map((item) => {
            const active = pathname === item.href || (item.href !== "/" && pathname.startsWith(item.href))
            const Icon = item.icon
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`flex min-h-14 flex-col items-center justify-center gap-1 rounded-xl text-[0.65rem] font-bold ${active ? "text-green" : "text-muted"}`}
              >
                <Icon size={19} strokeWidth={active ? 2.4 : 1.9} aria-hidden="true" />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </div>
      </nav>
    </div>
  )
}
