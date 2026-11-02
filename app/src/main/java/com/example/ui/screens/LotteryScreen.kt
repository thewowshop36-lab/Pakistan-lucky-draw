package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DrawEntity
import com.example.data.entity.ScratchRecordEntity
import com.example.ui.LotteryViewModel
import com.example.ui.components.AppLanguage
import com.example.ui.components.CountdownTimerView
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LotteryScreen(
  viewModel: LotteryViewModel,
  activeDraws: List<DrawEntity>,
  selectedDraw: DrawEntity?,
  selectedDays: Set<Int>,
  selectedMonth: Int?,
  lineQuantity: Int,
  userBalance: Long,
  scratchRecords: List<ScratchRecordEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Number Draws (6+1), 1 = Instant Scratch Cards

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(EmeraldDark)
  ) {
    // Top Sub-tabs for Lottery Hub
    TabRow(
      selectedTabIndex = activeSubTab,
      containerColor = Color(0xFF051A0F),
      contentColor = GoldPrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
          color = GoldPrimary,
          height = 3.dp
        )
      }
    ) {
      Tab(
        selected = activeSubTab == 0,
        onClick = { activeSubTab = 0 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
              text = if (currentLanguage == AppLanguage.EN) "6+1 Lucky Draws" else "6+1 لکی ڈراز",
              fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Normal,
              fontSize = 13.sp
            )
          }
        },
        modifier = Modifier.testTag("lottery_tab_draws")
      )

      Tab(
        selected = activeSubTab == 1,
        onClick = { activeSubTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
              text = if (currentLanguage == AppLanguage.EN) "Scratch Games" else "سکریچ گیمز",
              fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Normal,
              fontSize = 13.sp
            )
          }
        },
        modifier = Modifier.testTag("lottery_tab_scratch")
      )
    }

    if (activeSubTab == 0) {
      // 6+1 Lucky Draws Section
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(4.dp))
        }

        // Available Lotteries Selector Bar
        item {
          Column {
            Text(
              text = if (currentLanguage == AppLanguage.EN) "SELECT LOTTERY GAME" else "لاٹری کا انتخاب کریں",
              color = GoldPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              activeDraws.forEach { draw ->
                val isSelected = (selectedDraw?.id == draw.id)
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF133C27) else Color(0xFF092014)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .border(
                      1.2.dp,
                      if (isSelected) GoldBright else EmeraldBorder,
                      RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.selectDrawForPlay(draw) }
                    .padding(8.dp)
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = draw.categoryBadge,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) GoldBright else TextMuted
                    )
                    Text(
                      text = draw.title.take(16),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary,
                      maxLines = 1
                    )
                    Text(
                      text = "Rs. %,d".format(draw.ticketPricePkr),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Black,
                      color = GoldPrimary
                    )
                  }
                }
              }
            }
          }
        }

        // Interactive 6+1 Number Selection Component for the chosen lottery
        item {
          val active = selectedDraw ?: activeDraws.firstOrNull()
          if (active != null) {
            DrawGameScreen(
              viewModel = viewModel,
              draw = active,
              selectedDays = selectedDays,
              selectedMonth = selectedMonth,
              lineQuantity = lineQuantity,
              userBalance = userBalance,
              currentLanguage = currentLanguage
            )
          } else {
            Text("No active lotteries at the moment.", color = TextMuted)
          }
        }
      }
    } else {
      // Instant Scratch Games Section
      ScratchCardsScreen(
        viewModel = viewModel,
        userBalance = userBalance,
        scratchRecords = scratchRecords,
        currentLanguage = currentLanguage
      )
    }
  }
}
