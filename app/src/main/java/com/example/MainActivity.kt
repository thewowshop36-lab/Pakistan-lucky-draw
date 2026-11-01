package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.LotteryRepository
import com.example.ui.LotteryScreen
import com.example.ui.LotteryViewModel
import com.example.ui.components.AgeVerificationDialog
import com.example.ui.components.LanguageManager
import com.example.ui.components.LotteryTopBar
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.DrawGameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.ScratchCardsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val database = remember { AppDatabase.getDatabase(context, coroutineScope) }
        val repository = remember { LotteryRepository(database.lotteryDao()) }
        val viewModel: LotteryViewModel = viewModel(
          factory = LotteryViewModel.Factory(repository)
        )

        MainAppContent(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun MainAppContent(viewModel: LotteryViewModel) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  val language by viewModel.language.collectAsStateWithLifecycle()
  val is18Verified by viewModel.is18Verified.collectAsStateWithLifecycle()
  val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
  val activeDraws by viewModel.activeDraws.collectAsStateWithLifecycle()
  val completedDraws by viewModel.completedDraws.collectAsStateWithLifecycle()
  val allDraws by viewModel.allDraws.collectAsStateWithLifecycle()
  val userTickets by viewModel.userTickets.collectAsStateWithLifecycle()
  val allTickets by viewModel.allTickets.collectAsStateWithLifecycle()
  val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
  val scratchRecords by viewModel.scratchRecords.collectAsStateWithLifecycle()

  val selectedDraw by viewModel.selectedDraw.collectAsStateWithLifecycle()
  val selectedDays by viewModel.selectedDays.collectAsStateWithLifecycle()
  val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
  val lineQuantity by viewModel.lineQuantity.collectAsStateWithLifecycle()

  // Collect toasts
  LaunchedEffect(Unit) {
    viewModel.toastMessage.collectLatest { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
  }

  // Handle system back navigation
  BackHandler(enabled = currentScreen != LotteryScreen.HOME) {
    viewModel.navigateTo(LotteryScreen.HOME)
  }

  // 18+ Verification Modal on first launch
  if (!is18Verified) {
    AgeVerificationDialog(
      currentLanguage = language,
      onConfirm18 = { viewModel.confirm18Plus() }
    )
  }

  Scaffold(
    topBar = {
      LotteryTopBar(
        currentBalance = currentUser?.walletBalancePkr ?: 0L,
        currentLanguage = language,
        isAdmin = isAdminMode,
        onLanguageToggle = { viewModel.toggleLanguage() },
        onRoleToggle = { viewModel.toggleAdminMode() },
        onWalletClick = { viewModel.navigateTo(LotteryScreen.WALLET) }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Color(0xFF03140A),
        contentColor = GoldPrimary,
        modifier = Modifier
          .border(0.5.dp, EmeraldBorder)
          .navigationBarsPadding()
      ) {
        NavigationBarItem(
          selected = currentScreen == LotteryScreen.HOME,
          onClick = { viewModel.navigateTo(LotteryScreen.HOME) },
          icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp)) },
          label = { Text(text = LanguageManager.get("nav_home", language), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = navBarColors(),
          modifier = Modifier.testTag("nav_item_home")
        )

        NavigationBarItem(
          selected = currentScreen == LotteryScreen.PLAY,
          onClick = { viewModel.navigateTo(LotteryScreen.PLAY) },
          icon = { Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = "Draw", modifier = Modifier.size(20.dp)) },
          label = { Text(text = LanguageManager.get("nav_play", language), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = navBarColors(),
          modifier = Modifier.testTag("nav_item_play")
        )

        NavigationBarItem(
          selected = currentScreen == LotteryScreen.SCRATCH,
          onClick = { viewModel.navigateTo(LotteryScreen.SCRATCH) },
          icon = { Icon(imageVector = Icons.Default.Casino, contentDescription = "Scratch", modifier = Modifier.size(20.dp)) },
          label = { Text(text = LanguageManager.get("nav_scratch", language), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = navBarColors(),
          modifier = Modifier.testTag("nav_item_scratch")
        )

        NavigationBarItem(
          selected = currentScreen == LotteryScreen.RESULTS,
          onClick = { viewModel.navigateTo(LotteryScreen.RESULTS) },
          icon = { Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Results", modifier = Modifier.size(20.dp)) },
          label = { Text(text = LanguageManager.get("nav_results", language), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = navBarColors(),
          modifier = Modifier.testTag("nav_item_results")
        )

        NavigationBarItem(
          selected = currentScreen == LotteryScreen.WALLET,
          onClick = { viewModel.navigateTo(LotteryScreen.WALLET) },
          icon = { Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", modifier = Modifier.size(20.dp)) },
          label = { Text(text = LanguageManager.get("nav_wallet", language), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = navBarColors(),
          modifier = Modifier.testTag("nav_item_wallet")
        )

        if (isAdminMode) {
          NavigationBarItem(
            selected = currentScreen == LotteryScreen.ADMIN,
            onClick = { viewModel.navigateTo(LotteryScreen.ADMIN) },
            icon = { Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp)) },
            label = { Text(text = "Admin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
            colors = navBarColors(),
            modifier = Modifier.testTag("nav_item_admin")
          )
        } else {
          NavigationBarItem(
            selected = currentScreen == LotteryScreen.HOW_TO_PLAY,
            onClick = { viewModel.navigateTo(LotteryScreen.HOW_TO_PLAY) },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Rules", modifier = Modifier.size(20.dp)) },
            label = { Text(text = "Rules", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = navBarColors(),
            modifier = Modifier.testTag("nav_item_rules")
          )
        }
      }
    },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(EmeraldDark)
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        LotteryScreen.HOME -> HomeScreen(
          viewModel = viewModel,
          activeDraws = activeDraws,
          currentLanguage = language
        )
        LotteryScreen.PLAY -> DrawGameScreen(
          viewModel = viewModel,
          draw = selectedDraw ?: activeDraws.firstOrNull(),
          selectedDays = selectedDays,
          selectedMonth = selectedMonth,
          lineQuantity = lineQuantity,
          userBalance = currentUser?.walletBalancePkr ?: 0L,
          currentLanguage = language
        )
        LotteryScreen.SCRATCH -> ScratchCardsScreen(
          viewModel = viewModel,
          userBalance = currentUser?.walletBalancePkr ?: 0L,
          scratchRecords = scratchRecords,
          currentLanguage = language
        )
        LotteryScreen.RESULTS -> ResultsScreen(
          completedDraws = completedDraws,
          allTickets = allTickets,
          userTickets = userTickets,
          currentLanguage = language
        )
        LotteryScreen.WALLET -> WalletScreen(
          viewModel = viewModel,
          currentUser = currentUser,
          transactions = userTransactions,
          currentLanguage = language
        )
        LotteryScreen.HOW_TO_PLAY -> HowToPlayScreen(
          currentLanguage = language
        )
        LotteryScreen.ADMIN -> AdminScreen(
          viewModel = viewModel,
          allDraws = allDraws,
          allTickets = allTickets,
          allUsers = allUsers
        )
      }
    }
  }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
  selectedIconColor = GoldBright,
  selectedTextColor = GoldBright,
  indicatorColor = Color(0xFF113824),
  unselectedIconColor = TextMuted,
  unselectedTextColor = TextMuted
)
