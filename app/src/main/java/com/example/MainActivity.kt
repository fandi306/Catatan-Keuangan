package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.FinanceRepository
import com.example.ui.auth.AuthManager
import com.example.ui.auth.AuthScreen
import com.example.ui.components.AddTransactionDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CurhatAiScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.NavyHero
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.AppPreferences
import com.example.util.AppThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CatatanKeuanganApp.initFirebase(this)
        enableEdgeToEdge()

        setContent {
            val appPreferences = remember { AppPreferences.getInstance(this@MainActivity) }
            val themeMode by appPreferences.themeMode.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()

            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember {
        mutableStateOf(
            try {
                Firebase.auth.currentUser
            } catch (_: Exception) {
                null
            }
        )
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = try {
                auth.currentUser
            } catch (_: Exception) {
                null
            }
        }
        try {
            Firebase.auth.addAuthStateListener(listener)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                Firebase.auth.removeAuthStateListener(listener)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainAppShell(
            userId = currentUser!!.uid,
            onSignedOut = {
                currentUser = null
            }
        )
    }
}

enum class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
    val isPrimaryTab: Boolean = true
) {
    HOME("Beranda", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TRANSACTIONS("Transaksi", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_transactions"),
    CURHAT_AI("Curhat AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_curhat_ai", isPrimaryTab = false),
    ANALYTICS("Pencapaian", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "nav_analytics"),
    PROFILE("Profil", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun MainAppShell(
    userId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(userId) { FinanceRepository(context) }
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.Factory(userId, repository),
        key = "FinanceViewModel_$userId"
    )

    var currentNav by remember { mutableStateOf(NavItem.HOME) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showManageAccountsDialog by remember { mutableStateOf(false) }

    val accounts by viewModel.accounts.collectAsState()
    val accountBalances by viewModel.accountBalances.collectAsState()
    val uiNotice by viewModel.uiNotice.collectAsState()
    val lastDeletedTx by viewModel.lastDeletedTransaction.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val primaryActionBg = if (isDark) GoldTarget else NavyHero
    val primaryActionContent = if (isDark) com.example.ui.theme.NavyHeroDark else Color.White

    LaunchedEffect(uiNotice) {
        val notice = uiNotice
        if (notice != null) {
            if (notice == "Transaksi dihapus" && lastDeletedTx != null) {
                val result = snackbarHostState.showSnackbar(
                    message = notice,
                    actionLabel = "Urungkan",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = notice,
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.clearUiNotice()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // Tablet & Large Screen Layout: Sidebar Navigation Rail + Fluid Content Area
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Spacer(modifier = Modifier.height(14.dp))
                        FloatingActionButton(
                            onClick = { showAddTxDialog = true },
                            containerColor = primaryActionBg,
                            contentColor = primaryActionContent,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("fab_add_transaction_tablet")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Catat transaksi", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    },
                    modifier = Modifier.testTag("navigation_rail")
                ) {
                    NavItem.entries.filter { it.isPrimaryTab }.forEach { item ->
                        val isSelected = currentNav == item
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentNav = item },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = primaryActionBg,
                                selectedTextColor = primaryActionBg,
                                indicatorColor = GoldTarget.copy(alpha = 0.25f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentNav,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.fillMaxSize(),
                            label = "tabletNavTransition"
                        ) { targetNav ->
                            when (targetNav) {
                                NavItem.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToProfile = { currentNav = NavItem.PROFILE },
                                    onNavigateToTransactions = { currentNav = NavItem.TRANSACTIONS },
                                    onNavigateToAnalytics = { currentNav = NavItem.ANALYTICS },
                                    onNavigateToCurhatAi = { currentNav = NavItem.CURHAT_AI },
                                    onAddNewTransaction = { showAddTxDialog = true }
                                )
                                NavItem.TRANSACTIONS -> TransactionsScreen(
                                    viewModel = viewModel,
                                    onAddNewTransaction = { showAddTxDialog = true }
                                )
                                NavItem.CURHAT_AI -> CurhatAiScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentNav = NavItem.PROFILE }
                                )
                                NavItem.ANALYTICS -> AnalyticsScreen(
                                    viewModel = viewModel,
                                    onSetTargetClick = { currentNav = NavItem.HOME }
                                )
                                NavItem.PROFILE -> ProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentNav = NavItem.HOME },
                                    onNavigateToCurhatAi = { currentNav = NavItem.CURHAT_AI },
                                    onSignedOut = onSignedOut
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Phone Layout
            if (currentNav == NavItem.CURHAT_AI) {
                CurhatAiScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentNav = NavItem.PROFILE }
                )
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        if (currentNav == NavItem.HOME || currentNav == NavItem.TRANSACTIONS) {
                        ExtendedFloatingActionButton(
                            onClick = { showAddTxDialog = true },
                            containerColor = primaryActionBg,
                            contentColor = primaryActionContent,
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("fab_add_transaction")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Catat transaksi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("bottom_navigation_bar")
                    ) {
                        NavItem.entries.filter { it.isPrimaryTab }.forEach { item ->
                            val isSelected = currentNav == item
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentNav = item },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = primaryActionBg,
                                    selectedTextColor = primaryActionBg,
                                    indicatorColor = GoldTarget.copy(alpha = 0.25f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentNav,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.fillMaxSize(),
                            label = "mainNavTransition"
                        ) { targetNav ->
                            when (targetNav) {
                                NavItem.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToProfile = { currentNav = NavItem.PROFILE },
                                    onNavigateToTransactions = { currentNav = NavItem.TRANSACTIONS },
                                    onNavigateToAnalytics = { currentNav = NavItem.ANALYTICS },
                                    onNavigateToCurhatAi = { currentNav = NavItem.CURHAT_AI },
                                    onAddNewTransaction = { showAddTxDialog = true }
                                )
                                NavItem.TRANSACTIONS -> TransactionsScreen(
                                    viewModel = viewModel,
                                    onAddNewTransaction = { showAddTxDialog = true }
                                )
                                NavItem.CURHAT_AI -> CurhatAiScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentNav = NavItem.PROFILE }
                                )
                                NavItem.ANALYTICS -> AnalyticsScreen(
                                    viewModel = viewModel,
                                    onSetTargetClick = { currentNav = NavItem.HOME }
                                )
                                NavItem.PROFILE -> ProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentNav = NavItem.HOME },
                                    onNavigateToCurhatAi = { currentNav = NavItem.CURHAT_AI },
                                    onSignedOut = onSignedOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            accounts = accounts,
            onDismiss = { showAddTxDialog = false },
            onCreateAccount = {
                showAddTxDialog = false
                showManageAccountsDialog = true
            },
            onSave = { type, desc, amount, account, category, date ->
                viewModel.addTransaction(type, desc, amount, account, category, date)
                showAddTxDialog = false
            }
        )
    }

    if (showManageAccountsDialog) {
        com.example.ui.components.AccountManagementDialog(
            accounts = accounts,
            balances = accountBalances,
            onDismiss = { showManageAccountsDialog = false },
            onAddAccount = { name, type ->
                viewModel.addAccount(name, type)
            },
            onDeleteAccount = { accId ->
                viewModel.deleteAccount(accId)
            }
        )
    }
}
