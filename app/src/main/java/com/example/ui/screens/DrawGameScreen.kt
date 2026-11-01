package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DrawEntity
import com.example.data.entity.TicketEntity
import com.example.ui.LotteryViewModel
import com.example.ui.components.AppLanguage
import com.example.ui.components.CountdownTimerView
import com.example.ui.components.LanguageManager
import com.example.ui.components.NumberBall
import com.example.ui.theme.AccentRuby
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawGameScreen(
  viewModel: LotteryViewModel,
  draw: DrawEntity?,
  selectedDays: Set<Int>,
  selectedMonth: Int?,
  lineQuantity: Int,
  userBalance: Long,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  val activeDraw = draw ?: return

  var showSuccessDialog by remember { mutableStateOf(false) }
  var purchasedTickets by remember { mutableStateOf<List<TicketEntity>>(emptyList()) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val totalCost = activeDraw.ticketPricePkr * lineQuantity
  val canBuy = selectedDays.size == 6 && selectedMonth != null && userBalance >= totalCost

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(EmeraldDark)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
    }

    // Draw Title & Prize Pool Header Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(16.dp))
          .padding(bottom = 4.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = activeDraw.title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Rs. %,d".format(activeDraw.prizePoolPkr),
            color = GoldBright,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
          )

          Text(
            text = "GRAND JACKPOT PRIZE POOL (PKR)",
            color = GoldDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Live Timer
          CountdownTimerView(targetEpoch = activeDraw.drawTime)
        }
      }
    }

    // Quick Action Bar: Quick Pick & Clear
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { viewModel.quickPickNumbers() },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF133C27),
            contentColor = GoldBright
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
            .testTag("quick_pick_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = GoldBright)
            Text(
              text = LanguageManager.get("quick_pick", currentLanguage),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        OutlinedButton(
          onClick = { viewModel.clearSelection() },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .border(1.dp, Color(0xFF5C2025), RoundedCornerShape(10.dp))
            .testTag("clear_selection_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
            Text(
              text = LanguageManager.get("clear_selection", currentLanguage),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Selected Balls Summary Bar
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071F13)),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "YOUR SELECTION",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextMuted,
              letterSpacing = 1.sp
            )
            Text(
              text = "${selectedDays.size}/6 Days • ${if (selectedMonth != null) "1/1 Month" else "0/1 Month"}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedDays.size == 6 && selectedMonth != null) GoldBright else TextSecondary
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // 6 Day slots
            val sortedList = selectedDays.sorted()
            for (i in 0 until 6) {
              if (i < sortedList.size) {
                NumberBall(number = sortedList[i], isSelected = true, size = 36.dp, fontSize = 12)
              } else {
                EmptyBallSlot(label = "D${i + 1}")
              }
            }

            // Month Slot
            Text(text = "+", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            if (selectedMonth != null) {
              NumberBall(number = selectedMonth, isSelected = true, isMonth = true, size = 36.dp, fontSize = 12)
            } else {
              EmptyBallSlot(label = "M", isMonth = true)
            }
          }
        }
      }
    }

    // Step 1: Pick 6 Days Grid (1 to 31)
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(EmeraldSurface)
          .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = LanguageManager.get("days_pick", currentLanguage),
            color = GoldPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${selectedDays.size}/6 Selected",
            color = if (selectedDays.size == 6) GoldBright else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          maxItemsInEachRow = 7
        ) {
          for (day in 1..31) {
            NumberBall(
              number = day,
              isSelected = selectedDays.contains(day),
              onClick = { viewModel.toggleDaySelection(day) },
              size = 40.dp,
              fontSize = 13,
              testTag = "day_ball_$day"
            )
          }
        }
      }
    }

    // Step 2: Pick 1 Month Grid (1 to 12)
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(EmeraldSurface)
          .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = LanguageManager.get("month_pick", currentLanguage),
            color = AccentRuby,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (selectedMonth != null) "Month $selectedMonth" else "Select 1",
            color = if (selectedMonth != null) Color(0xFFFF8A80) else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          maxItemsInEachRow = 6
        ) {
          for (month in 1..12) {
            NumberBall(
              number = month,
              isSelected = selectedMonth == month,
              onClick = { viewModel.selectMonth(month) },
              isMonth = true,
              size = 42.dp,
              fontSize = 13,
              testTag = "month_ball_$month"
            )
          }
        }
      }
    }

    // Line Quantity Selector
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF092014))
          .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "NUMBER OF LINES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
          )
          Text(
            text = "$lineQuantity Line(s) = Rs. %,d".format(totalCost),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldBright
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          IconButton(
            onClick = { viewModel.setLineQuantity(lineQuantity - 1) },
            enabled = lineQuantity > 1,
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFF133C27))
              .testTag("decrease_line_quantity")
          ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
          }

          Text(
            text = "$lineQuantity",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp)
          )

          IconButton(
            onClick = { viewModel.setLineQuantity(lineQuantity + 1) },
            enabled = lineQuantity < 10,
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFF133C27))
              .testTag("increase_line_quantity")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    // Purchase Bottom CTA Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Total Amount:", color = TextSecondary, fontSize = 13.sp)
            Text(text = "Rs. %,d".format(totalCost), color = GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Black)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Wallet Balance:", color = TextMuted, fontSize = 11.sp)
            Text(
              text = "Rs. %,d".format(userBalance),
              color = if (userBalance >= totalCost) Color(0xFF00E676) else AccentRuby,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              viewModel.purchaseTickets(
                onSuccess = { tickets ->
                  purchasedTickets = tickets
                  showSuccessDialog = true
                },
                onError = { err ->
                  errorMessage = err
                }
              )
            },
            enabled = canBuy,
            colors = ButtonDefaults.buttonColors(
              containerColor = GoldPrimary,
              contentColor = Color(0xFF141000),
              disabledContainerColor = Color(0xFF1E382A),
              disabledContentColor = Color(0xFF4A6B59)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("confirm_buy_ticket_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null)
              Text(
                text = if (!canBuy && userBalance < totalCost) "Insufficient PKR Balance"
                else if (selectedDays.size != 6 || selectedMonth == null) "Pick 6 Days + 1 Month"
                else LanguageManager.get("buy_ticket", currentLanguage) + " (Rs. %,d)".format(totalCost),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Success Dialog
  if (showSuccessDialog) {
    AlertDialog(
      onDismissRequest = { showSuccessDialog = false },
      confirmButton = {
        Button(
          onClick = { showSuccessDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000))
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
          Text(text = "Ticket Confirmed!", color = GoldBright, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
      },
      text = {
        Column {
          Text(
            text = "Your official Pakistan Lottery entry has been placed for ${activeDraw.title}.",
            color = TextSecondary,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          purchasedTickets.forEach { t ->
            Text(
              text = "• Ticket ID: ${t.id} [${t.selectedDaysJson} + M${t.selectedMonth}]",
              color = GoldBright,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      },
      containerColor = EmeraldSurface
    )
  }

  // Error Dialog
  if (errorMessage != null) {
    AlertDialog(
      onDismissRequest = { errorMessage = null },
      confirmButton = {
        Button(
          onClick = { errorMessage = null },
          colors = ButtonDefaults.buttonColors(containerColor = AccentRuby)
        ) {
          Text("OK", color = Color.White)
        }
      },
      title = { Text(text = "Notice", color = AccentRuby, fontWeight = FontWeight.Bold) },
      text = { Text(text = errorMessage ?: "", color = TextPrimary) },
      containerColor = EmeraldSurface
    )
  }
}

@Composable
private fun EmptyBallSlot(label: String, isMonth: Boolean = false) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(36.dp)
      .clip(CircleShape)
      .background(Color(0xFF092014))
      .border(
        1.dp,
        if (isMonth) Color(0xFF4A181C) else Color(0xFF1E5236),
        CircleShape
      )
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted
    )
  }
}
