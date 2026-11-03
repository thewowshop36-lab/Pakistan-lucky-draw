import { createClient } from "@/lib/supabase/server"
import type { Draw, SiteContent, WinnerRecord } from "@/lib/types"

const DRAW_COLUMNS = "id,draw_number,title,description,prize_details,ticket_price,total_tickets,sold_tickets,profit_margin,banner_image_url,status,winner_ticket,winner_user_id,created_at,updated_at,completed_at"
const CONTENT_COLUMNS = "id,section_name,content_text,media_url,display_order,updated_at"

export async function getActiveDraws(): Promise<Draw[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("draws")
    .select(DRAW_COLUMNS)
    .eq("status", "active")
    .order("draw_number", { ascending: true })

  return (data ?? []) as unknown as Draw[]
}

export async function getRecentDraws(limit = 6): Promise<Draw[]> {
  const supabase = await createClient()
  const { data } = await supabase
    .from("draws")
    .select(DRAW_COLUMNS)
    .eq("status", "active")
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
