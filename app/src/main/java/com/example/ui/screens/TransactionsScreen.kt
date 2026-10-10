package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.components.ScanReceiptDialog
import com.example.ui.components.TransactionItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavyHeroDark
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.Formatters

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onAddNewTransaction: () -> Unit = {}
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val transactions by viewModel.displayedTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showScanReceiptDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val chipSelectedBg = if (isDark) GoldTarget else NavyHero
    val chipSelectedContent = if (isDark) NavyHeroDark else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("transactions_screen")
        ) {
            // Header Bar with Title, Scan Nota Button, and Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Transaksi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${transactions.size} transaksi tercatat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pindahkan tombol Scan Nota ke halaman transaksi sesuai permintaan pengguna
                    OutlinedButton(
                        onClick = { showScanReceiptDialog = true },
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_scan_receipt_transactions")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scan Nota",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(99.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        FilledIconButton(
                            onClick = { viewModel.changeMonth(-1) },
                            modifier = Modifier.size(28.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Bulan lalu"
                            )
                        }

                        Text(
                            text = Formatters.formatMonthYear(selectedMonth),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        FilledIconButton(
                            onClick = { viewModel.changeMonth(1) },
                            modifier = Modifier.size(28.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Bulan depan"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini Month Flow Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreen.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Masuk",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen
                        )
                        Text(
                            text = Formatters.formatRupiah(monthlyIncome),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRed.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keluar",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed
                        )
                        Text(
                            text = Formatters.formatRupiah(monthlyExpense),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "all",
                    onClick = { viewModel.setFilter("all") },
                    label = { Text("Semua") },
                    shape = RoundedCornerShape(99.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = chipSelectedContent
                    )
                )
                FilterChip(
                    selected = filterType == "out",
                    onClick = { viewModel.setFilter("out") },
                    label = { Text("Pengeluaran") },
                    shape = RoundedCornerShape(99.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = chipSelectedContent
                    )
                )
                FilterChip(
                    selected = filterType == "in",
                    onClick = { viewModel.setFilter("in") },
                    label = { Text("Pemasukan") },
                    shape = RoundedCornerShape(99.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = chipSelectedContent
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cari berdasarkan keterangan, kategori, akun...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Transactions List Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (transactions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        itemsIndexed(transactions, key = { _, tx -> tx.id }) { index, tx ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Column {
                                    TransactionItem(
                                        transaction = tx,
                                        onDeleteClick = { viewModel.deleteTransaction(tx) }
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Belum Ada Transaksi (Rp 0)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Catat pengeluaran belanja, foto struk dengan Scan Nota, atau catat pemasukan gaji Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onAddNewTransaction,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) GoldTarget else NavyHero,
                                        contentColor = if (isDark) NavyHeroDark else Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Catat Manual", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showScanReceiptDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Nota AI")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScanReceiptDialog) {
        ScanReceiptDialog(
            accounts = accounts,
            onDismiss = { showScanReceiptDialog = false },
            onConfirmExpense = { desc, amount, account, category, date ->
                viewModel.addTransaction("out", desc, amount, account, category, date)
                showScanReceiptDialog = false
            }
        )
    }
}
