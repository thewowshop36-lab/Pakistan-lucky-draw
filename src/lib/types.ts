export type DrawStatus = "active" | "completed"

export type Draw = {
  id: string
  draw_number: number
  title: string
  description: string
  prize_details: string
  ticket_price: number | string
  total_tickets: number
  sold_tickets: number
  profit_margin: number | string
  banner_image_url: string | null
  status: DrawStatus
  winner_ticket: number | null
  winner_user_id?: string | null
  created_at: string
  updated_at?: string
  completed_at: string | null
}

export type SiteContent = {
  id: string
  section_name: "hero_banner" | "guidelines" | "trust" | "videos"
  content_text: string
  media_url: string | null
  display_order: number
  updated_at: string
}

export type Profile = {
  id: string
  name: string
  phone: string | null
  balance: number | string
  referral_count: number
  free_tickets_earned: number
  created_at: string
}

export type UserTicket = {
  id: string
  ticket_number: number
  status: "active" | "won" | "lost"
  source: "paid" | "referral"
  purchase_date: string
  draws: { draw_number: number; title: string } | null
}

export type WinnerRecord = {
  id: string
  draw_number: number
  title: string
  prize_details: string
  winner_ticket: number
  winner_name: string
  completed_at: string
}

export type WalletTransaction = {
  id: string
  amount: number | string
  transaction_type: "ticket_purchase" | "admin_credit" | "admin_debit"
  description: string
  created_at: string
}
