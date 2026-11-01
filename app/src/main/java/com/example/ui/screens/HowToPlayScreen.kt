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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLanguage
import com.example.ui.components.LanguageManager
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HowToPlayScreen(
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  val expandedFaqs = remember { mutableStateMapOf<Int, Boolean>() }

  val faqs = listOf(
    Pair(
      "How does the Pakistan Lottery 6+1 Lucky Draw work?",
      "Modeled after the prestigious UAE Lottery Lucky Day, players pick 6 Days from 1 to 31, and 1 Month from 1 to 12. Matching all 6 days plus the exact month wins the Grand Bumper Jackpot (up to Rs. 75 Million PKR). Lesser matches win generous secondary cash tiers."
    ),
    Pair(
      "How are winnings declared and credited?",
      "All draws are transparently verified. Upon official draw declaration by the commission, prizes are credited directly into your verified PKR wallet. You can withdraw your balance anytime to JazzCash, EasyPaisa, or any Pakistani Bank Account."
    ),
    Pair(
      "What are the prize tiers and matching rules?",
      "• Match 6 Days + 1 Month: 1st Prize (Grand Jackpot)\n• Match 6 Days only: 2nd Prize (Rs. 5,000,000)\n• Match 5 Days + 1 Month: 3rd Prize (Rs. 1,000,000)\n• Match 5 Days only: 4th Prize (Rs. 100,000)\n• Match 4 Days: 5th Prize (Rs. 10,000)"
    ),
    Pair(
      "How do Instant Scratch Cards work?",
      "Digital scratch cards allow instant wins. Match 3 identical symbols or multiplier icons on a 3x3 grid to win up to Rs. 5,000,000 immediately."
    ),
    Pair(
      "Who is eligible to participate?",
      "Participation is strictly restricted to individuals aged 18 and older. We follow strict Responsible Gaming protocols."
    )
  )

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

    item {
      Text(
        text = LanguageManager.get("how_to_play", currentLanguage),
        color = GoldPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Black
      )
      Text(
        text = "Official Rules, Prize Tiers & Responsible Gaming Guidelines",
        color = TextSecondary,
        fontSize = 12.sp
      )
    }

    // Rules Overview Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)), RoundedCornerShape(16.dp))
          .padding(bottom = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = GoldBright)
            Text(
              text = "The 6+1 Game Formula",
              color = GoldBright,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          RulePoint(
            number = "1",
            title = "Pick 6 Days (1-31)",
            desc = "Choose 6 numbers representing days of the month from the 31-day interactive grid, or tap Quick Pick for auto selection."
          )

          Spacer(modifier = Modifier.height(8.dp))

          RulePoint(
            number = "2",
            title = "Pick 1 Lucky Month (1-12)",
            desc = "Select your special golden month number from 1 to 12. This completes your official entry."
          )

          Spacer(modifier = Modifier.height(8.dp))

          RulePoint(
            number = "3",
            title = "Watch Live Draw or Check Ticket",
            desc = "Each draw has a live countdown. Check your ticket ID in the Ticket Verifier anytime."
          )
        }
      }
    }

    // Responsible Gaming 18+ Badge
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF38150C))
          .border(1.dp, Color(0xFFFF5722), RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFF8A65))
        Column {
          Text(text = "18+ RESPONSIBLE GAMING", color = Color(0xFFFFAB91), fontSize = 11.sp, fontWeight = FontWeight.Black)
          Text(
            text = "Play responsibly. Set spending limits and participate only for entertainment.",
            color = TextSecondary,
            fontSize = 10.sp
          )
        }
      }
    }

    // FAQs Accordion
    item {
      Text(
        text = "FREQUENTLY ASKED QUESTIONS",
        color = GoldPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
    }

    itemsIndexed(faqs) { index, pair ->
      val (q, a) = pair
      val isExpanded = expandedFaqs[index] == true

      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .clickable { expandedFaqs[index] = !isExpanded }
          .padding(12.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
              Text(text = q, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Icon(
              imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = TextSecondary
            )
          }

          AnimatedVisibility(visible = isExpanded) {
            Column {
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = a, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun RulePoint(number: String, title: String, desc: String) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(GoldPrimary)
    ) {
      Text(text = number, color = Color(0xFF141000), fontSize = 12.sp, fontWeight = FontWeight.Black)
    }

    Column {
      Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Text(text = desc, color = TextSecondary, fontSize = 11.sp)
    }
  }
}
