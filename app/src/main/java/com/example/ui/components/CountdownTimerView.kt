package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun CountdownTimerView(
  targetEpoch: Long,
  modifier: Modifier = Modifier
) {
  var remainingMillis by remember(targetEpoch) {
    mutableLongStateOf((targetEpoch - System.currentTimeMillis()).coerceAtLeast(0L))
  }

  LaunchedEffect(targetEpoch) {
    while (remainingMillis > 0) {
      delay(1000L)
      remainingMillis = (targetEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
    }
  }

  val totalSeconds = remainingMillis / 1000
  val days = totalSeconds / (24 * 3600)
  val hours = (totalSeconds % (24 * 3600)) / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60

  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
  ) {
    TimerDigitBox(label = "DAYS", value = String.format("%02d", days))
    TimerSeparator()
    TimerDigitBox(label = "HOURS", value = String.format("%02d", hours))
    TimerSeparator()
    TimerDigitBox(label = "MINS", value = String.format("%02d", minutes))
    TimerSeparator()
    TimerDigitBox(label = "SECS", value = String.format("%02d", seconds), isAccent = true)
  }
}

@Composable
private fun TimerDigitBox(
  label: String,
  value: String,
  isAccent: Boolean = false
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(52.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF071F13))
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          listOf(
            if (isAccent) GoldBright else EmeraldBorder,
            Color(0xFF04120A)
          )
        ),
        shape = RoundedCornerShape(8.dp)
      )
      .padding(vertical = 6.dp)
  ) {
    Text(
      text = value,
      fontSize = 18.sp,
      fontWeight = FontWeight.Black,
      color = if (isAccent) GoldBright else GoldPrimary
    )
    Text(
      text = label,
      fontSize = 8.sp,
      fontWeight = FontWeight.SemiBold,
      color = TextMuted
    )
  }
}

@Composable
private fun TimerSeparator() {
  Text(
    text = ":",
    color = GoldPrimary,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    modifier = Modifier.padding(bottom = 8.dp)
  )
}
