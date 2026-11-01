package com.example.data.repository

import com.example.data.entity.DrawEntity
import com.example.data.entity.ScratchRecordEntity
import com.example.data.entity.TicketEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.local.LotteryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class LotteryRepository(private val dao: LotteryDao) {

  fun getUser(id: String): Flow<UserEntity?> = dao.getUser(id)
  fun getAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()

  fun getActiveDraws(): Flow<List<DrawEntity>> = dao.getActiveDraws()
  fun getCompletedDraws(): Flow<List<DrawEntity>> = dao.getCompletedDraws()
  fun getAllDraws(): Flow<List<DrawEntity>> = dao.getAllDraws()

  fun getTicketsForUser(userId: String): Flow<List<TicketEntity>> = dao.getTicketsForUser(userId)
  fun getTicketsForDraw(drawId: String): Flow<List<TicketEntity>> = dao.getTicketsForDraw(drawId)
  fun getAllTickets(): Flow<List<TicketEntity>> = dao.getAllTickets()

  fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>> = dao.getTransactionsForUser(userId)
  fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAllTransactions()
  fun getAllScratchRecords(): Flow<List<ScratchRecordEntity>> = dao.getAllScratchRecords()

  suspend fun buyTickets(
    userId: String,
    userName: String,
    userPhone: String,
    drawId: String,
    drawTitle: String,
    ticketPricePkr: Long,
    entries: List<Pair<List<Int>, Int>> // List of (6 days, 1 month)
  ): Result<List<TicketEntity>> {
    val totalCost = ticketPricePkr * entries.size
    val user = dao.getUser(userId).firstOrNull() ?: return Result.failure(Exception("User not found"))

    if (user.walletBalancePkr < totalCost) {
      return Result.failure(Exception("Insufficient wallet balance. Please top up your PKR balance."))
    }

    val newBalance = user.walletBalancePkr - totalCost
    dao.updateWalletBalance(userId, newBalance)

    val createdTickets = mutableListOf<TicketEntity>()
    for (entry in entries) {
      val ticketId = "PKL-2026-" + (10000..99999).random()
      val sortedDays = entry.first.sorted()
      val daysJson = sortedDays.joinToString(",") { String.format("%02d", it) }
      val ticket = TicketEntity(
        id = ticketId,
        drawId = drawId,
        drawTitle = drawTitle,
        userId = userId,
        userName = userName,
        userPhone = userPhone,
        selectedDaysJson = daysJson,
        selectedMonth = entry.second,
        pricePkr = ticketPricePkr
      )
      createdTickets.add(ticket)
      dao.insertTicket(ticket)
    }

    val txn = TransactionEntity(
      id = "TXN-" + (10000..99999).random(),
      userId = userId,
      amountPkr = totalCost,
      type = "PURCHASE",
      status = "COMPLETED",
      description = "${entries.size}x Entries for $drawTitle",
      paymentMethod = "WALLET",
      referenceNumber = createdTickets.first().id
    )
    dao.insertTransaction(txn)

    return Result.success(createdTickets)
  }

  suspend fun depositMoney(
    userId: String,
    amountPkr: Long,
    method: String,
    refNo: String
  ): TransactionEntity {
    val user = dao.getUser(userId).firstOrNull()
    if (user != null) {
      dao.updateWalletBalance(userId, user.walletBalancePkr + amountPkr)
    }
    val txn = TransactionEntity(
      id = "TXN-" + (10000..99999).random(),
      userId = userId,
      amountPkr = amountPkr,
      type = "DEPOSIT",
      status = "COMPLETED",
      description = "$method Deposit Top-up (Ref: $refNo)",
      paymentMethod = method,
      referenceNumber = refNo
    )
    dao.insertTransaction(txn)
    return txn
  }

  suspend fun requestWithdrawal(
    userId: String,
    amountPkr: Long,
    method: String,
    accountTitle: String,
    accountNumber: String
  ): Result<TransactionEntity> {
    val user = dao.getUser(userId).firstOrNull() ?: return Result.failure(Exception("User not found"))
    if (user.walletBalancePkr < amountPkr) {
      return Result.failure(Exception("Insufficient balance to withdraw."))
    }
    val newBalance = user.walletBalancePkr - amountPkr
    dao.updateWalletBalance(userId, newBalance)

    val txn = TransactionEntity(
      id = "TXN-" + (10000..99999).random(),
      userId = userId,
      amountPkr = amountPkr,
      type = "WITHDRAWAL",
      status = "COMPLETED",
      description = "$method Payout to $accountTitle ($accountNumber)",
      paymentMethod = method,
      referenceNumber = "PO-" + (100000..999999).random()
    )
    dao.insertTransaction(txn)
    return Result.success(txn)
  }

  suspend fun recordScratchGame(
    userId: String,
    cardName: String,
    costPkr: Long,
    prizeWonPkr: Long,
    isWin: Boolean
  ) {
    val user = dao.getUser(userId).firstOrNull() ?: return
    val netChange = prizeWonPkr - costPkr
    val newBalance = (user.walletBalancePkr + netChange).coerceAtLeast(0L)
    dao.updateWalletBalance(userId, newBalance)

    val record = ScratchRecordEntity(
      id = UUID.randomUUID().toString(),
      cardName = cardName,
      costPkr = costPkr,
      prizeWonPkr = prizeWonPkr,
      isWin = isWin
    )
    dao.insertScratchRecord(record)

    if (prizeWonPkr > 0) {
      val winTxn = TransactionEntity(
        id = "TXN-" + (10000..99999).random(),
        userId = userId,
        amountPkr = prizeWonPkr,
        type = "WIN",
        status = "COMPLETED",
        description = "Instant Win: $cardName Scratch Card",
        paymentMethod = "WALLET",
        referenceNumber = record.id.take(8)
      )
      dao.insertTransaction(winTxn)
    }
  }

  // Super Admin Methods
  suspend fun adminCreateDraw(
    title: String,
    prizePoolPkr: Long,
    ticketPricePkr: Long,
    drawTime: Long,
    categoryBadge: String
  ) {
    val id = "DRW-" + (7000..9999).random()
    val draw = DrawEntity(
      id = id,
      title = title,
      prizePoolPkr = prizePoolPkr,
      ticketPricePkr = ticketPricePkr,
      status = "ACTIVE",
      drawTime = drawTime,
      categoryBadge = categoryBadge
    )
    dao.insertDraw(draw)
  }

  suspend fun adminUpdateDrawStatus(drawId: String, newStatus: String) {
    val draw = dao.getDrawById(drawId) ?: return
    val updated = draw.copy(status = newStatus)
    dao.updateDraw(updated)
  }

  suspend fun adminAdjustBalance(userId: String, deltaPkr: Long, note: String) {
    val user = dao.getUser(userId).firstOrNull() ?: return
    val newBalance = (user.walletBalancePkr + deltaPkr).coerceAtLeast(0L)
    dao.updateWalletBalance(userId, newBalance)

    val txn = TransactionEntity(
      id = "TXN-" + (10000..99999).random(),
      userId = userId,
      amountPkr = kotlin.math.abs(deltaPkr),
      type = "ADMIN_ADJUSTMENT",
      status = "COMPLETED",
      description = "Admin Balance ${if (deltaPkr >= 0) "Credit" else "Debit"}: $note",
      paymentMethod = "ADMIN"
    )
    dao.insertTransaction(txn)
  }

  // 100% MANUAL WINNER DECLARATION TOOL
  suspend fun adminDeclareWinner(
    drawId: String,
    winnerTicketId: String,
    winningDays: List<Int>,
    winningMonth: Int,
    customPrizePkr: Long?
  ): Result<DrawEntity> {
    val draw = dao.getDrawById(drawId) ?: return Result.failure(Exception("Draw not found"))
    val ticket = dao.getTicketById(winnerTicketId) ?: return Result.failure(Exception("Ticket $winnerTicketId not found"))

    val prizeAmount = customPrizePkr ?: draw.prizePoolPkr
    val now = System.currentTimeMillis()

    // 1. Mark ticket as winner
    val updatedTicket = ticket.copy(
      isWinner = true,
      prizeWonPkr = prizeAmount,
      matchesCount = 7
    )
    dao.updateTicket(updatedTicket)

    // 2. Credit the winner's wallet
    val winnerUser = dao.getUser(ticket.userId).firstOrNull()
    if (winnerUser != null) {
      dao.updateWalletBalance(winnerUser.id, winnerUser.walletBalancePkr + prizeAmount)

      // Insert WIN transaction
      val winTxn = TransactionEntity(
        id = "TXN-" + (10000..99999).random(),
        userId = winnerUser.id,
        amountPkr = prizeAmount,
        type = "WIN",
        status = "COMPLETED",
        description = "Grand Winner Prize for ${draw.title} (Ticket: ${ticket.id})",
        paymentMethod = "LOTTERY_POOL",
        referenceNumber = ticket.id
      )
      dao.insertTransaction(winTxn)
    }

    // 3. Update Draw status to COMPLETED and set public winner info
    val daysJson = winningDays.sorted().joinToString(",") { String.format("%02d", it) }
    val updatedDraw = draw.copy(
      status = "COMPLETED",
      winningDaysJson = daysJson,
      winningMonth = winningMonth,
      winnerTicketId = ticket.id,
      winnerUserId = ticket.userId,
      winnerUserName = ticket.userName,
      winnerPrizeAmountPkr = prizeAmount,
      declaredAt = now,
      categoryBadge = "OFFICIAL RESULT"
    )
    dao.updateDraw(updatedDraw)

    return Result.success(updatedDraw)
  }
}
