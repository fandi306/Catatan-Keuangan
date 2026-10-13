package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavyHeroDark
import com.example.ui.theme.NavySurfaceDark
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BudgetCategoryHelper
import com.example.util.ChatMessage
import com.example.util.FinancialContext
import com.example.util.FinancialNotificationHelper
import com.example.util.Formatters
import com.example.util.GeminiChatHelper
import com.example.util.ProposedTransaction
import kotlinx.coroutines.launch

@Composable
fun CurhatAiScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val userProfile by viewModel.userProfile.collectAsState()
    val totalAsset by viewModel.totalAsset.collectAsState()
    val liquidAsset by viewModel.liquidAsset.collectAsState()
    val nonLiquidAsset by viewModel.nonLiquidAsset.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyNet by viewModel.monthlyNet.collectAsState()
    val savingsTarget by viewModel.savingsTarget.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val displayedTransactions by viewModel.displayedTransactions.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(
                ChatMessage(
                    isUser = false,
                    text = "Halo ${userProfile?.displayName?.ifBlank { "Sahabat" } ?: "Sahabat"}! 👋 Aku Finansia, teman curhat dan asisten keuangan pribadimu.\n\nAku terhubung langsung dengan catatan finansialmu secara real-time. Kalau kamu ada pengeluaran tak terduga, pusing mikirin uang, atau butuh saran berhemat, ceritakan saja apa adanya. Kalau kamu sebutkan pengeluaran (misal: habis 1jt buat servis & makan), aku bisa langsung bantu catatkan ke pembukuan aplikasimu! 💡"
                )
            )
        }
    }

    val primaryBg = if (isDark) NavyHeroDark else NavyHero
    val primaryCardBg = if (isDark) NavySurfaceDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            FinancialNotificationHelper.sendTestNotification(context)
            Toast.makeText(context, "Notifikasi aktif! Uji coba berhasil dikirim ke panel notifikasi.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin notifikasi belum diberikan.", Toast.LENGTH_SHORT).show()
        }
    }

    val quickPrompts = listOf(
        "Hari ini habis 1jt: 900k servis motor, 100k makan",
        "Berapa sisa uang & pengeluaranku bulan ini?",
        "Lapar mata terus belanja, gimana cara ngeremnya?",
        "Pos apa yang paling boros bulan ini?",
        "Tips nabung realistis buat target impian"
    )

    fun sendUserMessage(msgText: String) {
        val clean = msgText.trim()
        if (clean.isBlank() || isLoading) return

        val userMsg = ChatMessage(isUser = true, text = clean)
        messages.add(userMsg)
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)

            val recentTxStr = displayedTransactions.take(5).joinToString("; ") {
                "${it.date}: ${it.description} (${if (it.type == "in") "+" else "-"}${Formatters.formatRupiah(it.amount)})"
            }

            val financialContext = FinancialContext(
                userName = userProfile?.displayName ?: "",
                totalAsset = totalAsset,
                liquidAsset = liquidAsset,
                nonLiquidAsset = nonLiquidAsset,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                monthlyNet = monthlyNet,
                savingsTarget = savingsTarget,
                accounts = accounts.map { it.name },
                topCategories = categoryBreakdown,
                recentTransactionsSummary = recentTxStr
            )

            val botResponse = GeminiChatHelper.sendMessage(
                history = messages.toList(),
                userMessage = clean,
                financialContext = financialContext
            )

            messages.add(botResponse)
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_curhat")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldTarget.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isDark) GoldTarget else NavyHero,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Finansia AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Teman Curhat & Asisten Finansial",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Test Notification button
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !FinancialNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val sent = FinancialNotificationHelper.sendTestNotification(context)
                                    if (sent) {
                                        Toast.makeText(context, "Notifikasi terkirim ke panel atas / control center!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Periksa izin notifikasi perangkat.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_test_notification")
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "Uji Notifikasi",
                                tint = GoldTarget
                            )
                        }

                        // Reset Chat
                        IconButton(
                            onClick = {
                                messages.clear()
                                messages.add(
                                    ChatMessage(
                                        isUser = false,
                                        text = "Percakapan baru telah dimulai. Ada cerita atau pengeluaran apa hari ini?"
                                    )
                                )
                            },
                            modifier = Modifier.testTag("btn_clear_chat")
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Hapus Obrolan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Real-time synchronization indicator banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GoldTarget.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (isDark) GoldTarget else NavyHero,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tersinkron dengan data saldo, ${accounts.size} akun & transaksi aktifmu",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) GoldTarget else NavyHero,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Chat list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.isUser) {
                        UserMessageBubble(message = message, isDark = isDark)
                    } else {
                        BotMessageBubble(
                            message = message,
                            accounts = accounts,
                            isDark = isDark,
                            onRecordTransactions = { proposedList, selectedAccountName ->
                                val total = proposedList.sumOf { it.amount }
                                proposedList.forEach { p ->
                                    viewModel.addTransaction(
                                        type = p.type,
                                        description = p.description,
                                        amount = p.amount,
                                        account = selectedAccountName,
                                        category = p.category,
                                        date = p.date
                                    )
                                }

                                // Update local message state to recorded
                                val idx = messages.indexOfFirst { it.id == message.id }
                                if (idx != -1) {
                                    messages[idx] = messages[idx].copy(isRecorded = true)
                                }

                                // Post system notification in Android Control Center
                                FinancialNotificationHelper.sendAiSyncSuccessNotification(
                                    context = context,
                                    count = proposedList.size,
                                    totalAmount = total,
                                    account = selectedAccountName
                                )

                                // Check if large expense alert should also fire
                                if (total >= 500_000L) {
                                    FinancialNotificationHelper.sendLargeExpenseNotification(
                                        context = context,
                                        description = proposedList.firstOrNull()?.description ?: "Pengeluaran Curhat",
                                        amount = total,
                                        account = selectedAccountName
                                    )
                                }

                                Toast.makeText(
                                    context,
                                    "✓ ${proposedList.size} Transaksi berhasil dicatat ke akun $selectedAccountName!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                                color = GoldTarget
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Finansia sedang memikirkan solusi & menganalisis catatanmu...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick suggestion prompts
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { sendUserMessage(prompt) },
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Message Input bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Curhat atau sebutkan pengeluaran...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_curhat_text"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldTarget,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendUserMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) {
                                    if (isDark) GoldTarget else NavyHero
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .testTag("btn_send_curhat")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim",
                            tint = if (inputText.isNotBlank() && !isLoading) {
                                if (isDark) NavyHeroDark else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserMessageBubble(
    message: ChatMessage,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(if (isDark) GoldTarget else NavyHero)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) NavyHeroDark else Color.White
            )
        }
    }
}

@Composable
fun BotMessageBubble(
    message: ChatMessage,
    accounts: List<Account>,
    isDark: Boolean,
    onRecordTransactions: (List<ProposedTransaction>, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GoldTarget.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (isDark) GoldTarget else NavyHero,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Interactive transaction proposal card if any transactions were detected
            if (message.proposedTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ProposedTransactionsCard(
                    proposedTransactions = message.proposedTransactions,
                    accounts = accounts,
                    isRecorded = message.isRecorded,
                    isDark = isDark,
                    onConfirm = { selectedAccount ->
                        onRecordTransactions(message.proposedTransactions, selectedAccount)
                    }
                )
            }
        }
    }
}

@Composable
fun ProposedTransactionsCard(
    proposedTransactions: List<ProposedTransaction>,
    accounts: List<Account>,
    isRecorded: Boolean,
    isDark: Boolean,
    onConfirm: (String) -> Unit
) {
    val totalAmount = proposedTransactions.sumOf { it.amount }
    val defaultAccount = accounts.firstOrNull()?.name ?: "Tunai"
    var selectedAccount by remember { mutableStateOf(defaultAccount) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NavySurfaceDark else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRecorded) IncomeGreen.copy(alpha = 0.6f) else GoldTarget.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_detected_transactions")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isRecorded) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isRecorded) IncomeGreen else GoldTarget,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecorded) "Transaksi Sudah Tercatat" else "Deteksi Catatan Finansial",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecorded) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = Formatters.formatRupiah(totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Items list
            proposedTransactions.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Kategori: ${item.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = Formatters.formatRupiah(item.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.type == "in") IncomeGreen else ExpenseRed
                    )
                }
            }

            if (!isRecorded) {
                Spacer(modifier = Modifier.height(10.dp))

                // Account choice chips
                Text(
                    text = "Pilih Akun Sumber:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val availableAccounts = if (accounts.isEmpty()) listOf("Tunai") else accounts.map { it.name }
                    items(availableAccounts) { accName ->
                        val isSelected = selectedAccount == accName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = accName },
                            label = { Text(accName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldTarget.copy(alpha = 0.25f),
                                selectedLabelColor = if (isDark) GoldTarget else NavyHero
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm action button
                Button(
                    onClick = { onConfirm(selectedAccount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_confirm_add_curhat_tx"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) GoldTarget else NavyHero,
                        contentColor = if (isDark) NavyHeroDark else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Catat ke Pengeluaran Sekarang",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IncomeGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tercatat di data riwayat & saldo aplikasi Anda",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
