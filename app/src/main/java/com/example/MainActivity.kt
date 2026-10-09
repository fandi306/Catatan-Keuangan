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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
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
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.NavyHero
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
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

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
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
    val tag: String
) {
    HOME("Beranda", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TRANSACTIONS("Transaksi", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_transactions"),
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

    val accounts by viewModel.accounts.collectAsState()
    val uiNotice by viewModel.uiNotice.collectAsState()
    val lastDeletedTx by viewModel.lastDeletedTransaction.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentNav == NavItem.HOME || currentNav == NavItem.TRANSACTIONS) {
                ExtendedFloatingActionButton(
                    onClick = { showAddTxDialog = true },
                    containerColor = NavyHero,
                    contentColor = Color.White,
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
                NavItem.entries.forEach { item ->
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
                            selectedIconColor = NavyHero,
                            selectedTextColor = NavyHero,
                            indicatorColor = GoldTarget.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { paddingValues ->
        // Responsive container: Centered fluid layout up to 760.dp for seamless tablet & mobile experience
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
                            onAddNewTransaction = { showAddTxDialog = true }
                        )
                        NavItem.TRANSACTIONS -> TransactionsScreen(
                            viewModel = viewModel,
                            onAddNewTransaction = { showAddTxDialog = true }
                        )
                        NavItem.ANALYTICS -> AnalyticsScreen(
                            viewModel = viewModel,
                            onSetTargetClick = { currentNav = NavItem.HOME }
                        )
                        NavItem.PROFILE -> ProfileScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentNav = NavItem.HOME },
                            onSignedOut = onSignedOut
                        )
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            accounts = accounts,
            onDismiss = { showAddTxDialog = false },
            onSave = { type, desc, amount, account, category, date ->
                viewModel.addTransaction(type, desc, amount, account, category, date)
                showAddTxDialog = false
            }
        )
    }
}
