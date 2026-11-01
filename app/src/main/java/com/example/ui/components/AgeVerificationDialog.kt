package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AgeVerificationDialog(
  currentLanguage: AppLanguage,
  onConfirm18: () -> Unit
) {
  Dialog(
    onDismissRequest = { /* Must actively confirm */ },
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(20.dp))
        .testTag("age_verification_modal")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
      ) {
        // Official Emblem
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(EmeraldDark)
            .border(2.dp, Brush.linearGradient(listOf(GoldBright, GoldDark)), CircleShape)
        ) {
          Image(
            painter = painterResource(id = R.drawable.pak_lottery_logo),
            contentDescription = "Emblem",
            modifier = Modifier.size(62.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 18+ Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF38150C))
            .border(1.dp, Color(0xFFFF5722), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Age Limit",
            tint = Color(0xFFFF8A65),
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "18+ YEARS ONLY",
            color = Color(0xFFFFAB91),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = if (currentLanguage == AppLanguage.EN) "Age Verification & Terms" else "عمر کی تصدیق اور شرائط",
          color = GoldPrimary,
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (currentLanguage == AppLanguage.EN)
            "Welcome to Pakistan Lottery. In compliance with responsible gaming standards, you must be 18 years or older to participate in draws, buy tickets, or play instant scratch games."
          else
            "پاکستان لاٹری میں خوش آمدید۔ ذمہ دارانہ گیمنگ کے اصولوں کے تحت، قرعہ اندازی میں شرکت، ٹکٹ خریدنے یا سکریچ کارڈز کھیلنے کے لیے آپ کی عمر 18 سال یا اس سے زیادہ ہونی چاہیے۔",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 19.sp,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onConfirm18,
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldPrimary,
            contentColor = Color(0xFF141000)
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_18_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = Color(0xFF141000)
            )
            Text(
              text = if (currentLanguage == AppLanguage.EN) "I am 18 or Older — Enter" else "میری عمر 18 سال یا زیادہ ہے — داخل ہوں",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  }
}
