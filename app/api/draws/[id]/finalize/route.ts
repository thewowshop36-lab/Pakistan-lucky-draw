import { revalidatePath } from "next/cache"
import { createClient } from "@/lib/supabase/server"
import { createAdminClient } from "@/lib/supabase/admin"

export const runtime = "nodejs"

function errorResponse(message: string, status: number) {
  return Response.json({ error: message }, { status })
}

export async function POST(request: Request, context: { params: Promise<{ id: string }> }) {
  const { id: drawId } = await context.params
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(drawId)) {
    return errorResponse("Draw not found.", 404)
  }

  const body = await request.json().catch(() => ({})) as { manual?: unknown }
  const manual = body.manual === true
  if (manual) {
    const userClient = await createClient()
    const { data: { user } } = await userClient.auth.getUser()
    if (!user) return errorResponse("Sign in with an administrator account to close this draw early.", 401)
    const { data: membership, error: membershipError } = await userClient
      .from("admin_users")
      .select("user_id")
      .eq("user_id", user.id)
      .maybeSingle()
    if (membershipError || !membership) return errorResponse("Administrator access is required to close this draw early.", 403)
  }


  const adminClient = createAdminClient()
  const { data: draw, error: drawError } = await adminClient
    .from("draws")
    .select("id,status")
    .eq("id", drawId)
    .maybeSingle()
  if (drawError || !draw) return errorResponse("Draw not found.", 404)
  if (draw.status === "completed") return Response.json({ status: "completed", draw_id: drawId })

  const { error: closeError } = await adminClient.rpc("mark_draw_closed", {
    p_draw_id: drawId,
    p_allow_partial: manual,
  })
  if (closeError) {
    const { data: refreshedDraw } = await adminClient.from("draws").select("status").eq("id", drawId).maybeSingle()
    if (refreshedDraw?.status === "completed") return Response.json({ status: "completed", draw_id: drawId })
    const closeMessage = closeError.message.toLowerCase()
    if (closeMessage.includes("pending payment")) return errorResponse("Review pending ticket payments before publishing the result.", 409)
    if (closeMessage.includes("sold out") || closeMessage.includes("close only") || closeMessage.includes("at least one")) {
      return errorResponse("This draw is not ready to close under its current settings.", 409)
    }
    return errorResponse("The draw could not be closed. Refresh and try again.", 409)
  }

  let beaconResponse: Response
  try {
    beaconResponse = await fetch("https://beacon.nist.gov/beacon/2.0/pulse/last", {
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
      headers: { accept: "application/json" },
    })
  } catch {
    return errorResponse("The NIST randomness beacon is temporarily unavailable. Retry shortly.", 502)
  }
  if (!beaconResponse.ok) return errorResponse("The NIST randomness beacon is temporarily unavailable. Retry shortly.", 502)

  const payload = await beaconResponse.json().catch(() => null) as { pulse?: Record<string, unknown>; pulses?: Record<string, unknown>[]; pulseList?: Record<string, unknown>[] } | null
  const pulse = payload?.pulse ?? payload?.pulses?.[0] ?? payload?.pulseList?.[0]
  const outputValue = typeof pulse?.outputValue === "string" ? pulse.outputValue : ""
  const pulseIndex = String(pulse?.pulseIndex ?? "")
  const rawTimestamp = String(pulse?.timeStamp ?? pulse?.timestamp ?? "")
  const beaconTimestamp = new Date(rawTimestamp)
  const pulseUri = typeof pulse?.uri === "string"
    ? pulse.uri
    : pulseIndex ? `https://beacon.nist.gov/beacon/2.0/pulse/${encodeURIComponent(pulseIndex)}` : ""
  const signature = typeof pulse?.signatureValue === "string" ? pulse.signatureValue : null

  if (!pulse || !/^[a-f\d]{64,512}$/i.test(outputValue) || outputValue.length % 2 !== 0 || !pulseIndex || Number.isNaN(beaconTimestamp.getTime()) || !pulseUri.startsWith("https://beacon.nist.gov/")) {
    return errorResponse("The NIST service returned an incomplete randomness pulse. Retry shortly.", 502)
  }

  const { data: result, error: finalizeError } = await adminClient.rpc("finalize_draw_with_beacon", {
    p_draw_id: drawId,
    p_beacon_output_value: outputValue,
    p_beacon_pulse_index: pulseIndex,
    p_beacon_timestamp: beaconTimestamp.toISOString(),
    p_beacon_pulse_uri: pulseUri,
    p_beacon_signature: signature,
  })

  if (finalizeError) {
    const message = finalizeError.message.toLowerCase()
    if (message.includes("after the draw closes")) {
      return errorResponse("The latest NIST pulse predates the draw close. Retry after the next public pulse.", 409)
    }
    if (message.includes("pending payment")) return errorResponse("Review pending ticket payments before publishing the result.", 409)
    if (message.includes("prize pool")) return errorResponse("The configured cash prizes exceed the revenue-backed prize pool.", 409)
    return errorResponse("The verified result could not be published. Refresh and try again.", 409)
  }

  revalidatePath("/")
  revalidatePath("/draws")
  revalidatePath("/history")
  revalidatePath("/admin")
  return Response.json(result)
}
