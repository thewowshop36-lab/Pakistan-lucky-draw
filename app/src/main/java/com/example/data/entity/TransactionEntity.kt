package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey val id: String, // e.g. "TXN-74892"
  val userId: String,
  val amountPkr: Long,
  val type: String, // "DEPOSIT", "PURCHASE", "WIN", "WITHDRAWAL", "ADMIN_ADJUSTMENT"
  val status: String = "COMPLETED", // "COMPLETED", "PENDING", "REJECTED"
  val description: String,
  val paymentMethod: String = "WALLET", // "JAZZCASH", "EASYPAISA", "BANK", "WALLET"
  val referenceNumber: String = "",
  val createdAt: Long = System.currentTimeMillis()
)
