package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
  @PrimaryKey val id: String, // e.g. "PKL-2026-9042"
  val drawId: String,
  val drawTitle: String,
  val userId: String,
  val userName: String,
  val userPhone: String,
  val selectedDaysJson: String, // e.g. "03,09,14,21,25,30" (6 numbers from 1 to 31)
  val selectedMonth: Int, // 1 to 12
  val purchaseDate: Long = System.currentTimeMillis(),
  val pricePkr: Long,
  val isWinner: Boolean = false,
  val prizeWonPkr: Long = 0,
  val matchesCount: Int = 0
)
