import type { Metadata, Viewport } from "next"
import type { ReactNode } from "react"
import { SiteChrome } from "@/components/site-chrome"
import { createClient } from "@/lib/supabase/server"
import "./globals.css"

export const metadata: Metadata = {
  title: {
    default: "Lucky Draw | Clear terms, public results",
    template: "%s | Lucky Draw",
  },
  description: "Browse active lucky draws, track your tickets, and review completed results in one place.",
  applicationName: "Lucky Draw",
  robots: { index: true, follow: true },
}

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#10382a",
  colorScheme: "light",
}

type InitialUser = {
  id: string
  email: string
  name: string
} | null

export default async function RootLayout({ children }: { children: ReactNode }) {
  const supabase = await createClient()
  const { data: { user } } = await supabase.auth.getUser()
  let initialUser: InitialUser = null

  if (user) {
    initialUser = {
      id: user.id,
      email: user.email ?? "",
      name: typeof user.user_metadata?.name === "string" ? user.user_metadata.name : "My profile",
    }
  }

  return (
    <html lang="en">
      <body>
        <SiteChrome initialUser={initialUser}>{children}</SiteChrome>
      </body>
    </html>
  )
}
