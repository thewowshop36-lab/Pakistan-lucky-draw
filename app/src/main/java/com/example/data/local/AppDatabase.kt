package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.entity.DrawEntity
import com.example.data.entity.ScratchRecordEntity
import com.example.data.entity.TicketEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    UserEntity::class,
    DrawEntity::class,
    TicketEntity::class,
    TransactionEntity::class,
    ScratchRecordEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun lotteryDao(): LotteryDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "pakistan_lottery_db"
        )
          .addCallback(AppDatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class AppDatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database.lotteryDao())
          }
        }
      }

      suspend fun populateInitialData(dao: LotteryDao) {
        val now = System.currentTimeMillis()
        val twoDaysLater = now + (2 * 24 * 60 * 60 * 1000L)
        val eightHoursLater = now + (8 * 60 * 60 * 1000L)
        val fourDaysLater = now + (4 * 24 * 60 * 60 * 1000L)
        val yesterday = now - (1 * 24 * 60 * 60 * 1000L)

        // 1. Initial Users
        val mainUser = UserEntity(
          id = "usr_001",
          name = "Muhammad Zeeshan",
          phone = "+92 300 7861122",
          email = "zeeshan.pk@gmail.com",
          walletBalancePkr = 15000L,
          role = "USER",
          createdAt = now - 864000000L
        )

        val adminUser = UserEntity(
          id = "usr_admin",
          name = "Super Admin (Karachi HQ)",
          phone = "+92 321 0000000",
          email = "admin@paklottery.gov.pk",
          walletBalancePkr = 50000000L,
          role = "ADMIN",
          createdAt = now - 999999999L
        )

        val user2 = UserEntity(
          id = "usr_002",
          name = "Ali Hamza",
          phone = "+92 333 4567890",
          email = "ali.hamza@lahore.pk",
          walletBalancePkr = 4500L,
          role = "USER",
          createdAt = now - 500000000L
        )

        val user3 = UserEntity(
          id = "usr_003",
          name = "Fatima Noor",
          phone = "+92 345 8899001",
          email = "fatima.noor@isb.pk",
          walletBalancePkr = 8200L,
          role = "USER",
          createdAt = now - 300000000L
        )

        dao.insertUser(mainUser)
        dao.insertUser(adminUser)
        dao.insertUser(user2)
        dao.insertUser(user3)

        // 2. Initial Draws
        val activeDraw1 = DrawEntity(
          id = "DRW-7781",
          title = "Pakistan Grand Bumper 2026",
          prizePoolPkr = 75000000L, // 75 Million PKR
          ticketPricePkr = 1000L,
          status = "ACTIVE",
          drawTime = twoDaysLater,
          categoryBadge = "MEGA BUMPER"
        )

        val activeDraw2 = DrawEntity(
          id = "DRW-7782",
          title = "Daily Super 7 Lucky Draw",
          prizePoolPkr = 10000000L, // 10 Million PKR
          ticketPricePkr = 500L,
          status = "ACTIVE",
          drawTime = eightHoursLater,
          categoryBadge = "DAILY DRAW"
        )

        val activeDraw3 = DrawEntity(
          id = "DRW-7783",
          title = "Shaheen Fortune Bumper",
          prizePoolPkr = 35000000L, // 35 Million PKR
          ticketPricePkr = 750L,
          status = "ACTIVE",
          drawTime = fourDaysLater,
          categoryBadge = "WEEKLY SPECIAL"
        )

        val completedDraw = DrawEntity(
          id = "DRW-7780",
          title = "Quaid-e-Azam Memorial Draw",
          prizePoolPkr = 50000000L,
          ticketPricePkr = 1000L,
          status = "COMPLETED",
          drawTime = yesterday,
          winningDaysJson = "05,12,19,23,28,31",
          winningMonth = 8,
          winnerTicketId = "PKL-2026-78401",
          winnerUserId = "usr_002",
          winnerUserName = "Ali Hamza (Lahore)",
          winnerPrizeAmountPkr = 50000000L,
          declaredAt = yesterday + 3600000L,
          categoryBadge = "OFFICIAL RESULT"
        )

        dao.insertDraws(listOf(activeDraw1, activeDraw2, activeDraw3, completedDraw))

        // 3. Pre-populated tickets (enabling Admin manual winner review immediately)
        val ticket1 = TicketEntity(
          id = "PKL-2026-78401",
          drawId = "DRW-7780",
          drawTitle = "Quaid-e-Azam Memorial Draw",
          userId = "usr_002",
          userName = "Ali Hamza",
          userPhone = "+92 333 4567890",
          selectedDaysJson = "05,12,19,23,28,31",
          selectedMonth = 8,
          purchaseDate = yesterday - 120000L,
          pricePkr = 1000L,
          isWinner = true,
          prizeWonPkr = 50000000L,
          matchesCount = 7
        )

        val ticket2 = TicketEntity(
          id = "PKL-2026-89102",
          drawId = "DRW-7781",
          drawTitle = "Pakistan Grand Bumper 2026",
          userId = "usr_001",
          userName = "Muhammad Zeeshan",
          userPhone = "+92 300 7861122",
          selectedDaysJson = "07,14,21,22,29,30",
          selectedMonth = 3,
          purchaseDate = now - 7200000L,
          pricePkr = 1000L,
          isWinner = false,
          prizeWonPkr = 0L,
          matchesCount = 0
        )

        val ticket3 = TicketEntity(
          id = "PKL-2026-89103",
          drawId = "DRW-7781",
          drawTitle = "Pakistan Grand Bumper 2026",
          userId = "usr_003",
          userName = "Fatima Noor",
          userPhone = "+92 345 8899001",
          selectedDaysJson = "03,09,15,18,24,31",
          selectedMonth = 11,
          purchaseDate = now - 3600000L,
          pricePkr = 1000L,
          isWinner = false,
          prizeWonPkr = 0L,
          matchesCount = 0
        )

        val ticket4 = TicketEntity(
          id = "PKL-2026-89104",
          drawId = "DRW-7781",
          drawTitle = "Pakistan Grand Bumper 2026",
          userId = "usr_002",
          userName = "Ali Hamza",
          userPhone = "+92 333 4567890",
          selectedDaysJson = "02,11,17,20,26,28",
          selectedMonth = 6,
          purchaseDate = now - 1800000L,
          pricePkr = 1000L,
          isWinner = false,
          prizeWonPkr = 0L,
          matchesCount = 0
        )

        dao.insertTickets(listOf(ticket1, ticket2, ticket3, ticket4))

        // 4. Initial Transactions
        val txn1 = TransactionEntity(
          id = "TXN-88201",
          userId = "usr_001",
          amountPkr = 16000L,
          type = "DEPOSIT",
          status = "COMPLETED",
          description = "EasyPaisa Fast Top-up",
          paymentMethod = "EASYPAISA",
          referenceNumber = "EP-998241029",
          createdAt = now - 86400000L
        )

        val txn2 = TransactionEntity(
          id = "TXN-88202",
          userId = "usr_001",
          amountPkr = 1000L,
          type = "PURCHASE",
          status = "COMPLETED",
          description = "1x Grand Bumper Entry (Ticket: PKL-2026-89102)",
          paymentMethod = "WALLET",
          referenceNumber = "PKL-2026-89102",
          createdAt = now - 7200000L
        )

        dao.insertTransactions(listOf(txn1, txn2))
      }
    }
  }
}
