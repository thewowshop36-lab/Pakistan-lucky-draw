import { NextResponse, type NextRequest } from "next/server"
import { safeInternalPath } from "@/lib/format"
import { createClient } from "@/lib/supabase/server"

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code")
  const destination = safeInternalPath(request.nextUrl.searchParams.get("next"), "/profile")

  if (code) {
    const supabase = await createClient()
    const { error } = await supabase.auth.exchangeCodeForSession(code)
    if (!error) return NextResponse.redirect(new URL(destination, request.nextUrl.origin))
  }

  return NextResponse.redirect(new URL("/auth/error", request.nextUrl.origin))
}
