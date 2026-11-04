export type DrawStatus = "active" | "completed"
export type DrawEndCondition = "sold_out" | "admin_close"
export type PaymentProvider = "jazzcash" | "easypaisa"

export type Draw = {
  id: string
  draw_number: number
  title: string
  description: string
  prize_details: string
  ticket_price: number | string
  total_tickets: number
  sold_tickets: number
  reserved_tickets: number
  profit_margin: number | string
  banner_image_url: string | null
  status: DrawStatus
  end_condition: DrawEndCondition
  closed_at: string | null
  winner_ticket: number | null
  server_seed_commitment: string | null
  revealed_server_seed: string | null
  beacon_source: string | null
  beacon_pulse_index: string | null
  beacon_timestamp: string | null
  beacon_output_value: string | null
  beacon_pulse_uri: string | null
  beacon_signature: string | null
  draw_algorithm: string
  created_at: string
  updated_at?: string
  completed_at: string | null
}

export type DrawPrize = {
  id?: string
  draw_id: string
  tier_order: number
  prize_name: string
  prize_description: string
  prize_amount: number | string
  winner_ticket_number: number | null
  winner_user_id?: string | null
  winner_public_name: string | null
}

export type DrawVerification = {
  id: string
  draw_number: number
  title: string
  prize_details: string
  status: DrawStatus
  end_condition: DrawEndCondition
  sold_tickets: number
  total_tickets: number
  profit_margin: number | string
  winner_ticket: number | null
  winner_name: string
  server_seed_commitment: string | null
  revealed_server_seed: string | null
  closed_at: string | null
  beacon_source: string | null
  beacon_pulse_index: string | null
  beacon_timestamp: string | null
  beacon_output_value: string | null
  beacon_pulse_uri: string | null
  beacon_signature: string | null
  draw_algorithm: string | null
  created_at: string
  completed_at: string
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
  email_verified_at: string | null
  phone_verified_at: string | null
  age_confirmed_at: string | null
  terms_accepted_at: string | null
  terms_version: string | null
  account_status: "active" | "suspended"
  suspended_at: string | null
  suspension_reason: string | null
  balance: number | string
  referral_count: number
  free_tickets_earned: number
  created_at: string
}

export type UserTicket = {
  id: string
  ticket_number: number
  status: "active" | "won" | "lost"
  source: "paid" | "referral" | "manual_payment"
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
  transaction_type: "ticket_purchase" | "admin_credit" | "admin_debit" | "deposit_credit" | "withdrawal_hold" | "withdrawal_release" | "draw_prize"
  description: string
  created_at: string
}

export type WalletRequest = {
  id: string
  user_id: string
  request_type: "deposit" | "withdrawal"
  provider: PaymentProvider
  amount: number | string
  account_phone: string
  transaction_reference: string | null
  status: "pending" | "approved" | "rejected" | "paid"
  admin_note: string
  created_at: string
  updated_at: string
}

export type PaymentAccount = {
  provider: PaymentProvider
  account_title: string
  account_identifier: string
  instructions: string
  is_active: boolean
  updated_at: string
}

export type TicketPaymentSubmission = {
  id: string
  user_id: string
  draw_id: string
  provider: PaymentProvider
  quantity: number
  ticket_price: number | string
  amount: number | string
  transaction_reference: string
  proof_path: string
  status: "pending" | "approved" | "rejected"
  admin_note: string
  reviewed_at: string | null
  created_at: string
  profiles?: { name: string; phone: string | null } | { name: string; phone: string | null }[] | null
  draws?: { draw_number: number; title: string } | { draw_number: number; title: string }[] | null
}

export type DrawFinancialSummary = {
  draw_id: string
  draw_number: number
  gross_revenue: number | string
  platform_profit: number | string
  prize_pool: number | string
  committed_prizes: number | string
  remaining_prize_pool: number | string
  paid_tickets: number
  referral_tickets: number
  pending_payment_amount: number | string
  pending_payment_tickets: number
  reserved_tickets: number
}

export type AdminAuditLog = {
  id: number
  actor_user_id: string | null
  actor_name: string | null
  action: string
  entity_type: string
  entity_id: string | null
  details: Record<string, unknown>
  created_at: string
}
