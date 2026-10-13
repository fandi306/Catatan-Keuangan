package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppPreferences
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.AccountChipsRow
import com.example.ui.components.AccountManagementDialog
import com.example.ui.components.EditTargetDialog
import com.example.ui.components.HeroCard
import com.example.ui.components.MonthlySummaryCard
import com.example.ui.components.RotatableFinancialQuoteCard
import com.example.ui.components.TransactionItem
import com.example.ui.components.WarningBannersSection
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavyHeroDark
import com.example.ui.theme.NavySurfaceDark
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.Formatters
import com.example.util.ProfilePhotoHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCurhatAi: () -> Unit = {},
    onAddNewTransaction: () -> Unit
) {
    val totalAsset by viewModel.totalAsset.collectAsState()
    val liquidAsset by viewModel.liquidAsset.collectAsState()
    val nonLiquidAsset by viewModel.nonLiquidAsset.collectAsState()
    val savingsTarget by viewModel.savingsTarget.collectAsState()
    val targetProgressPct by viewModel.targetProgressPct.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val accountBalances by viewModel.accountBalances.collectAsState()

    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyNet by viewModel.monthlyNet.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val financialAdvice by viewModel.financialAdvice.collectAsState()

    val displayedTransactions by viewModel.displayedTransactions.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()

    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser = try { Firebase.auth.currentUser } catch (_: Exception) { null }

    var showEditTargetDialog by remember { mutableStateOf(false) }
    var showManageAccountsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val appPreferences = remember { AppPreferences.getInstance(context) }
    val homeSettings by appPreferences.homeSettings.collectAsState()

    val rawName = userProfile?.displayName?.ifBlank { null }
        ?: currentUser?.displayName?.ifBlank { null }
        ?: "Fandi"
    val firstName = rawName.trim().split(" ").firstOrNull()?.ifBlank { "Fandi" } ?: "Fandi"
    val photoUri = userProfile?.photoUri?.ifBlank { null } ?: currentUser?.photoUrl?.toString()

    // Responsive adaptive layout: 2 columns on wide screens (tablets) or single clean column on mobile phones
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTablet = maxWidth >= 700.dp

        if (isTablet) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1040.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    LineBankStyleHeader(
                        firstName = firstName,
                        photoUri = photoUri,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }

                item {
                    val hasRightColumn = homeSettings.showAccountChips ||
                            homeSettings.showQuotes ||
                            (homeSettings.showFinancialWarnings && financialAdvice.warnings.isNotEmpty()) ||
                            (homeSettings.showRecentTransactions && displayedTransactions.isNotEmpty())

                    if (hasRightColumn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left Column: Hero Total Asset + Monthly Summary Chart (Inti Utama - Tidak Disembunyikan)
                            Column(
                                modifier = Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                HeroCard(
                                    totalAsset = totalAsset,
                                    liquidAsset = liquidAsset,
                                    nonLiquidAsset = nonLiquidAsset,
                                    savingsTarget = savingsTarget,
                                    targetProgressPct = targetProgressPct,
                                    onEditTargetClick = { showEditTargetDialog = true },
                                    onViewAnalyticsClick = onNavigateToAnalytics,
                                    showSparklineCurve = homeSettings.showSparklineCurve
                                )

                                MonthlySummaryCard(
                                    selectedMonth = selectedMonth,
                                    income = monthlyIncome,
                                    expense = monthlyExpense,
                                    net = monthlyNet,
                                    categoryBreakdown = categoryBreakdown,
                                    onPrevMonth = { viewModel.changeMonth(-1) },
                                    onNextMonth = { viewModel.changeMonth(1) }
                                )
                            }

                            // Right Column: Dompet & Akun, Kutipan Finansial, Warnings, Transaksi Terkini
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (homeSettings.showAccountChips) {
                                    AccountChipsRow(
                                        accounts = accounts,
                                        balances = accountBalances,
                                        onManageAccountsClick = { showManageAccountsDialog = true }
                                    )
                                }

                                if (homeSettings.showQuotes) {
                                    RotatableFinancialQuoteCard(
                                        advice = financialAdvice
                                    )
                                }

                                if (homeSettings.showFinancialWarnings && financialAdvice.warnings.isNotEmpty()) {
                                    WarningBannersSection(
                                        warnings = financialAdvice.warnings
                                    )
                                }

                                if (homeSettings.showRecentTransactions) {
                                    RecentTransactionsCard(
                                        transactions = displayedTransactions.take(6),
                                        onViewAllClick = onNavigateToTransactions,
                                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Clean Mode on Tablet: Kolom terpusat rapi dan elegan
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HeroCard(
                                totalAsset = totalAsset,
                                liquidAsset = liquidAsset,
                                nonLiquidAsset = nonLiquidAsset,
                                savingsTarget = savingsTarget,
                                targetProgressPct = targetProgressPct,
                                onEditTargetClick = { showEditTargetDialog = true },
                                onViewAnalyticsClick = onNavigateToAnalytics,
                                showSparklineCurve = homeSettings.showSparklineCurve
                            )

                            MonthlySummaryCard(
                                selectedMonth = selectedMonth,
                                income = monthlyIncome,
                                expense = monthlyExpense,
                                net = monthlyNet,
                                categoryBreakdown = categoryBreakdown,
                                onPrevMonth = { viewModel.changeMonth(-1) },
                                onNextMonth = { viewModel.changeMonth(1) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            // Mobile Phone: Single clean scrollable column
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    LineBankStyleHeader(
                        firstName = firstName,
                        photoUri = photoUri,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }

                item {
                    HeroCard(
                        totalAsset = totalAsset,
                        liquidAsset = liquidAsset,
                        nonLiquidAsset = nonLiquidAsset,
                        savingsTarget = savingsTarget,
                        targetProgressPct = targetProgressPct,
                        onEditTargetClick = { showEditTargetDialog = true },
                        onViewAnalyticsClick = onNavigateToAnalytics,
                        showSparklineCurve = homeSettings.showSparklineCurve
                    )
                }

                // Guide for new users when wallets/accounts are empty
                if (accounts.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth().testTag("empty_accounts_banner")
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GoldTarget.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = if (isSystemInDarkTheme()) GoldTarget else NavyHero,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Buat Dompet Pertama Anda",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Anda belum memiliki akun atau dompet (seperti Tunai, Rekening, E-Wallet). Buat dompet dulu sebelum mencatat transaksi.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showManageAccountsDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSystemInDarkTheme()) GoldTarget else NavyHero,
                                        contentColor = if (isSystemInDarkTheme()) NavyHeroDark else Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buat Dompet Sekarang", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (allTransactions.isEmpty()) {
                    item {
                        NewUserGuidanceCard(
                            onAddTransactionClick = onAddNewTransaction,
                            onLoadSampleDataClick = { viewModel.restoreSeedData() }
                        )
                    }
                }

                if (homeSettings.showAccountChips) {
                    item {
                        AccountChipsRow(
                            accounts = accounts,
                            balances = accountBalances,
                            onManageAccountsClick = { showManageAccountsDialog = true }
                        )
                    }
                }

                // Ringkasan Bulanan (Inti Utama - Tidak Disembunyikan)
                item {
                    MonthlySummaryCard(
                        selectedMonth = selectedMonth,
                        income = monthlyIncome,
                        expense = monthlyExpense,
                        net = monthlyNet,
                        categoryBreakdown = categoryBreakdown,
                        onPrevMonth = { viewModel.changeMonth(-1) },
                        onNextMonth = { viewModel.changeMonth(1) }
                    )
                }



                // Kutipan Finansial Dinamis (Bisa disembunyikan via Kustomisasi Profil)
                if (homeSettings.showQuotes) {
                    item {
                        RotatableFinancialQuoteCard(
                            advice = financialAdvice
                        )
                    }
                }

                if (homeSettings.showFinancialWarnings && financialAdvice.warnings.isNotEmpty()) {
                    item {
                        WarningBannersSection(
                            warnings = financialAdvice.warnings
                        )
                    }
                }

                if (homeSettings.showRecentTransactions) {
                    item {
                        RecentTransactionsCard(
                            transactions = displayedTransactions.take(5),
                            onViewAllClick = onNavigateToTransactions,
                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }

    if (showEditTargetDialog) {
        EditTargetDialog(
            currentTarget = savingsTarget,
            onDismiss = { showEditTargetDialog = false },
            onSave = { newTarget ->
                viewModel.updateSavingsTarget(newTarget)
                showEditTargetDialog = false
            }
        )
    }

    if (showManageAccountsDialog) {
        AccountManagementDialog(
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

/**
 * Onboarding Guidance Card displayed when user has 0 transactions
 */
@Composable
private fun NewUserGuidanceCard(
    onAddTransactionClick: () -> Unit,
    onLoadSampleDataClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = GoldTarget.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = GoldTarget,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Cara Mudah Mengisi Catatan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saldo & transaksi Anda saat ini masih 0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GuidanceStep(
                step = "1",
                title = "Catat Transaksi Pertama",
                description = "Tekan tombol '+ Catat transaksi' di bawah. Pilih jenis Pemasukan (misal: Gaji) atau Pengeluaran (misal: Makan)."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GuidanceStep(
                step = "2",
                title = "Pilih Akun / Dompet",
                description = "Tentukan apakah uang disimpan di Tunai, Bank BRI, Line Bank, atau akun lainnya agar saldo tercatat akurat."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GuidanceStep(
                step = "3",
                title = "Pantau Grafik Pencapaian",
                description = "Setiap pemasukan otomatis menaikkan persentase capaian target tabungan dan grafik kurva aset Anda!"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddTransactionClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) GoldTarget else NavyHero,
                        contentColor = if (isSystemInDarkTheme()) NavyHeroDark else Color.White
                    ),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Sekarang", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLoadSampleDataClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contoh Data")
                }
            }
        }
    }
}

@Composable
private fun GuidanceStep(
    step: String,
    title: String,
    description: String
) {
    val isDark = isSystemInDarkTheme()
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = if (isDark) GoldTarget else NavyHero,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    color = if (isDark) NavyHeroDark else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun LineBankStyleHeader(
    firstName: String,
    photoUri: String?,
    onNavigateToProfile: () -> Unit
) {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val greetingWord = when (hour) {
        in 4..10 -> "Selamat Pagi,"
        in 11..14 -> "Selamat Siang,"
        in 15..18 -> "Selamat Sore,"
        else -> "Selamat Malam,"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("brand_header")
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavyHero,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.catatan_keuangan_icon_1791516859908),
                            contentDescription = "Logo",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Catatan Keuangan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }

            // Right icons: Cloud Status & Profile Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = IncomeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Sync Aktif",
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isDark = isSystemInDarkTheme()
                val presetEmoji = ProfilePhotoHelper.getPresetEmoji(photoUri)
                val resolvedModel = ProfilePhotoHelper.resolveImageModel(photoUri)

                Surface(
                    shape = CircleShape,
                    color = if (isDark) NavySurfaceDark else NavyHero,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, GoldTarget, CircleShape)
                        .clickable { onNavigateToProfile() }
                        .testTag("profile_avatar_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!presetEmoji.isNullOrBlank()) {
                            Text(
                                text = presetEmoji,
                                fontSize = 20.sp
                            )
                        } else if (resolvedModel != null) {
                            AsyncImage(
                                model = resolvedModel,
                                contentDescription = "Foto Profil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = firstName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big Typography Greeting like reference screenshot:
        // "Selamat Siang,
        //  FANDI!"
        Column {
            Text(
                text = greetingWord,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 36.sp
            )
            Text(
                text = "${firstName.uppercase()}!",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
                lineHeight = 40.sp
            )
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    transactions: List<com.example.data.model.Transaction>,
    onViewAllClick: () -> Unit,
    onDeleteTransaction: (com.example.data.model.Transaction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terkini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onViewAllClick,
                    modifier = Modifier.testTag("btn_view_all_transactions")
                ) {
                    Text(
                        text = "Lihat Semua",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isNotEmpty()) {
                Column {
                    transactions.forEachIndexed { index, tx ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Column {
                                TransactionItem(
                                    transaction = tx,
                                    onDeleteClick = { onDeleteTransaction(tx) }
                                )
                                if (index < transactions.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        thickness = 0.8.dp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum Ada Catatan Transaksi (Rp 0)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketuk tombol '+ Catat transaksi' di bawah untuk mulai mencatat pemasukan atau pengeluaran pertama Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurhatAiBannerCard(
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NavySurfaceDark else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldTarget.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("banner_curhat_ai")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GoldTarget.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDark) GoldTarget else NavyHero,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Curhat Finansial ke AI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IncomeGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Tersinkron",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ada pengeluaran tak terduga atau overbudget? Ceritakan, AI siap mencatatkan otomatis!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = GoldTarget,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
