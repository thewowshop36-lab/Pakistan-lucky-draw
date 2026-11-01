package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.entity.DrawEntity
import com.example.data.entity.TicketEntity
import com.example.data.entity.UserEntity
import com.example.ui.LotteryViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(
  viewModel: LotteryViewModel,
  allDraws: List<DrawEntity>,
  allTickets: List<TicketEntity>,
  allUsers: List<UserEntity>,
  modifier: Modifier = Modifier
) {
  var adminTab by remember { mutableIntStateOf(0) } // 0 = Manual Winner Declaration, 1 = Draw Management, 2 = Users & Finances

  // Winner declaration state
  var selectedDrawId by remember { mutableStateOf(allDraws.firstOrNull { it.status == "ACTIVE" }?.id ?: "") }
  var ticketSearchQuery by remember { mutableStateOf("") }
  var selectedCandidateTicket by remember { mutableStateOf<TicketEntity?>(null) }
  var showConfirmDeclarationDialog by remember { mutableStateOf(false) }

  // Draw Management dialog
  var showCreateDrawDialog by remember { mutableStateOf(false) }

  // User finance dialog
  var selectedUserForAdjust by remember { mutableStateOf<UserEntity?>(null) }

  val tabs = listOf("Winner Selection", "Draw Management", "Users & Finances")

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(EmeraldDark)
  ) {
    // Admin Mode Header
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF260A0D))
        .border(1.dp, Color(0xFFFF5252))
        .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
          Text(text = "SUPER ADMIN DASHBOARD", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Text(text = "100% MANUAL CONTROL", color = GoldBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }

    TabRow(
      selectedTabIndex = adminTab,
      containerColor = Color(0xFF091F14),
      contentColor = GoldPrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[adminTab]),
          color = GoldPrimary,
          height = 3.dp
        )
      }
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = adminTab == index,
          onClick = { adminTab = index },
          text = {
            Text(
              text = title,
              fontSize = 12.sp,
              fontWeight = if (adminTab == index) FontWeight.Bold else FontWeight.Normal,
              color = if (adminTab == index) GoldBright else TextMuted
            )
          },
          modifier = Modifier.testTag("admin_tab_$index")
        )
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      when (adminTab) {
        0 -> {
          // ================= TAB 0: MANUAL WINNER DECLARATION TOOL =================
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(16.dp))
                .padding(bottom = 4.dp)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = GoldBright)
                  Text(
                    text = "Manual Winner Declaration Tool",
                    color = GoldBright,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Per official lottery regulations, the system NEVER declares winners automatically. You have 100% manual control to select any Ticket ID or trigger a live pool randomizer.",
                  color = TextSecondary,
                  fontSize = 11.sp,
                  lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Select Active Draw Dropdown/Chips
                Text(text = "SELECT DRAW TO DECLARE:", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                val pendingDraws = allDraws.filter { it.status != "COMPLETED" }
                if (pendingDraws.isEmpty()) {
                  Text(text = "No active draws awaiting declaration.", color = TextMuted, fontSize = 12.sp)
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pendingDraws.forEach { d ->
                      val isSelected = (selectedDrawId == d.id)
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(8.dp))
                          .background(if (isSelected) Color(0xFF133C27) else Color(0xFF092014))
                          .border(1.dp, if (isSelected) GoldBright else EmeraldBorder, RoundedCornerShape(8.dp))
                          .clickable {
                            selectedDrawId = d.id
                            selectedCandidateTicket = null
                          }
                          .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column {
                          Text(text = d.title, color = if (isSelected) GoldBright else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                          Text(text = "Prize: Rs. %,d • Status: %s".format(d.prizePoolPkr, d.status), color = TextSecondary, fontSize = 10.sp)
                        }
                        if (isSelected) {
                          Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          val activeDrawForDeclaration = allDraws.find { it.id == selectedDrawId }
          val poolTickets = allTickets.filter { it.drawId == selectedDrawId }

          if (activeDrawForDeclaration != null) {
            // Draw Pool Stats
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF071F13))
                  .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = "TICKETS IN POOL", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                  Text(text = "${poolTickets.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = GoldBright)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = "PRIZE POOL (PKR)", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                  Text(text = "Rs. %,d".format(activeDrawForDeclaration.prizePoolPkr), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
                }
              }
            }

            // Quick Live Randomizer Button
            item {
              Button(
                onClick = {
                  if (poolTickets.isNotEmpty()) {
                    selectedCandidateTicket = poolTickets.random()
                  }
                },
                enabled = poolTickets.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF133C27),
                  contentColor = GoldBright
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
                  .testTag("admin_randomizer_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = GoldBright)
                  Text("Admin-Triggered Live Pool Randomizer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }

            // Selected Candidate Card (Review & Declare)
            selectedCandidateTicket?.let { candidate ->
              item {
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF0E3822)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldBright, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .testTag("candidate_winner_card")
                ) {
                  Column {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(text = "CANDIDATE WINNER SELECTED", color = GoldBright, fontWeight = FontWeight.Black, fontSize = 13.sp)
                      Text(text = candidate.id, color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Winner: ${candidate.userName} (${candidate.userPhone})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Numbers: ${candidate.selectedDaysJson} • Month ${candidate.selectedMonth}", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Prize: Rs. %,d".format(activeDrawForDeclaration.prizePoolPkr), color = GoldBright, fontSize = 16.sp, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                      onClick = { showConfirmDeclarationDialog = true },
                      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("declare_winner_confirm_button")
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null)
                        Text("Confirm Winner & Credit Prize PKR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      }
                    }
                  }
                }
              }
            }

            // Search and Manual Selection from Purchased Pool
            item {
              Column {
                Text(text = "ALL PURCHASED TICKETS IN POOL (${poolTickets.size})", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = ticketSearchQuery,
                  onValueChange = { ticketSearchQuery = it.uppercase() },
                  placeholder = { Text("Search Ticket ID or User Name...", color = TextMuted) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = EmeraldBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                  )
                )
              }
            }

            val filteredPool = poolTickets.filter {
              it.id.contains(ticketSearchQuery, ignoreCase = true) ||
                it.userName.contains(ticketSearchQuery, ignoreCase = true)
            }

            if (filteredPool.isEmpty()) {
              item {
                Text(text = "No tickets found for this draw.", color = TextMuted, fontSize = 12.sp)
              }
            } else {
              items(filteredPool) { ticket ->
                val isChosen = (selectedCandidateTicket?.id == ticket.id)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isChosen) Color(0xFF133C27) else Color(0xFF092014))
                    .border(1.dp, if (isChosen) GoldBright else EmeraldBorder, RoundedCornerShape(10.dp))
                    .clickable { selectedCandidateTicket = ticket }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(text = ticket.id, color = if (isChosen) GoldBright else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${ticket.userName} • ${ticket.userPhone}", color = TextSecondary, fontSize = 11.sp)
                    Text(text = "Numbers: ${ticket.selectedDaysJson} [M${ticket.selectedMonth}]", color = TextMuted, fontSize = 10.sp)
                  }

                  Button(
                    onClick = { selectedCandidateTicket = ticket },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isChosen) GoldPrimary else Color(0xFF113824),
                      contentColor = if (isChosen) Color(0xFF141000) else GoldBright
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                  ) {
                    Text(text = if (isChosen) "Selected" else "Select Winner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        1 -> {
          // ================= TAB 1: DRAW MANAGEMENT =================
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "ACTIVE & UPCOMING DRAWS", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Button(
                onClick = { showCreateDrawDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp).testTag("create_draw_button")
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("New Draw", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          items(allDraws) { draw ->
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
                .padding(bottom = 2.dp)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = draw.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = draw.status,
                    color = when (draw.status) {
                      "ACTIVE" -> Color(0xFF00E676)
                      "PAUSED" -> Color(0xFFFFB74D)
                      else -> TextMuted
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Prize: Rs. %,d • Ticket: Rs. %,d".format(draw.prizePoolPkr, draw.ticketPricePkr), color = GoldBright, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (draw.status == "ACTIVE") {
                    OutlinedButton(
                      onClick = { viewModel.adminUpdateDrawStatus(draw.id, "PAUSED") },
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.size(4.dp))
                      Text("Pause", fontSize = 11.sp)
                    }
                  } else if (draw.status == "PAUSED") {
                    OutlinedButton(
                      onClick = { viewModel.adminUpdateDrawStatus(draw.id, "ACTIVE") },
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.size(4.dp))
                      Text("Activate", fontSize = 11.sp)
                    }
                  }

                  if (draw.status != "COMPLETED") {
                    Button(
                      onClick = {
                        selectedDrawId = draw.id
                        adminTab = 0
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Text("Declare Winner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }

        2 -> {
          // ================= TAB 2: USERS & FINANCES =================
          item {
            Text(text = "REGISTERED USERS & WALLET CONTROL", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          items(allUsers) { user ->
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = user.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  Text(text = "${user.phone} • ${user.role}", color = TextSecondary, fontSize = 11.sp)
                  Text(text = "Wallet: Rs. %,d".format(user.walletBalancePkr), color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }

                Button(
                  onClick = { selectedUserForAdjust = user },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF113824), contentColor = GoldBright),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text("Adjust Balance", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  // Create Draw Dialog
  if (showCreateDrawDialog) {
    CreateDrawModal(
      onDismiss = { showCreateDrawDialog = false },
      onCreate = { title, pool, cost, durationDays, badge ->
        val drawTime = System.currentTimeMillis() + (durationDays * 86400000L)
        viewModel.adminCreateNewDraw(title, pool, cost, drawTime, badge) {
          showCreateDrawDialog = false
        }
      }
    )
  }

  // Adjust User Balance Dialog
  selectedUserForAdjust?.let { user ->
    AdjustBalanceModal(
      user = user,
      onDismiss = { selectedUserForAdjust = null },
      onConfirm = { delta, note ->
        viewModel.adminAdjustUserBalance(user.id, delta, note) {
          selectedUserForAdjust = null
        }
      }
    )
  }

  // Confirm Declaration & Credit Dialog
  if (showConfirmDeclarationDialog && selectedCandidateTicket != null) {
    val cand = selectedCandidateTicket!!
    val activeDraw = allDraws.find { it.id == selectedDrawId }
    val days = cand.selectedDaysJson.split(",").mapNotNull { it.trim().toIntOrNull() }

    AlertDialog(
      onDismissRequest = { showConfirmDeclarationDialog = false },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminDeclareWinner(
              drawId = cand.drawId,
              winnerTicketId = cand.id,
              winningDays = days,
              winningMonth = cand.selectedMonth,
              customPrizePkr = activeDraw?.prizePoolPkr,
              onSuccess = {
                showConfirmDeclarationDialog = false
                selectedCandidateTicket = null
              },
              onError = { /* handled in VM */ }
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000))
        ) {
          Text("Confirm & Credit Prize", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showConfirmDeclarationDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      title = { Text("Publish & Credit Winner", color = GoldBright, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Are you sure you want to declare this ticket as the official winner?", color = TextPrimary, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          Text("• Winner: ${cand.userName} (${cand.userPhone})", color = TextSecondary, fontSize = 12.sp)
          Text("• Ticket: ${cand.id}", color = TextSecondary, fontSize = 12.sp)
          Text("• Prize to Credit: Rs. %,d".format(activeDraw?.prizePoolPkr ?: 0L), color = GoldBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))
          Text("This action will immediately transfer the PKR balance to the winner's wallet and publish the results publicly.", color = TextMuted, fontSize = 11.sp)
        }
      },
      containerColor = EmeraldSurface
    )
  }
}

@Composable
private fun CreateDrawModal(
  onDismiss: () -> Unit,
  onCreate: (String, Long, Long, Int, String) -> Unit
) {
  var title by remember { mutableStateOf("Pakistan Mega Fortune Draw") }
  var prizePoolStr by remember { mutableStateOf("25000000") }
  var ticketCostStr by remember { mutableStateOf("500") }
  var durationDays by remember { mutableIntStateOf(3) }
  var categoryBadge by remember { mutableStateOf("SPECIAL BUMPER") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
        .padding(2.dp)
        .testTag("create_draw_modal")
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Create Lucky Draw", color = GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Black)
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Draw Title", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = EmeraldBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = prizePoolStr,
          onValueChange = { prizePoolStr = it.filter { ch -> ch.isDigit() } },
          label = { Text("Prize Pool (PKR)", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = EmeraldBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = ticketCostStr,
          onValueChange = { ticketCostStr = it.filter { ch -> ch.isDigit() } },
          label = { Text("Ticket Price (PKR)", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = EmeraldBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            val pool = prizePoolStr.toLongOrNull() ?: 10000000L
            val cost = ticketCostStr.toLongOrNull() ?: 500L
            onCreate(title, pool, cost, durationDays, categoryBadge)
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("submit_create_draw_button")
        ) {
          Text("Launch Draw", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun AdjustBalanceModal(
  user: UserEntity,
  onDismiss: () -> Unit,
  onConfirm: (Long, String) -> Unit
) {
  var isCredit by remember { mutableStateOf(true) }
  var amountStr by remember { mutableStateOf("5000") }
  var note by remember { mutableStateOf("Admin Bonus") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
        .padding(2.dp)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Adjust User Balance", color = GoldBright, fontSize = 17.sp, fontWeight = FontWeight.Bold)
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Text("User: ${user.name} (${user.phone})", color = TextSecondary, fontSize = 12.sp)
        Text("Current: Rs. %,d".format(user.walletBalancePkr), color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { isCredit = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isCredit) Color(0xFF00C853) else Color(0xFF113824)
            ),
            modifier = Modifier.weight(1f)
          ) {
            Text("Credit (+)")
          }
          Button(
            onClick = { isCredit = false },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (!isCredit) Color(0xFFFF5252) else Color(0xFF113824)
            ),
            modifier = Modifier.weight(1f)
          ) {
            Text("Debit (-)")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
          label = { Text("Amount in PKR", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = EmeraldBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Adjustment Reason / Note", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = EmeraldBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            val amt = amountStr.toLongOrNull() ?: 0L
            val signed = if (isCredit) amt else -amt
            onConfirm(signed, note)
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Apply Adjustment", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
