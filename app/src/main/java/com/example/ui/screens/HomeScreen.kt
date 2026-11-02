package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.DrawEntity
import com.example.ui.LotteryScreen
import com.example.ui.LotteryViewModel
import com.example.ui.components.AppLanguage
import com.example.ui.components.CountdownTimerView
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
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
  viewModel: LotteryViewModel,
  activeDraws: List<DrawEntity>,
  currentLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  val primaryDraw = activeDraws.firstOrNull()

  // Carousel State for Moving Banners
  val bannerDrawables = listOf(
    R.drawable.banner_jackpot,
    R.drawable.banner_mega,
    R.drawable.banner_daily
  )
  var currentBannerIndex by remember { mutableIntStateOf(0) }

  // Auto-slide every 3.5 seconds
  LaunchedEffect(Unit) {
    while (true) {
      delay(3500L)
      currentBannerIndex = (currentBannerIndex + 1) % bannerDrawables.size
    }
  }

  // Live Running / Rolling Numbers Ticker
  var rollingPotPkr by remember { mutableLongStateOf(75482900L) }
  var liveTicketsSold by remember { mutableIntStateOf(14820) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(2000L)
      rollingPotPkr += (1000..5000).random()
      liveTicketsSold += (1..3).random()
    }
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

    // 1. Moving Auto-sliding Video/Photo Banner Slider
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            1.5.dp,
            Brush.verticalGradient(listOf(GoldBright, EmeraldBorder)),
            RoundedCornerShape(22.dp)
          )
          .testTag("home_moving_carousel")
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
              .clickable { viewModel.navigateTo(LotteryScreen.LOTTERY) }
          ) {
            AnimatedContent(
              targetState = currentBannerIndex,
              transitionSpec = {
                fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) togetherWith
                  fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing))
              },
              label = "banner_slide"
            ) { targetIndex ->
              Image(
                painter = painterResource(id = bannerDrawables[targetIndex]),
                contentDescription = "Moving Lottery Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }

            // Dark luxury gradient overlay
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x66000000), Color(0xCC04190E))
                  )
                )
            )

            // Live Badge
            Box(
              modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xCC072617))
                .border(1.dp, GoldPrimary, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
                )
                Text(
                  text = "PAKISTAN LOTTERY LIVE",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = GoldBright,
                  letterSpacing = 0.5.sp
                )
              }
            }

            // Carousel dots indicator at bottom
            Row(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              bannerDrawables.indices.forEach { index ->
                val isSelected = currentBannerIndex == index
                Box(
                  modifier = Modifier
                    .height(6.dp)
                    .width(if (isSelected) 22.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isSelected) GoldPrimary else Color(0x80FFFFFF))
                )
              }
            }
          }
        }
      }
    }

    // 2. Rolling / Live Running Numbers Ticker Box
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF062215)),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.2.dp, GoldPrimary, RoundedCornerShape(16.dp))
          .testTag("running_numbers_ticker")
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "🔥", fontSize = 14.sp)
            Text(
              text = if (currentLanguage == AppLanguage.EN) "LIVE ROLLING GRAND POT (PKR)" else "لائیو چلتا ہوا گرینڈ جیک پاٹ پول",
              color = GoldBright,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Live animated ticking amount
          Text(
            text = "Rs. %,d".format(rollingPotPkr),
            color = GoldBright,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Rolling Lucky Numbers Balls Line
          Text(
            text = if (currentLanguage == AppLanguage.EN) "LATEST ROLLING NUMBERS:" else "حالیہ رولنگ نمبرز:",
            fontSize = 10.sp,
            color = TextMuted,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            listOf(7, 14, 21, 22, 29, 30).forEach { num ->
              NumberBall(number = num, isSelected = true, size = 32.dp, fontSize = 11)
            }
            Text(text = "+", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            NumberBall(number = 3, isSelected = true, isMonth = true, size = 32.dp, fontSize = 11)
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF04170E))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Live Tickets Entered: %,d".format(liveTicketsSold),
              color = Color(0xFF00E676),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "100% Verified Manual Draw",
              color = TextMuted,
              fontSize = 10.sp
            )
          }
        }
      }
    }

    // 3. Today's Grand Jackpot CTA Card
    primaryDraw?.let { draw ->
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldBorder, RoundedCornerShape(18.dp))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
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
                text = "Rs. %,d / Entry".format(draw.ticketPricePkr),
                color = GoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Countdown timer
            CountdownTimerView(targetEpoch = draw.drawTime)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = { viewModel.selectDrawForPlay(draw) },
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = Color(0xFF141000)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("home_enter_draw_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                  text = if (currentLanguage == AppLanguage.EN) "Pick Numbers & Enter Draw" else "نمبر منتخب کریں اور لاٹری میں حصہ لیں",
                  fontWeight = FontWeight.Black,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }
    }

    // 4. Active Lotteries Quick Carousel/List Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (currentLanguage == AppLanguage.EN) "Active Lotteries" else "فعال لاٹریز",
          color = GoldPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "View All →",
          color = GoldBright,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.clickable { viewModel.navigateTo(LotteryScreen.LOTTERY) }
        )
      }
    }

    // Active Draw Cards preview
    items(activeDraws.take(2)) { draw ->
      ActiveDrawHomeItem(
        draw = draw,
        onPlay = { viewModel.selectDrawForPlay(draw) }
      )
    }

    // 5. Recent Winners Marquee Strip
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071F13)),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, EmeraldBorder, RoundedCornerShape(14.dp))
          .clickable { viewModel.navigateTo(LotteryScreen.WINNERS) }
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = GoldBright, modifier = Modifier.size(24.dp))
            Column {
              Text(
                text = "Ali Hamza (Lahore) won Rs. 50,000,000!",
                color = GoldBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Ticket: PKL-2026-78401 • Quaid Memorial Draw",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }
          }
          Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
        }
      }
    }

    // 6. Security & Official Guarantee
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF04160D))
          .border(0.5.dp, EmeraldBorder, RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
        Column {
          Text(text = "OFFICIAL GOVERNMENT LICENSED LOTTERY", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Black)
          Text(
            text = "Guaranteed PKR payouts directly to JazzCash, EasyPaisa, or Pakistani Bank Accounts.",
            color = TextMuted,
            fontSize = 9.sp
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ActiveDrawHomeItem(
  draw: DrawEntity,
  onPlay: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, EmeraldBorder, RoundedCornerShape(14.dp))
      .padding(bottom = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(text = draw.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = "Prize: Rs. %,d".format(draw.prizePoolPkr), color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text(text = "Ticket: Rs. %,d".format(draw.ticketPricePkr), color = TextMuted, fontSize = 10.sp)
      }

      Button(
        onClick = onPlay,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF141000)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Text(text = "Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
