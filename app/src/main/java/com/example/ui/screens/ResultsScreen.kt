package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.AppLanguage
import com.example.ui.components.LanguageManager
import com.example.ui.components.NumberBall
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultsScreen(
  completedDraws: List<DrawEntity>,
  allTickets: List<TicketEntity>,
  userTickets: List<TicketEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var searchTicketQuery by remember { mutableStateOf("") }
  var ticketCheckResult by remember { mutableStateOf<Pair<TicketEntity, DrawEntity?>?>(null) }
  var searchError by remember { mutableStateOf<String?>(null) }

  val tabs = listOf(
    if (currentLanguage == AppLanguage.EN) "Draw Results" else "ڈرا نتائج",
    if (currentLanguage == AppLanguage.EN) "Winners Board" else "فاتحین بورڈ",
    if (currentLanguage == AppLanguage.EN) "Ticket Checker" else "ٹکٹ چیکر"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(EmeraldDark)
  ) {
    // Top Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color(0xFF061B10),
      contentColor = GoldPrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = GoldPrimary,
          height = 3.dp
        )
      }
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = {
            Text(
              text = title,
              fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
              fontSize = 13.sp,
              color = if (selectedTab == index) GoldBright else TextMuted
            )
          },
          modifier = Modifier.testTag("results_tab_$index")
        )
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      when (selectedTab) {
        0 -> {
          // Tab 0: Past Draw Results
          if (completedDraws.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(40.dp),
                contentAlignment = Alignment.Center
              ) {
                Text("No completed draws yet. Check back after live draw!", color = TextSecondary)
              }
            }
          } else {
            items(completedDraws) { draw ->
              CompletedDrawResultCard(draw = draw)
            }
          }
        }

        1 -> {
          // Tab 1: Public Winners Transparency Board
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF071F13)),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldBright)
                Text(
                  text = "Official Public Transparency Record (All Winners Guaranteed & Paid in PKR)",
                  fontSize = 11.sp,
                  color = GoldBright,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          // Pre-baked winners + dynamic winners from completed draws
          val allWinners = completedDraws.filter { it.winnerTicketId.isNotEmpty() }
          items(allWinners) { draw ->
            WinnerTransparencyCard(
              winnerName = maskName(draw.winnerUserName),
              ticketId = draw.winnerTicketId,
              drawTitle = draw.title,
              prizePkr = draw.winnerPrizeAmountPkr,
              drawDate = draw.declaredAt
            )
          }

          // Also sample historical winners
          item {
            WinnerTransparencyCard(
              winnerName = "Ali*** H*** (Lahore)",
              ticketId = "PKL-2026-78401",
              drawTitle = "Quaid-e-Azam Memorial Draw",
              prizePkr = 50000000L,
              drawDate = System.currentTimeMillis() - 86400000L
            )
          }

          item {
            WinnerTransparencyCard(
              winnerName = "Babar*** A*** (Rawalpindi)",
              ticketId = "PKL-2026-66319",
              drawTitle = "Super 7 Daily Draw",
              prizePkr = 10000000L,
              drawDate = System.currentTimeMillis() - 172800000L
            )
          }

          item {
            WinnerTransparencyCard(
              winnerName = "Zahid*** K*** (Karachi)",
              ticketId = "PKL-2026-55104",
              drawTitle = "Shaheen Fortune Bumper",
              prizePkr = 35000000L,
              drawDate = System.currentTimeMillis() - 259200000L
            )
          }
        }

        2 -> {
          // Tab 2: Ticket Checker
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EmeraldSurface)
                .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
            ) {
              Text(
                text = "Instant Ticket Verifier",
                color = GoldPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Enter your Ticket ID (e.g. PKL-2026-78401) to verify winning status against official draws.",
                color = TextSecondary,
                fontSize = 12.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              OutlinedTextField(
                value = searchTicketQuery,
                onValueChange = { searchTicketQuery = it.uppercase() },
                label = { Text("Ticket ID", color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("ticket_checker_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GoldPrimary,
                  unfocusedBorderColor = EmeraldBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )

              Spacer(modifier = Modifier.height(10.dp))

              Button(
                onClick = {
                  val query = searchTicketQuery.trim()
                  val foundTicket = allTickets.find { it.id.equals(query, ignoreCase = true) }
                  if (foundTicket != null) {
                    val matchingDraw = completedDraws.find { it.id == foundTicket.drawId }
                    ticketCheckResult = Pair(foundTicket, matchingDraw)
                    searchError = null
                  } else {
                    ticketCheckResult = null
                    searchError = "No ticket found matching ID '$query'."
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("verify_ticket_button")
              ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Check Ticket Status", fontWeight = FontWeight.Bold)
              }

              if (searchError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = searchError ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
              }
            }
          }

          // Verified Ticket Card display
          ticketCheckResult?.let { (ticket, draw) ->
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (ticket.isWinner) Color(0xFF0D3820) else Color(0xFF14241B)),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    1.5.dp,
                    if (ticket.isWinner) GoldBright else EmeraldBorder,
                    RoundedCornerShape(16.dp)
                  )
                  .padding(14.dp)
              ) {
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = "Ticket: ${ticket.id}", color = GoldBright, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(
                      text = if (ticket.isWinner) "WINNER! ⭐" else if (draw != null) "Unmatched" else "Draw Pending",
                      color = if (ticket.isWinner) Color(0xFF00E676) else TextMuted,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  }

                  Spacer(modifier = Modifier.height(6.dp))
                  Text(text = "Draw: ${ticket.drawTitle}", color = TextSecondary, fontSize = 12.sp)

                  if (ticket.isWinner) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "PRIZE WON: Rs. %,d".format(ticket.prizeWonPkr),
                      color = GoldBright,
                      fontWeight = FontWeight.Black,
                      fontSize = 18.sp
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))
                  Text(text = "Selected Numbers: ${ticket.selectedDaysJson} + Month ${ticket.selectedMonth}", color = TextPrimary, fontSize = 12.sp)
                }
              }
            }
          }

          // Quick My Purchased Tickets list
          if (userTickets.isNotEmpty()) {
            item {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "YOUR PURCHASED TICKETS",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
            }

            items(userTickets) { t ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF092014))
                  .border(1.dp, EmeraldBorder, RoundedCornerShape(10.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = t.id, color = GoldBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(text = "${t.selectedDaysJson} • Month ${t.selectedMonth}", color = TextSecondary, fontSize = 11.sp)
                }

                Button(
                  onClick = {
                    searchTicketQuery = t.id
                    val matchingDraw = completedDraws.find { it.id == t.drawId }
                    ticketCheckResult = Pair(t, matchingDraw)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF133C27), contentColor = GoldBright),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(30.dp)
                ) {
                  Text("Check", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompletedDrawResultCard(draw: DrawEntity) {
  val days = draw.winningDaysJson.split(",").mapNotNull { it.trim().toIntOrNull() }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(16.dp))
      .padding(bottom = 6.dp)
      .testTag("completed_draw_${draw.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = draw.title,
          color = TextPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "OFFICIAL DRAW",
          color = Color(0xFF00E676),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0C2B1B))
            .border(0.5.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Prize Pool: Rs. %,d".format(draw.prizePoolPkr),
        color = GoldBright,
        fontSize = 17.sp,
        fontWeight = FontWeight.Black
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "WINNING NUMBERS (6 DAYS + 1 MONTH)",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 0.5.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        days.forEach { num ->
          NumberBall(number = num, isSelected = true, size = 34.dp, fontSize = 11)
        }
        Text(text = "+", color = GoldPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        NumberBall(number = draw.winningMonth, isSelected = true, isMonth = true, size = 34.dp, fontSize = 11)
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Winner details
      if (draw.winnerUserName.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF071F13))
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = "1ST PRIZE WINNER", fontSize = 9.sp, color = GoldDark, fontWeight = FontWeight.Bold)
            Text(text = draw.winnerUserName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(text = "Ticket: ${draw.winnerTicketId}", fontSize = 10.sp, color = TextMuted)
            Text(text = "Rs. %,d".format(draw.winnerPrizeAmountPkr), fontSize = 13.sp, fontWeight = FontWeight.Black, color = GoldBright)
          }
        }
      }
    }
  }
}

@Composable
private fun WinnerTransparencyCard(
  winnerName: String,
  ticketId: String,
  drawTitle: String,
  prizePkr: Long,
  drawDate: Long
) {
  val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(drawDate))

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
      .padding(bottom = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF143B27))
            .border(1.dp, GoldPrimary, CircleShape)
        ) {
          Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
        }

        Column {
          Text(text = winnerName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text(text = drawTitle, color = TextMuted, fontSize = 11.sp)
          Text(text = "Ticket: $ticketId • $dateStr", color = TextSecondary, fontSize = 10.sp)
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(text = "Rs. %,d".format(prizePkr), color = GoldBright, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(text = "PAID (PKR)", color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

private fun maskName(name: String): String {
  if (name.length <= 4) return name
  val parts = name.split(" ")
  return parts.joinToString(" ") { p ->
    if (p.length > 2) p.take(2) + "***" else p
  }
}
