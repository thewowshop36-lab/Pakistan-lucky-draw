package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.DrawEntity
import com.example.data.entity.ScratchRecordEntity
import com.example.data.entity.TicketEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LotteryDao {

  // Users
  @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
  fun getUser(id: String): Flow<UserEntity?>

  @Query("SELECT * FROM users ORDER BY createdAt DESC")
  fun getAllUsers(): Flow<List<UserEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Query("UPDATE users SET walletBalancePkr = :newBalance WHERE id = :userId")
  suspend fun updateWalletBalance(userId: String, newBalance: Long)

  // Draws
  @Query("SELECT * FROM draws ORDER BY drawTime ASC")
  fun getAllDraws(): Flow<List<DrawEntity>>

  @Query("SELECT * FROM draws WHERE status = 'ACTIVE' ORDER BY drawTime ASC")
  fun getActiveDraws(): Flow<List<DrawEntity>>

  @Query("SELECT * FROM draws WHERE status = 'COMPLETED' ORDER BY declaredAt DESC")
  fun getCompletedDraws(): Flow<List<DrawEntity>>

  @Query("SELECT * FROM draws WHERE id = :id LIMIT 1")
  suspend fun getDrawById(id: String): DrawEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDraw(draw: DrawEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDraws(draws: List<DrawEntity>)

  @Update
  suspend fun updateDraw(draw: DrawEntity)

  // Tickets
  @Query("SELECT * FROM tickets ORDER BY purchaseDate DESC")
  fun getAllTickets(): Flow<List<TicketEntity>>

  @Query("SELECT * FROM tickets WHERE drawId = :drawId ORDER BY purchaseDate DESC")
  fun getTicketsForDraw(drawId: String): Flow<List<TicketEntity>>

  @Query("SELECT * FROM tickets WHERE userId = :userId ORDER BY purchaseDate DESC")
  fun getTicketsForUser(userId: String): Flow<List<TicketEntity>>

  @Query("SELECT * FROM tickets WHERE id = :ticketId LIMIT 1")
  suspend fun getTicketById(ticketId: String): TicketEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTicket(ticket: TicketEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTickets(tickets: List<TicketEntity>)

  @Update
  suspend fun updateTicket(ticket: TicketEntity)

  // Transactions
  @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC")
  fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransactions(transactions: List<TransactionEntity>)

  @Query("UPDATE transactions SET status = :status WHERE id = :id")
  suspend fun updateTransactionStatus(id: String, status: String)

  // Scratch Records
  @Query("SELECT * FROM scratch_records ORDER BY playedAt DESC")
  fun getAllScratchRecords(): Flow<List<ScratchRecordEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertScratchRecord(record: ScratchRecordEntity)
}
