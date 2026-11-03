import type { Metadata } from "next"
import { AuthForm } from "@/components/auth-form"
import { safeInternalPath } from "@/lib/format"

export const metadata: Metadata = { title: "Create account" }

type PageProps = { searchParams: Promise<{ ref?: string; next?: string }> }

export default async function SignUpPage({ searchParams }: PageProps) {
  const params = await searchParams
  return (
    <div className="page-shell grid min-h-[70vh] place-items-center py-10">
      <AuthForm mode="signup" referralId={params.ref} nextPath={safeInternalPath(params.next)} />
    </div>
  )
}
