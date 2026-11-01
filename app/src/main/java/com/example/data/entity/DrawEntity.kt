package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draws")
data class DrawEntity(
  @PrimaryKey val id: String,
  val title: String,
  val prizePoolPkr: Long,
  val ticketPricePkr: Long,
  val status: String = "ACTIVE", // "ACTIVE", "PAUSED", "COMPLETED"
  val drawTime: Long, // Epoch timestamp of draw time
  val winningDaysJson: String = "", // e.g. "04,11,18,22,27,31"
  val winningMonth: Int = 0, // e.g. 8
  val winnerTicketId: String = "",
  val winnerUserId: String = "",
  val winnerUserName: String = "",
  val winnerPrizeAmountPkr: Long = 0,
  val declaredAt: Long = 0,
  val categoryBadge: String = "GRAND BUMPER"
)
