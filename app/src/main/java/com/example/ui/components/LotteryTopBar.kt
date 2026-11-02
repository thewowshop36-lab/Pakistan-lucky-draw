package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LotteryTopBar(
  currentBalance: Long,
  currentLanguage: AppLanguage,
  isAdmin: Boolean,
  onLanguageToggle: () -> Unit,
  onRoleToggle: () -> Unit,
  onWalletClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF03140B), EmeraldDark)
        )
      )
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          listOf(Color(0xFF1E5236), Color.Transparent)
        ),
        shape = androidx.compose.ui.graphics.RectangleShape
      )
      .statusBarsPadding()
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Brand Logo & Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.pak_lottery_logo),
          contentDescription = "Pakistan Lottery Emblem",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(1.5.dp, Brush.linearGradient(listOf(GoldBright, GoldDark)), CircleShape)
        )

        Column {
          Text(
            text = LanguageManager.get("app_name", currentLanguage),
            color = GoldPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )
          Text(
            text = if (isAdmin) "SUPER ADMIN CONSOLE" else LanguageManager.get("app_tagline", currentLanguage),
            color = if (isAdmin) Color(0xFFFF5252) else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // Actions: Language Toggle, Wallet Pill, Admin Toggle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Language Toggle Pill
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C2B1C))
            .border(1.dp, EmeraldBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onLanguageToggle)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("language_toggle_button")
        ) {
          Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = "Translate",
            tint = GoldBright,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = if (currentLanguage == AppLanguage.EN) "اردو" else "EN",
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
          )
        }

        // Wallet Balance Pill
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF133824))
            .border(1.dp, Brush.linearGradient(listOf(GoldBright, GoldDark)), RoundedCornerShape(20.dp))
            .clickable(onClick = onWalletClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("topbar_wallet_pill")
        ) {
          Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = "Wallet",
            tint = GoldPrimary,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Rs. %,d".format(currentBalance),
            color = GoldBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 5.dp)
          )
        }

        // If Admin is active, show Exit Admin pill
        if (isAdmin) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(Color(0xFF5C1010))
              .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(20.dp))
              .clickable(onClick = onRoleToggle)
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("admin_role_toggle_button")
          ) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = "Exit Admin",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "Exit",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(start = 3.dp)
            )
          }
        }
      }
    }
  }
}
