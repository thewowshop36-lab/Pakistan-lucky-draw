package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ScratchRecordEntity
import com.example.ui.LotteryViewModel
import com.example.ui.ScratchCardDef
import com.example.ui.components.AppLanguage
import com.example.ui.components.LanguageManager
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.random.Random

@Composable
fun ScratchCardsScreen(
  viewModel: LotteryViewModel,
  userBalance: Long,
  scratchRecords: List<ScratchRecordEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  var activeCardForPlay by remember { mutableStateOf<ScratchCardDef?>(null) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(EmeraldDark)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
    }

    // Header info
    item {
      Column {
        Text(
          text = LanguageManager.get("nav_scratch", currentLanguage),
          color = GoldPrimary,
          fontSize = 22.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = if (currentLanguage == AppLanguage.EN) "Instant Win Digital Scratch Cards • Match 3 Symbols to Win PKR"
          else "فوری سکریچ کارڈز • انعامات جیتنے کے لیے 3 علامتیں ملائیں",
          color = TextSecondary,
          fontSize = 12.sp
        )
      }
    }

    // Grid of 4 Scratch Cards
    items(viewModel.scratchCards) { card ->
      ScratchCardListingItem(
        card = card,
        userBalance = userBalance,
        onPlay = { activeCardForPlay = card }
      )
    }

    // Recent Scratch Winnings Log
    if (scratchRecords.isNotEmpty()) {
      item {
        Text(
          text = "YOUR SCRATCH HISTORY",
          color = GoldPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      items(scratchRecords.take(5)) { record ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF071F13))
            .border(1.dp, EmeraldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = record.cardName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
              text = if (record.isWin) "WON: Rs. %,d".format(record.prizeWonPkr) else "Better luck next time",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (record.isWin) Color(0xFF00E676) else TextMuted
            )
          }

          Text(
            text = "Cost: Rs. %,d".format(record.costPkr),
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // Interactive Scratch Modal
  activeCardForPlay?.let { card ->
    InteractiveScratchDialog(
      card = card,
      userBalance = userBalance,
      onDismiss = { activeCardForPlay = null },
      onConfirmPlay = { mult, onDone ->
        viewModel.executeScratchResult(card, mult) { wonPrize, isWin ->
          onDone(wonPrize, isWin)
        }
      }
    )
  }
}

@Composable
private fun ScratchCardListingItem(
  card: ScratchCardDef,
  userBalance: Long,
  onPlay: () -> Unit
) {
  val canAfford = userBalance >= card.pricePkr

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        1.2.dp,
        Brush.horizontalGradient(listOf(GoldBright, Color(card.colorHex), EmeraldBorder)),
        RoundedCornerShape(18.dp)
      )
      .testTag("scratch_card_item_${card.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(card.colorHex))
            .border(1.dp, GoldBright, RoundedCornerShape(14.dp))
        ) {
          Text(text = card.symbols.first(), fontSize = 26.sp)
        }

        Column {
          Text(text = card.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          Text(text = card.subtitle, color = TextMuted, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Win Up To: Rs. %,d".format(card.maxPrizePkr),
            color = GoldBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "Rs. %,d".format(card.pricePkr),
          color = TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = onPlay,
          enabled = canAfford,
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldPrimary,
            contentColor = Color(0xFF141000)
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .height(36.dp)
            .testTag("play_scratch_${card.id}")
        ) {
          Text(text = "Scratch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun InteractiveScratchDialog(
  card: ScratchCardDef,
  userBalance: Long,
  onDismiss: () -> Unit,
  onConfirmPlay: (Int, (Long, Boolean) -> Unit) -> Unit
) {
  // Pre-generate grid of 9 cells
  // Randomly determine if this ticket is a winner (approx 45% win chance)
  var isTicketDrawn by remember { mutableStateOf(false) }
  var isGameConcluded by remember { mutableStateOf(false) }
  var winAmountPkr by remember { mutableStateOf(0L) }
  var isWin by remember { mutableStateOf(false) }

  // 9 grid cells
  val revealedCells = remember { mutableStateListOf(false, false, false, false, false, false, false, false, false) }
  val cellSymbols = remember {
    mutableStateListOf<String>().apply {
      // 3 matching or mixed
      val winRoll = Random.nextInt(100)
      if (winRoll < 45) {
        val winningSymbol = card.symbols.random()
        val others = (card.symbols - winningSymbol).shuffled()
        addAll(listOf(winningSymbol, winningSymbol, winningSymbol, others[0], others[1], others[2], others[0], others[1], others[2]).shuffled())
      } else {
        addAll((card.symbols + card.symbols).shuffled().take(9))
      }
    }
  }

  // Count if 3 matched
  fun checkWinCondition() {
    val symbolCounts = cellSymbols.groupingBy { it }.eachCount()
    val has3Match = symbolCounts.any { it.value >= 3 }
    val multiplier = if (has3Match) {
      listOf(1, 2, 5, 10, 50).random()
    } else {
      0
    }

    onConfirmPlay(multiplier) { prize, won ->
      winAmountPkr = prize
      isWin = won
      isGameConcluded = true
    }
  }

  fun scratchAll() {
    for (i in 0 until 9) {
      revealedCells[i] = true
    }
    if (!isGameConcluded) {
      checkWinCondition()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
        .padding(2.dp)
        .testTag("interactive_scratch_dialog")
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = card.title, color = GoldBright, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(text = "Cost: Rs. %,d • Match 3 Symbols".format(card.pricePkr), color = TextSecondary, fontSize = 11.sp)
          }

          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3x3 Scratch Foil Grid
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF04180E))
            .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (row in 0..2) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              for (col in 0..2) {
                val index = row * 3 + col
                ScratchCell(
                  symbol = cellSymbols[index],
                  isRevealed = revealedCells[index],
                  onClick = {
                    if (!revealedCells[index]) {
                      revealedCells[index] = true
                      // Check if all revealed
                      if (revealedCells.all { it } && !isGameConcluded) {
                        checkWinCondition()
                      }
                    }
                  },
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Instructions or Win Notification
        AnimatedVisibility(visible = isGameConcluded) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isWin) Color(0xFF0A3A20) else Color(0xFF261214))
              .border(1.dp, if (isWin) Color(0xFF00E676) else Color(0xFFFF5252), RoundedCornerShape(10.dp))
              .padding(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.Casino,
                contentDescription = null,
                tint = if (isWin) GoldBright else Color(0xFFFF8A80)
              )
              Text(
                text = if (isWin) "YOU WON Rs. %,d!".format(winAmountPkr) else "No 3 Matches - Try Again!",
                color = if (isWin) GoldBright else Color(0xFFFFCDD2),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
              )
            }
            if (isWin) {
              Text(text = "Prize credited to your wallet balance!", color = TextSecondary, fontSize = 11.sp)
            }
          }
        }

        if (!isGameConcluded) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
            Text(
              text = "Tap or scratch foil cells to reveal symbols",
              color = TextMuted,
              fontSize = 11.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Scratch All & Close/Replay
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (!isGameConcluded) {
            Button(
              onClick = { scratchAll() },
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = Color(0xFF141000)
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("scratch_all_button")
            ) {
              Text("Reveal All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          } else {
            Button(
              onClick = onDismiss,
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("close_scratch_button")
            ) {
              Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ScratchCell(
  symbol: String,
  isRevealed: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .height(72.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(enabled = !isRevealed, onClick = onClick)
      .then(
        if (isRevealed) Modifier.background(Color(0xFF0F3622))
        else Modifier.background(Brush.linearGradient(listOf(Color(0xFF8A9A90), Color(0xFF556B5D))))
      )
      .border(
        1.dp,
        if (isRevealed) GoldBright else Color(0xFFA5B8AC),
        RoundedCornerShape(10.dp)
      )
      .testTag("scratch_cell")
  ) {
    if (isRevealed) {
      Text(
        text = symbol,
        fontSize = 28.sp,
        textAlign = TextAlign.Center
      )
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "🪙", fontSize = 16.sp)
        Text(text = "SCRATCH", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF192A20))
      }
    }
  }
}
