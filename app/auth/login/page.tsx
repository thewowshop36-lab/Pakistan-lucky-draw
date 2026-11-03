import type { Metadata } from "next"
import { AuthForm } from "@/components/auth-form"
import { safeInternalPath } from "@/lib/format"

export const metadata: Metadata = { title: "Sign in" }

type PageProps = { searchParams: Promise<{ next?: string }> }

export default async function LoginPage({ searchParams }: PageProps) {
  const params = await searchParams
  return (
    <div className="page-shell grid min-h-[70vh] place-items-center py-10">
      <AuthForm mode="login" nextPath={safeInternalPath(params.next)} />
    </div>
  )
}
