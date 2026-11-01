package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String,
  val name: String,
  val phone: String,
  val email: String,
  val walletBalancePkr: Long,
  val role: String = "USER", // "USER" or "ADMIN"
  val createdAt: Long = System.currentTimeMillis()
)
