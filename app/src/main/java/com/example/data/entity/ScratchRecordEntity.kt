package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scratch_records")
data class ScratchRecordEntity(
  @PrimaryKey val id: String,
  val cardName: String,
  val costPkr: Long,
  val prizeWonPkr: Long,
  val isWin: Boolean,
  val playedAt: Long = System.currentTimeMillis()
)
