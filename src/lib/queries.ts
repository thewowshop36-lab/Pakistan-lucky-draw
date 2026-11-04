import { createClient } from "@/lib/supabase/server"
import type { Draw, DrawPrize, DrawVerification, PaymentAccount, SiteContent, WinnerRecord } from "@/lib/types"

export const DRAW_COLUMNS = "id,draw_number,title,description,prize_details,ticket_price,total_tickets,sold_tickets,reserved_tickets,profit_margin,banner_image_url,status,end_condition,closed_at,winner_ticket,server_seed_commitment,revealed_server_seed,beacon_source,beacon_pulse_index,beacon_timestamp,beacon_output_value,beacon_pulse_uri,beacon_signature,draw_algorithm,created_at,updated_at,completed_at"
const CONTENT_COLUMNS = "id,section_name,content_text,media_url,display_order,updated_at"

export async function getActiveDraws(): Promise<Draw[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("draws")
    .select(DRAW_COLUMNS)
    .eq("status", "active")
    .or("closed_at.is.null,end_condition.eq.sold_out")
    .order("draw_number", { ascending: true })

  return (data ?? []) as unknown as Draw[]
}

export async function getRecentDraws(limit = 6): Promise<Draw[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("draws")
    .select(DRAW_COLUMNS)
    .eq("status", "active")
    .or("closed_at.is.null,end_condition.eq.sold_out")
    .order("draw_number", { ascending: true })
    .limit(limit)

  return (data ?? []) as unknown as Draw[]
}

export async function getSiteContent(): Promise<SiteContent[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("site_content")
    .select(CONTENT_COLUMNS)
    .order("display_order", { ascending: true })
    .order("updated_at", { ascending: false })

  return (data ?? []) as unknown as SiteContent[]
}

export async function getWinnerHistory(): Promise<WinnerRecord[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("winner_history")
    .select("id,draw_number,title,prize_details,winner_ticket,winner_name,completed_at")
    .order("completed_at", { ascending: false })

  return (data ?? []) as unknown as WinnerRecord[]
}

export async function getDrawVerifications(): Promise<DrawVerification[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("draw_verification_public")
    .select("id,draw_number,title,prize_details,status,end_condition,sold_tickets,total_tickets,profit_margin,winner_ticket,winner_name,server_seed_commitment,revealed_server_seed,closed_at,beacon_source,beacon_pulse_index,beacon_timestamp,beacon_output_value,beacon_pulse_uri,beacon_signature,draw_algorithm,created_at,completed_at")
    .order("completed_at", { ascending: false })
  return (data ?? []) as unknown as DrawVerification[]
}

export async function getPublicDrawPrizes(drawIds: string[]): Promise<DrawPrize[]> {
  if (!drawIds.length) return []
  const supabase = await createClient()
  const { data } = await supabase
    .from("draw_prizes_public")
    .select("draw_id,tier_order,prize_name,prize_description,prize_amount,winner_ticket_number,winner_public_name")
    .in("draw_id", drawIds)
    .order("tier_order", { ascending: true })
  return (data ?? []) as unknown as DrawPrize[]
}

export async function getActivePaymentAccounts(): Promise<PaymentAccount[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("payment_accounts")
    .select("provider,account_title,account_identifier,instructions,is_active,updated_at")
    .eq("is_active", true)
    .order("provider", { ascending: true })
  return (data ?? []) as unknown as PaymentAccount[]
}
