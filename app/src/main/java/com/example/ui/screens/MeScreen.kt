package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.TicketEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.ui.LotteryViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeScreen(
  viewModel: LotteryViewModel,
  currentUser: UserEntity?,
  userTickets: List<TicketEntity>,
  transactions: List<TransactionEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  var showDepositModal by remember { mutableStateOf(false) }
  var showWithdrawModal by remember { mutableStateOf(false) }
  var showAdminPinDialog by remember { mutableStateOf(false) }
  var adminPinInput by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }
  var selectedTxFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Deposits, 2=Purchases, 3=Winnings

  val balance = currentUser?.walletBalancePkr ?: 0L

  val filteredTransactions = when (selectedTxFilter) {
    1 -> transactions.filter { it.type == "DEPOSIT" }
    2 -> transactions.filter { it.type == "PURCHASE" }
    3 -> transactions.filter { it.type == "WIN" }
    else -> transactions
  }

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

    // 1. User Profile Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.2.dp, Brush.horizontalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
          .padding(bottom = 2.dp)
          .testTag("me_profile_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(Color(0xFF0F3622))
              .border(1.5.dp, GoldPrimary, CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "User",
              tint = GoldBright,
              modifier = Modifier.size(32.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = currentUser?.name ?: "Muhammad Zeeshan",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF0C2B1B))
                  .border(0.5.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text(text = "VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
              }
            }

            Text(
              text = currentUser?.phone ?: "+92 300 7861122",
              color = TextSecondary,
              fontSize = 12.sp
            )

            Text(
              text = "Account ID: PKL-USR-7862",
              color = TextMuted,
              fontSize = 10.sp
            )
          }
        }
      }
    }

    // 2. Wallet Balance Card (PKR)
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF062215)),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, GoldPrimary, RoundedCornerShape(20.dp))
          .testTag("me_wallet_card")
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = if (currentLanguage == AppLanguage.EN) "AVAILABLE WALLET BALANCE" else "دستیاب والیٹ بیلنس",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Rs. %,d".format(balance),
                color = GoldBright,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0E3822))
                .border(1.dp, GoldBright, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(text = "PKR (Rs.)", fontSize = 11.sp, color = GoldBright, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Deposit & Withdraw buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { showDepositModal = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = Color(0xFF141000)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("me_deposit_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                  text = if (currentLanguage == AppLanguage.EN) "Deposit PKR" else "رقم جمع کروائیں",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }

            OutlinedButton(
              onClick = { showWithdrawModal = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
                .testTag("me_withdraw_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Text(
                  text = if (currentLanguage == AppLanguage.EN) "Withdraw" else "رقم نکلوائیں",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    }

    // 3. Pakistani Local Payment Support Notice
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF071F13))
          .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "JazzCash", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = "•", color = TextMuted)
        Text(text = "EasyPaisa", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = "•", color = TextMuted)
        Text(text = "Meezan Bank IBAN", color = Color(0xFF448AFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    // 4. My Purchased Tickets List
    item {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (currentLanguage == AppLanguage.EN) "MY PURCHASED TICKETS" else "میرے خریدے گئے ٹکٹس",
            color = GoldPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "${userTickets.size} Tickets",
            color = TextMuted,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (userTickets.isEmpty()) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
              .padding(16.dp)
          ) {
            Text(
              text = if (currentLanguage == AppLanguage.EN) "You have not purchased any tickets yet. Go to Lottery to pick numbers!" else "آپ نے ابھی تک کوئی ٹکٹ نہیں خریدا۔ لاٹری میں جا کر نمبر منتخب کریں!",
              color = TextMuted,
              fontSize = 12.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            userTickets.take(4).forEach { t ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (t.isWinner) Color(0xFF0D3820) else EmeraldSurface
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    1.dp,
                    if (t.isWinner) GoldBright else EmeraldBorder,
                    RoundedCornerShape(12.dp)
                  )
                  .padding(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(text = t.id, color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(text = t.drawTitle, color = TextSecondary, fontSize = 11.sp)
                    Text(
                      text = "Numbers: ${t.selectedDaysJson} [M${t.selectedMonth}]",
                      color = TextPrimary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Text(
                    text = if (t.isWinner) "WINNER! ⭐" else "Active in Draw",
                    color = if (t.isWinner) Color(0xFF00E676) else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }

    // 5. Transaction History
    item {
      Column {
        Text(
          text = if (currentLanguage == AppLanguage.EN) "TRANSACTION LOG" else "لین دین کا ریکارڈ",
          color = GoldPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf("All", "Deposits", "Purchases", "Winnings").forEachIndexed { idx, name ->
            FilterChip(
              selected = selectedTxFilter == idx,
              onClick = { selectedTxFilter = idx },
              label = { Text(name, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GoldPrimary,
                selectedLabelColor = Color(0xFF141000),
                containerColor = Color(0xFF0C2B1B),
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedTxFilter == idx,
                borderColor = if (selectedTxFilter == idx) GoldBright else EmeraldBorder
              )
            )
          }
        }
      }
    }

    if (filteredTransactions.isEmpty()) {
      item {
        Text(text = "No transactions found in this filter.", color = TextMuted, fontSize = 11.sp)
      }
    } else {
      items(filteredTransactions.take(6)) { tx ->
        val isCredit = tx.type == "DEPOSIT" || tx.type == "WIN"
        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(tx.createdAt))
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, EmeraldBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = tx.description, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(text = "$dateStr • ${tx.paymentMethod}", color = TextMuted, fontSize = 9.sp)
            }
            Text(
              text = "${if (isCredit) "+" else "-"} Rs. %,d".format(tx.amountPkr),
              color = if (isCredit) Color(0xFF00E676) else Color(0xFFFF8A80),
              fontSize = 12.sp,
              fontWeight = FontWeight.Black
            )
          }
        }
      }
    }

    // 6. Discreet Management / Admin Login Portal Button
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF05170E))
          .border(0.5.dp, Color(0xFF1A3826), RoundedCornerShape(12.dp))
          .clickable {
            adminPinInput = ""
            pinError = null
            showAdminPinDialog = true
          }
          .padding(14.dp)
          .testTag("admin_portal_login_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Staff",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "Super Admin Portal (Staff Management)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // Admin PIN Login Modal Dialog
  if (showAdminPinDialog) {
    AlertDialog(
      onDismissRequest = { showAdminPinDialog = false },
      confirmButton = {
        Button(
          onClick = {
            val success = viewModel.loginAdminWithPin(adminPinInput)
            if (success) {
              showAdminPinDialog = false
            } else {
              pinError = "Incorrect PIN code. Try: admin123"
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000))
        ) {
          Text("Unlock Admin", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAdminPinDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldBright)
          Text("Super Admin Login", color = GoldBright, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column {
          Text(
            text = "Enter the authorized Super Admin PIN code to access lottery controls and manual winner declaration.",
            color = TextSecondary,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = adminPinInput,
            onValueChange = { adminPinInput = it },
            placeholder = { Text("Enter PIN (e.g. admin123)", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_pin_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GoldPrimary,
              unfocusedBorderColor = EmeraldBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
          if (pinError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = pinError ?: "", color = Color(0xFFFF5252), fontSize = 11.sp)
          }
        }
      },
      containerColor = EmeraldSurface
    )
  }

  // Deposit Dialog Modal
  if (showDepositModal) {
    DepositDialog(
      onDismiss = { showDepositModal = false },
      onConfirmDeposit = { amt, method, ref ->
        viewModel.depositFunds(amt, method, ref) {
          showDepositModal = false
        }
      }
    )
  }

  // Withdraw Dialog Modal
  if (showWithdrawModal) {
    WithdrawDialog(
      userBalance = balance,
      onDismiss = { showWithdrawModal = false },
      onConfirmWithdraw = { amt, method, title, num ->
        viewModel.withdrawFunds(
          amount = amt,
          method = method,
          accountTitle = title,
          accountNumber = num,
          onSuccess = { showWithdrawModal = false },
          onError = { /* handled in VM */ }
        )
      }
    )
  }
}

@Composable
private fun DepositDialog(
  onDismiss: () -> Unit,
  onConfirmDeposit: (Long, String, String) -> Unit
) {
  var selectedMethod by remember { mutableStateOf("JazzCash") }
  var depositAmount by remember { mutableStateOf(2500L) }
  var transactionRef by remember { mutableStateOf("") }
  val quickAmounts = listOf(1000L, 2500L, 5000L, 10000L)

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = {
          val ref = if (transactionRef.isBlank()) "TID-" + (100000..999999).random() else transactionRef
          onConfirmDeposit(depositAmount, selectedMethod, ref)
        },
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000))
      ) {
        Text("Confirm Deposit", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
    },
    title = { Text("Deposit PKR Balance", color = GoldBright, fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Text(text = "1. Send PKR to official account:", fontSize = 11.sp, color = TextMuted)
        Text(
          text = when (selectedMethod) {
            "JazzCash" -> "JazzCash: 0300-8889911 (Pakistan Lottery)"
            "EasyPaisa" -> "EasyPaisa: 0345-7776655 (Pakistan Lottery)"
            else -> "Meezan Bank: PK55MEZN0001928374829102"
          },
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = GoldBright
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("JazzCash", "EasyPaisa", "Bank").forEach { m ->
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedMethod == m) GoldPrimary else Color(0xFF0A2618))
                .clickable { selectedMethod = m }
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = m,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedMethod == m) Color(0xFF141000) else TextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "2. Amount in PKR:", fontSize = 11.sp, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          quickAmounts.forEach { amt ->
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (depositAmount == amt) GoldPrimary else Color(0xFF0A2618))
                .clickable { depositAmount = amt }
                .padding(vertical = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "%,d".format(amt),
                fontSize = 10.sp,
                color = if (depositAmount == amt) Color(0xFF141000) else TextPrimary,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = transactionRef,
          onValueChange = { transactionRef = it },
          placeholder = { Text("Sender Mobile or TID Ref", color = TextMuted) },
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
    },
    containerColor = EmeraldSurface
  )
}

@Composable
private fun WithdrawDialog(
  userBalance: Long,
  onDismiss: () -> Unit,
  onConfirmWithdraw: (Long, String, String, String) -> Unit
) {
  var method by remember { mutableStateOf("JazzCash") }
  var amountStr by remember { mutableStateOf("2000") }
  var title by remember { mutableStateOf("Muhammad Zeeshan") }
  var num by remember { mutableStateOf("03007861122") }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      val amt = amountStr.toLongOrNull() ?: 0L
      Button(
        onClick = { onConfirmWithdraw(amt, method, title, num) },
        enabled = amt in 100..userBalance,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000))
      ) {
        Text("Request Payout", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
    },
    title = { Text("Withdraw PKR", color = GoldBright, fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Text(text = "Available: Rs. %,d".format(userBalance), color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
          label = { Text("Amount PKR", color = TextMuted) },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Account Title", color = TextMuted) },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = num,
          onValueChange = { num = it },
          label = { Text("Account Number / IBAN", color = TextMuted) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    containerColor = EmeraldSurface
  )
}
