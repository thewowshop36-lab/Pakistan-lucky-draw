package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentRuby
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary

@Composable
fun NumberBall(
  number: Int,
  isSelected: Boolean,
  onClick: (() -> Unit)? = null,
  isMonth: Boolean = false,
  size: Dp = 42.dp,
  fontSize: Int = 14,
  testTag: String = "ball_$number"
) {
  val animBg by animateColorAsState(
    targetValue = when {
      isSelected && isMonth -> AccentRuby
      isSelected -> GoldPrimary
      isMonth -> Color(0xFF261014)
      else -> Color(0xFF0F3120)
    },
    animationSpec = tween(200),
    label = "ballColor"
  )

  val borderBrush = when {
    isSelected && isMonth -> Brush.linearGradient(listOf(Color(0xFFFF8A80), AccentRuby))
    isSelected -> Brush.linearGradient(listOf(GoldBright, GoldDark))
    isMonth -> Brush.linearGradient(listOf(Color(0xFF5C2025), Color(0xFF331015)))
    else -> Brush.linearGradient(listOf(Color(0xFF1E5236), Color(0xFF113824)))
  }

  val textColor = when {
    isSelected && isMonth -> Color.White
    isSelected -> Color(0xFF1C1300)
    isMonth -> Color(0xFFFFCDD2)
    else -> Color(0xFFE2F1E8)
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(size)
      .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
      .clip(CircleShape)
      .background(animBg)
      .border(if (isSelected) 2.dp else 1.dp, borderBrush, CircleShape)
      .then(
        if (onClick != null) {
          Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
        } else {
          Modifier.testTag(testTag)
        }
      )
  ) {
    Text(
      text = String.format("%02d", number),
      fontSize = fontSize.sp,
      fontWeight = FontWeight.Bold,
      color = textColor
    )
  }
}
