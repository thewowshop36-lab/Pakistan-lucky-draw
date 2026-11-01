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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.DrawEntity
import com.example.ui.LotteryScreen
import com.example.ui.LotteryViewModel
import com.example.ui.components.AppLanguage
import com.example.ui.components.CountdownTimerView
import com.example.ui.components.LanguageManager
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldSurfaceVariant
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
  viewModel: LotteryViewModel,
  activeDraws: List<DrawEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  val primaryDraw = activeDraws.firstOrNull()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(EmeraldDark)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
    }

    // 1. Hero Grand Jackpot Banner
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            1.5.dp,
            Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)),
            RoundedCornerShape(20.dp)
          )
          .testTag("grand_jackpot_banner")
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.banner_jackpot),
              contentDescription = "Jackpot Banner",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay for smooth transition
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xCC05180E), EmeraldSurface)
                  )
                )
            )

            // Category badge
            Box(
              modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xE60A2618))
                .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Stars,
                  contentDescription = null,
                  tint = GoldBright,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "OFFICIAL GRAND DRAW",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = GoldBright,
                  letterSpacing = 0.5.sp
                )
              }
            }
          }

          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = primaryDraw?.title ?: "Pakistan Grand Bumper 2026",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Rs. %,d".format(primaryDraw?.prizePoolPkr ?: 75000000L),
              color = GoldBright,
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            )

            Text(
              text = LanguageManager.get("grand_jackpot", currentLanguage),
              color = TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Countdown Timer
            if (primaryDraw != null) {
              CountdownTimerView(targetEpoch = primaryDraw.drawTime)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = {
                if (primaryDraw != null) {
                  viewModel.selectDrawForPlay(primaryDraw)
                } else {
                  viewModel.navigateTo(LotteryScreen.PLAY)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = Color(0xFF141000)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("hero_play_now_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ConfirmationNumber,
                  contentDescription = null,
                  tint = Color(0xFF141000)
                )
                Text(
                  text = if (currentLanguage == AppLanguage.EN) "Pick Numbers & Enter (Rs. %,d)".format(primaryDraw?.ticketPricePkr ?: 1000L)
                  else "نمبر منتخب کریں اور ٹکٹ خریدیں",
                  fontWeight = FontWeight.Black,
                  fontSize = 14.sp
                )
              }
            }
          }
        }
      }
    }

    // 2. Recent Winners Ticker
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF071F13))
          .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = "Winner",
          tint = GoldBright,
          modifier = Modifier.size(20.dp)
        )
        Column {
          Text(
            text = "LATEST WINNER: Ali Hamza (Lahore)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GoldBright
          )
          Text(
            text = "Won Rs. 50,000,000 • Ticket PKL-2026-78401",
            fontSize = 10.sp,
            color = TextSecondary
          )
        }
      }
    }

    // 3. Active Draws Section Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(GoldPrimary)
          )
          Text(
            text = LanguageManager.get("active_draws", currentLanguage),
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Text(
          text = if (currentLanguage == AppLanguage.EN) "All Guaranteed" else "مکمل طور پر شفاف",
          color = TextMuted,
          fontSize = 12.sp
        )
      }
    }

    // Active Draw Cards
    items(activeDraws) { draw ->
      ActiveDrawCard(
        draw = draw,
        currentLanguage = currentLanguage,
        onPlay = { viewModel.selectDrawForPlay(draw) }
      )
    }

    // 4. Instant Scratch Cards Section
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
          .clickable { viewModel.navigateTo(LotteryScreen.SCRATCH) }
          .padding(14.dp)
          .testTag("scratch_cards_teaser_card")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
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
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F3622))
                .border(1.dp, GoldBright, CircleShape)
            ) {
              Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(24.dp)
              )
            }

            Column {
              Text(
                text = LanguageManager.get("nav_scratch", currentLanguage),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (currentLanguage == AppLanguage.EN) "Scratch & Win Up to Rs. 5,000,000" else "سکریچ کریں اور فوری جیتیں 50 لاکھ تک",
                color = GoldBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Open",
            tint = GoldPrimary
          )
        }
      }
    }

    // 5. How to Play Quick Steps
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF071F13))
          .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Text(
          text = if (currentLanguage == AppLanguage.EN) "How to Play (UAE Style 6+1)" else "کیسے کھیلیں (یو اے ای لاٹری فارمولا)",
          color = GoldPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        HowToStepRow(step = "1", title = "Pick 6 Days (1 to 31)", desc = "Select your 6 lucky date numbers from the days grid.")
        HowToStepRow(step = "2", title = "Pick 1 Month (1 to 12)", desc = "Select 1 lucky month ball from 1 to 12.")
        HowToStepRow(step = "3", title = "Win PKR Cash Prizes", desc = "Match numbers to win multi-million PKR jackpot tiers.")
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun ActiveDrawCard(
  draw: DrawEntity,
  currentLanguage: AppLanguage,
  onPlay: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp))
      .padding(bottom = 4.dp)
      .testTag("draw_card_${draw.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = draw.categoryBadge,
          color = GoldBright,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0A2B1A))
            .border(0.5.dp, GoldBright, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(13.dp)
          )
          Text(text = "LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = draw.title,
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Prize Pool: Rs. %,d".format(draw.prizePoolPkr),
        color = GoldBright,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = "TICKET PRICE", fontSize = 9.sp, color = TextMuted)
          Text(text = "Rs. %,d".format(draw.ticketPricePkr), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Button(
          onClick = onPlay,
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldPrimary,
            contentColor = Color(0xFF141000)
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("play_draw_${draw.id}")
        ) {
          Text(
            text = if (currentLanguage == AppLanguage.EN) "Play Draw" else "کھیلیں",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}

@Composable
private fun HowToStepRow(step: String, title: String, desc: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(22.dp)
        .clip(CircleShape)
        .background(GoldPrimary)
    ) {
      Text(
        text = step,
        color = Color(0xFF141000),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black
      )
    }

    Column {
      Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      Text(text = desc, color = TextSecondary, fontSize = 10.sp)
    }
  }
}
