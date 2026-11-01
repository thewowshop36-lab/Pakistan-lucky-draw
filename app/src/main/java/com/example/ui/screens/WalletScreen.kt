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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.ui.LotteryViewModel
import com.example.ui.components.AppLanguage
import com.example.ui.components.LanguageManager
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
fun WalletScreen(
  viewModel: LotteryViewModel,
  currentUser: UserEntity?,
  transactions: List<TransactionEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  var showDepositModal by remember { mutableStateOf(false) }
  var showWithdrawModal by remember { mutableStateOf(false) }
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

    // 1. Wallet Balance Hero Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
          .padding(bottom = 2.dp)
          .testTag("wallet_balance_card")
      ) {
        Column(
          modifier = Modifier.padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "OFFICIAL LOTTERY WALLET",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
              Text(
                text = currentUser?.name ?: "Valued Player",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = currentUser?.phone ?: "+92 300 7861122",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0C2B1C))
                .border(1.dp, GoldBright, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(text = "PKR (Rs.)", fontSize = 11.sp, color = GoldBright, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Rs. %,d".format(balance),
            color = GoldBright,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          Text(
            text = "Available Balance for Lucky Draws & Scratch Cards",
            color = TextSecondary,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Deposit & Withdraw action buttons
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
                .testTag("deposit_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = LanguageManager.get("deposit", currentLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                .testTag("withdraw_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Text(text = LanguageManager.get("withdraw", currentLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }
    }

    // 2. Pakistani Payment Gateway Support Badges
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF071F13))
          .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        PaymentMethodBadge(name = "JazzCash", colorHex = 0xFFFF1744)
        PaymentMethodBadge(name = "EasyPaisa", colorHex = 0xFF00C853)
        PaymentMethodBadge(name = "Bank Transfer", colorHex = 0xFF2979FF)
      }
    }

    // 3. Transactions Filter & Header
    item {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TRANSACTION HISTORY",
            color = GoldPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "${filteredTransactions.size} Records",
            color = TextMuted,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val filterNames = listOf("All", "Deposits", "Purchases", "Winnings")
          filterNames.forEachIndexed { idx, name ->
            FilterChip(
              selected = selectedTxFilter == idx,
              onClick = { selectedTxFilter = idx },
              label = { Text(name, fontSize = 11.sp) },
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

    // Transaction rows
    if (filteredTransactions.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "No transactions found in this category.", color = TextMuted, fontSize = 13.sp)
        }
      }
    } else {
      items(filteredTransactions) { tx ->
        TransactionRowItem(tx = tx)
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
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
private fun PaymentMethodBadge(name: String, colorHex: Long) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(Color(colorHex))
    )
    Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
  }
}

@Composable
private fun TransactionRowItem(tx: TransactionEntity) {
  val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(tx.createdAt))
  val isCredit = tx.type == "DEPOSIT" || tx.type == "WIN"

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
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isCredit) Color(0xFF0F3822) else Color(0xFF381515))
            .border(1.dp, if (isCredit) Color(0xFF00E676) else Color(0xFFFF5252), CircleShape)
        ) {
          Icon(
            imageVector = when (tx.type) {
              "DEPOSIT" -> Icons.Default.ArrowDownward
              "WIN" -> Icons.Default.Receipt
              "PURCHASE" -> Icons.Default.ShoppingCart
              else -> Icons.Default.ArrowUpward
            },
            contentDescription = null,
            tint = if (isCredit) Color(0xFF00E676) else Color(0xFFFF5252),
            modifier = Modifier.size(18.dp)
          )
        }

        Column {
          Text(text = tx.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          Text(text = "$dateStr • ${tx.paymentMethod}", color = TextMuted, fontSize = 10.sp)
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${if (isCredit) "+" else "-"} Rs. %,d".format(tx.amountPkr),
          color = if (isCredit) Color(0xFF00E676) else Color(0xFFFFCDD2),
          fontSize = 13.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = tx.status,
          color = if (tx.status == "COMPLETED") Color(0xFF81C784) else Color(0xFFFFB74D),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun DepositDialog(
  onDismiss: () -> Unit,
  onConfirmDeposit: (Long, String, String) -> Unit
) {
  var selectedMethod by remember { mutableStateOf("JazzCash") }
  var depositAmount by remember { mutableLongStateOf(2500L) }
  var transactionRef by remember { mutableStateOf("") }

  val quickAmounts = listOf(1000L, 2500L, 5000L, 10000L)

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
        .padding(2.dp)
        .testTag("deposit_dialog")
    ) {
      Column(
        modifier = Modifier.padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Deposit PKR Funds", color = GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Black)
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "1. SELECT LOCAL PAYMENT METHOD", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("JazzCash", "EasyPaisa", "Bank").forEach { method ->
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedMethod == method) GoldPrimary else Color(0xFF092014))
                .border(1.dp, if (selectedMethod == method) GoldBright else EmeraldBorder, RoundedCornerShape(8.dp))
                .clickable { selectedMethod = method }
                .padding(vertical = 8.dp)
            ) {
              Text(
                text = method,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedMethod == method) Color(0xFF141000) else TextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Official Merchant Account details instruction
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF071F13))
            .border(1.dp, EmeraldBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
        ) {
          Column {
            Text(
              text = when (selectedMethod) {
                "JazzCash" -> "Send to JazzCash Till / Acc: 0300-8889911 (Pakistan Lottery)"
                "EasyPaisa" -> "Send to EasyPaisa Merchant: 0345-7776655 (Pakistan Lottery)"
                else -> "Meezan Bank IBAN: PK55MEZN0001928374829102 (Pakistan Lottery Ltd)"
              },
              fontSize = 11.sp,
              color = GoldBright,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "2. CHOOSE AMOUNT (PKR)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickAmounts.forEach { amt ->
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (depositAmount == amt) GoldPrimary else Color(0xFF092014))
                .border(1.dp, if (depositAmount == amt) GoldBright else EmeraldBorder, RoundedCornerShape(8.dp))
                .clickable { depositAmount = amt }
                .padding(vertical = 6.dp)
            ) {
              Text(
                text = "%,d".format(amt),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (depositAmount == amt) Color(0xFF141000) else TextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "3. SENDER PHONE / TID REFERENCE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = transactionRef,
          onValueChange = { transactionRef = it },
          placeholder = { Text("e.g. 03001234567 or TID-998822", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("deposit_tid_input"),
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
            val ref = if (transactionRef.isBlank()) "TID-" + (100000..999999).random() else transactionRef
            onConfirmDeposit(depositAmount, selectedMethod, ref)
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("confirm_deposit_submit_button")
        ) {
          Text(text = "Confirm Deposit Rs. %,d".format(depositAmount), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun WithdrawDialog(
  userBalance: Long,
  onDismiss: () -> Unit,
  onConfirmWithdraw: (Long, String, String, String) -> Unit
) {
  var selectedMethod by remember { mutableStateOf("JazzCash") }
  var withdrawAmountStr by remember { mutableStateOf("2000") }
  var accountTitle by remember { mutableStateOf("Muhammad Zeeshan") }
  var accountNumber by remember { mutableStateOf("03007861122") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
        .padding(2.dp)
        .testTag("withdraw_dialog")
    ) {
      Column(
        modifier = Modifier.padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Withdraw Funds", color = GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Black)
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Available to withdraw: Rs. %,d".format(userBalance), color = TextSecondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "PAYOUT METHOD", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("JazzCash", "EasyPaisa", "Bank").forEach { method ->
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedMethod == method) GoldPrimary else Color(0xFF092014))
                .border(1.dp, if (selectedMethod == method) GoldBright else EmeraldBorder, RoundedCornerShape(8.dp))
                .clickable { selectedMethod = method }
                .padding(vertical = 6.dp)
            ) {
              Text(
                text = method,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedMethod == method) Color(0xFF141000) else TextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = withdrawAmountStr,
          onValueChange = { withdrawAmountStr = it.filter { ch -> ch.isDigit() } },
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
          value = accountTitle,
          onValueChange = { accountTitle = it },
          label = { Text("Account Title / Name", color = TextMuted) },
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
          value = accountNumber,
          onValueChange = { accountNumber = it },
          label = { Text("Mobile No. or IBAN", color = TextMuted) },
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

        val amt = withdrawAmountStr.toLongOrNull() ?: 0L
        val canWithdraw = amt in 100..userBalance

        Button(
          onClick = {
            onConfirmWithdraw(amt, selectedMethod, accountTitle, accountNumber)
          },
          enabled = canWithdraw,
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("confirm_withdraw_submit_button")
        ) {
          Text(text = "Withdraw Rs. %,d".format(amt), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
