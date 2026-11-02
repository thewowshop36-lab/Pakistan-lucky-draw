package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.DrawEntity
import com.example.data.entity.ScratchRecordEntity
import com.example.data.entity.TicketEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.LotteryRepository
import com.example.ui.components.AppLanguage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LotteryScreen {
  HOME,
  LOTTERY,
  WINNERS,
  ME,
  ADMIN
}

data class ScratchCardDef(
  val id: String,
  val title: String,
  val subtitle: String,
  val pricePkr: Long,
  val maxPrizePkr: Long,
  val colorHex: Long,
  val symbols: List<String>
)

class LotteryViewModel(private val repository: LotteryRepository) : ViewModel() {

  // Global App States
  private val _language = MutableStateFlow(AppLanguage.EN)
  val language: StateFlow<AppLanguage> = _language.asStateFlow()

  private val _is18Verified = MutableStateFlow(false)
  val is18Verified: StateFlow<Boolean> = _is18Verified.asStateFlow()

  private val _isAdminMode = MutableStateFlow(false)
  val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

  private val _currentScreen = MutableStateFlow(LotteryScreen.HOME)
  val currentScreen: StateFlow<LotteryScreen> = _currentScreen.asStateFlow()

  private val _currentUserId = MutableStateFlow("usr_001")
  val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

  private val _toastMessage = MutableSharedFlow<String>()
  val toastMessage = _toastMessage.asSharedFlow()

  // Room Flows
  val currentUser: StateFlow<UserEntity?> = _currentUserId.flatMapLatest { id ->
    repository.getUser(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val activeDraws: StateFlow<List<DrawEntity>> = repository.getActiveDraws()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val completedDraws: StateFlow<List<DrawEntity>> = repository.getCompletedDraws()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allDraws: StateFlow<List<DrawEntity>> = repository.getAllDraws()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val userTickets: StateFlow<List<TicketEntity>> = _currentUserId.flatMapLatest { id ->
    repository.getTicketsForUser(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allTickets: StateFlow<List<TicketEntity>> = repository.getAllTickets()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val userTransactions: StateFlow<List<TransactionEntity>> = _currentUserId.flatMapLatest { id ->
    repository.getTransactionsForUser(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val scratchRecords: StateFlow<List<ScratchRecordEntity>> = repository.getAllScratchRecords()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Draw Game Number Selection State
  private val _selectedDraw = MutableStateFlow<DrawEntity?>(null)
  val selectedDraw: StateFlow<DrawEntity?> = _selectedDraw.asStateFlow()

  private val _selectedDays = MutableStateFlow<Set<Int>>(emptySet())
  val selectedDays: StateFlow<Set<Int>> = _selectedDays.asStateFlow()

  private val _selectedMonth = MutableStateFlow<Int?>(null)
  val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()

  private val _lineQuantity = MutableStateFlow(1)
  val lineQuantity: StateFlow<Int> = _lineQuantity.asStateFlow()

  // Scratch Cards Catalog
  val scratchCards = listOf(
    ScratchCardDef(
      id = "sc_01",
      title = "Green Crescent 777",
      subtitle = "Instant Win Triple Luck",
      pricePkr = 100L,
      maxPrizePkr = 50000L,
      colorHex = 0xFF0D5030,
      symbols = listOf("🌙", "⭐", "777", "💎", "💰", "👑")
    ),
    ScratchCardDef(
      id = "sc_02",
      title = "Golden Markhor",
      subtitle = "National Treasure Jackpot",
      pricePkr = 250L,
      maxPrizePkr = 250000L,
      colorHex = 0xFF533B0A,
      symbols = listOf("🐐", "🏆", "🌟", "💵", "💎", "🎯")
    ),
    ScratchCardDef(
      id = "sc_03",
      title = "Shaheen Royal Fortune",
      subtitle = "Eagle Eye Mega Cash",
      pricePkr = 500L,
      maxPrizePkr = 1000000L,
      colorHex = 0xFF142B4E,
      symbols = listOf("🦅", "👑", "💰", "💎", "777", "🏆")
    ),
    ScratchCardDef(
      id = "sc_04",
      title = "K2 Super Bumper",
      subtitle = "Himalayan Peak Fortune",
      pricePkr = 1000L,
      maxPrizePkr = 5000000L,
      colorHex = 0xFF4A1024,
      symbols = listOf("🏔️", "⭐", "💎", "💰", "👑", "🔥")
    )
  )

  fun toggleLanguage() {
    _language.value = if (_language.value == AppLanguage.EN) AppLanguage.UR else AppLanguage.EN
  }

  fun confirm18Plus() {
    _is18Verified.value = true
  }

  fun loginAdminWithPin(pin: String): Boolean {
    if (pin.trim() == "admin123" || pin.trim() == "1234") {
      _isAdminMode.value = true
      _currentUserId.value = "usr_admin"
      _currentScreen.value = LotteryScreen.ADMIN
      emitToast("Welcome Super Admin!")
      return true
    } else {
      emitToast("Invalid Admin PIN. Try: admin123")
      return false
    }
  }

  fun exitAdminConsole() {
    _isAdminMode.value = false
    _currentUserId.value = "usr_001"
    _currentScreen.value = LotteryScreen.ME
    emitToast("Exited Admin Console")
  }

  fun navigateTo(screen: LotteryScreen) {
    _currentScreen.value = screen
  }

  fun selectDrawForPlay(draw: DrawEntity) {
    _selectedDraw.value = draw
    _selectedDays.value = emptySet()
    _selectedMonth.value = null
    _lineQuantity.value = 1
    _currentScreen.value = LotteryScreen.LOTTERY
  }

  fun toggleDaySelection(day: Int) {
    val current = _selectedDays.value.toMutableSet()
    if (current.contains(day)) {
      current.remove(day)
    } else {
      if (current.size < 6) {
        current.add(day)
      } else {
        emitToast("You can only pick 6 Days (1-31)")
      }
    }
    _selectedDays.value = current
  }

  fun selectMonth(month: Int) {
    _selectedMonth.value = if (_selectedMonth.value == month) null else month
  }

  fun quickPickNumbers() {
    val randomDays = (1..31).shuffled().take(6).toSet()
    val randomMonth = (1..12).random()
    _selectedDays.value = randomDays
    _selectedMonth.value = randomMonth
  }

  fun clearSelection() {
    _selectedDays.value = emptySet()
    _selectedMonth.value = null
  }

  fun setLineQuantity(quantity: Int) {
    if (quantity in 1..10) {
      _lineQuantity.value = quantity
    }
  }

  fun purchaseTickets(
    onSuccess: (List<TicketEntity>) -> Unit,
    onError: (String) -> Unit
  ) {
    val draw = _selectedDraw.value ?: activeDraws.value.firstOrNull()
    if (draw == null) {
      onError("No active draw selected")
      return
    }

    if (_selectedDays.value.size != 6 || _selectedMonth.value == null) {
      onError("Please select 6 Days (1-31) and 1 Month (1-12)")
      return
    }

    val user = currentUser.value
    if (user == null) {
      onError("Please sign in or select user")
      return
    }

    val qty = _lineQuantity.value
    val entries = mutableListOf<Pair<List<Int>, Int>>()
    // First entry is user's exact chosen numbers
    entries.add(Pair(_selectedDays.value.toList(), _selectedMonth.value!!))

    // If user bought multiple lines, generate additional quick picks
    for (i in 1 until qty) {
      val rDays = (1..31).shuffled().take(6)
      val rMonth = (1..12).random()
      entries.add(Pair(rDays, rMonth))
    }

    viewModelScope.launch {
      val result = repository.buyTickets(
        userId = user.id,
        userName = user.name,
        userPhone = user.phone,
        drawId = draw.id,
        drawTitle = draw.title,
        ticketPricePkr = draw.ticketPricePkr,
        entries = entries
      )

      result.fold(
        onSuccess = { tickets ->
          clearSelection()
          onSuccess(tickets)
          emitToast("Successfully purchased ${tickets.size} Ticket(s)!")
        },
        onFailure = { ex ->
          onError(ex.message ?: "Failed to purchase tickets")
        }
      )
    }
  }

  // Wallet operations
  fun depositFunds(amount: Long, method: String, refNo: String, onSuccess: () -> Unit) {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      repository.depositMoney(user.id, amount, method, refNo)
      emitToast("Rs. %,d deposited successfully via %s!".format(amount, method))
      onSuccess()
    }
  }

  fun withdrawFunds(
    amount: Long,
    method: String,
    accountTitle: String,
    accountNumber: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      val res = repository.requestWithdrawal(
        user.id,
        amount,
        method,
        accountTitle,
        accountNumber
      )
      res.fold(
        onSuccess = {
          emitToast("Withdrawal of Rs. %,d processed to %s!".format(amount, method))
          onSuccess()
        },
        onFailure = { ex ->
          onError(ex.message ?: "Withdrawal failed")
        }
      )
    }
  }

  // Scratch card play
  fun executeScratchResult(
    card: ScratchCardDef,
    wonMultiplier: Int, // 0 for loss, 1x, 2x, 5x, 10x, 100x etc.
    onFinished: (Long, Boolean) -> Unit
  ) {
    val user = currentUser.value ?: return
    if (user.walletBalancePkr < card.pricePkr) {
      emitToast("Insufficient balance for ${card.title}. Top up your wallet.")
      return
    }

    val wonPrize = if (wonMultiplier > 0) {
      (card.pricePkr * wonMultiplier).coerceAtMost(card.maxPrizePkr)
    } else {
      0L
    }
    val isWin = wonPrize > 0

    viewModelScope.launch {
      repository.recordScratchGame(
        userId = user.id,
        cardName = card.title,
        costPkr = card.pricePkr,
        prizeWonPkr = wonPrize,
        isWin = isWin
      )
      onFinished(wonPrize, isWin)
    }
  }

  // Super Admin Methods
  fun adminCreateNewDraw(
    title: String,
    prizePoolPkr: Long,
    ticketPricePkr: Long,
    drawTime: Long,
    categoryBadge: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      repository.adminCreateDraw(title, prizePoolPkr, ticketPricePkr, drawTime, categoryBadge)
      emitToast("Draw created successfully: $title")
      onSuccess()
    }
  }

  fun adminUpdateDrawStatus(drawId: String, status: String) {
    viewModelScope.launch {
      repository.adminUpdateDrawStatus(drawId, status)
      emitToast("Draw status updated to $status")
    }
  }

  fun adminAdjustUserBalance(userId: String, deltaPkr: Long, note: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      repository.adminAdjustBalance(userId, deltaPkr, note)
      emitToast("Wallet adjusted by Rs. %,d".format(deltaPkr))
      onSuccess()
    }
  }

  fun adminDeclareWinner(
    drawId: String,
    winnerTicketId: String,
    winningDays: List<Int>,
    winningMonth: Int,
    customPrizePkr: Long?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      val res = repository.adminDeclareWinner(
        drawId = drawId,
        winnerTicketId = winnerTicketId,
        winningDays = winningDays,
        winningMonth = winningMonth,
        customPrizePkr = customPrizePkr
      )
      res.fold(
        onSuccess = { draw ->
          emitToast("Winner declared! Rs. %,d credited to %s".format(draw.winnerPrizeAmountPkr, draw.winnerUserName))
          onSuccess()
        },
        onFailure = { ex ->
          onError(ex.message ?: "Failed to declare winner")
        }
      )
    }
  }

  private fun emitToast(msg: String) {
    viewModelScope.launch {
      _toastMessage.emit(msg)
    }
  }

  class Factory(private val repository: LotteryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(LotteryViewModel::class.java)) {
        return LotteryViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
