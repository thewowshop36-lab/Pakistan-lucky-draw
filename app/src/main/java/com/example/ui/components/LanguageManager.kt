package com.example.ui.components

enum class AppLanguage {
  EN,
  UR
}

object LanguageManager {
  private val translations = mapOf(
    "app_name" to mapOf(AppLanguage.EN to "Pakistan Lottery", AppLanguage.UR to "پاکستان لاٹری"),
    "app_tagline" to mapOf(AppLanguage.EN to "Official Lucky Draw & Gaming Portal", AppLanguage.UR to "سرکاری لکی ڈرا اور گیمنگ پورٹل"),
    "nav_home" to mapOf(AppLanguage.EN to "Home", AppLanguage.UR to "ہوم"),
    "nav_play" to mapOf(AppLanguage.EN to "Play Draw", AppLanguage.UR to "ڈرا کھیلیں"),
    "nav_scratch" to mapOf(AppLanguage.EN to "Scratch Cards", AppLanguage.UR to "سکریچ کارڈز"),
    "nav_results" to mapOf(AppLanguage.EN to "Results", AppLanguage.UR to "نتائج"),
    "nav_wallet" to mapOf(AppLanguage.EN to "Wallet", AppLanguage.UR to "والیٹ"),
    "nav_admin" to mapOf(AppLanguage.EN to "Admin Panel", AppLanguage.UR to "ایڈمن پینل"),
    "grand_jackpot" to mapOf(AppLanguage.EN to "Grand Jackpot Pool", AppLanguage.UR to "عظیم الشان جیک پاٹ پول"),
    "draw_countdown" to mapOf(AppLanguage.EN to "Live Draw Countdown", AppLanguage.UR to "قرعہ اندازی لائیو کاؤنٹ ڈاؤن"),
    "quick_pick" to mapOf(AppLanguage.EN to "Quick Pick (Random)", AppLanguage.UR to "کوئیک پک (خودکار)"),
    "clear_selection" to mapOf(AppLanguage.EN to "Clear", AppLanguage.UR to "صاف کریں"),
    "days_pick" to mapOf(AppLanguage.EN to "Step 1: Pick 6 Days (1-31)", AppLanguage.UR to "مرحلہ 1: 6 دن منتخب کریں (1-31)"),
    "month_pick" to mapOf(AppLanguage.EN to "Step 2: Pick 1 Month (1-12)", AppLanguage.UR to "مرحلہ 2: 1 مہینہ منتخب کریں (1-12)"),
    "ticket_price" to mapOf(AppLanguage.EN to "Ticket Price", AppLanguage.UR to "ٹکٹ کی قیمت"),
    "buy_ticket" to mapOf(AppLanguage.EN to "Confirm & Buy Ticket", AppLanguage.UR to "ٹکٹ خریدیں"),
    "scratch_play" to mapOf(AppLanguage.EN to "Scratch to Win", AppLanguage.UR to "سکریچ کریں اور جیتیں"),
    "balance" to mapOf(AppLanguage.EN to "Wallet Balance", AppLanguage.UR to "والیٹ بیلنس"),
    "deposit" to mapOf(AppLanguage.EN to "Deposit PKR", AppLanguage.UR to "رقم جمع کروائیں"),
    "withdraw" to mapOf(AppLanguage.EN to "Withdraw", AppLanguage.UR to "رقم نکلوائیں"),
    "recent_winners" to mapOf(AppLanguage.EN to "Recent Lucky Winners", AppLanguage.UR to "حالیہ خوش نصیب فاتحین"),
    "how_to_play" to mapOf(AppLanguage.EN to "How to Play", AppLanguage.UR to "کیسے کھیلیں"),
    "check_ticket" to mapOf(AppLanguage.EN to "Check My Ticket", AppLanguage.UR to "اپنا ٹکٹ چیک کریں"),
    "manual_winner_selection" to mapOf(AppLanguage.EN to "Manual Winner Selection", AppLanguage.UR to "دستی فاتح کا انتخاب"),
    "declare_winner_btn" to mapOf(AppLanguage.EN to "Declare & Credit Winner", AppLanguage.UR to "فاتح کا اعلان اور انعام"),
    "admin_mode" to mapOf(AppLanguage.EN to "Super Admin Mode", AppLanguage.UR to "سپر ایڈمن موڈ"),
    "switch_role" to mapOf(AppLanguage.EN to "Switch Role", AppLanguage.UR to "رول تبدیل کریں")
  )

  fun get(key: String, lang: AppLanguage): String {
    return translations[key]?.get(lang) ?: key
  }
}
