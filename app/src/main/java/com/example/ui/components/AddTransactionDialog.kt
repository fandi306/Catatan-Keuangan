package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Account
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavyHeroDark
import com.example.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.util.BudgetCategoryHelper
import com.example.util.BudgetGroup

val DEFAULT_CATEGORIES = BudgetCategoryHelper.CATEGORY_NAMES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onCreateAccount: () -> Unit = {},
    onSave: (type: String, desc: String, amount: Long, account: String, category: String, date: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryButtonBg = if (isDark) GoldTarget else NavyHero
    val primaryButtonContent = if (isDark) NavyHeroDark else Color.White

    var type by remember { mutableStateOf("out") } // "out" or "in"
    var description by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DEFAULT_CATEGORIES[0]) }
    var selectedAccount by remember {
        mutableStateOf(accounts.firstOrNull()?.name ?: "")
    }
    var selectedDate by remember { mutableStateOf(Formatters.currentIsoDate()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showScanReceipt by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_transaction_dialog")
        ) {
            if (accounts.isEmpty()) {
                // Empty Wallets/Accounts Guard
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.12f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isDark) GoldTarget else NavyHero,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Buat Dompet Terlebih Dahulu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Anda belum memiliki akun atau tempat penyimpanan (seperti Tunai, Rekening Bank, atau E-Wallet). Silakan buat dompet pertama Anda sebelum mulai mencatat transaksi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onCreateAccount()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryButtonBg,
                                contentColor = primaryButtonContent
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                        ) {
                            Text("Buat Dompet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Catat Transaksi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextButton(
                            onClick = { showScanReceipt = true },
                            modifier = Modifier.testTag("btn_open_scan_receipt")
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented button: Pengeluaran / Pemasukan
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = type == "out",
                            onClick = { type = "out" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = ExpenseRed.copy(alpha = 0.15f),
                                activeContentColor = ExpenseRed
                            ),
                            modifier = Modifier.testTag("segment_expense")
                        ) {
                            Text("Pengeluaran", fontWeight = FontWeight.Bold)
                        }

                        SegmentedButton(
                            selected = type == "in",
                            onClick = { type = "in" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = IncomeGreen.copy(alpha = 0.15f),
                                activeContentColor = IncomeGreen
                            ),
                            modifier = Modifier.testTag("segment_income")
                        ) {
                            Text("Pemasukan", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Keterangan
                    Text(
                        text = "Keterangan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Contoh: Makan Siang / Gaji Bulanan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_description"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nominal (Rp) with formatted thousands
                    Text(
                        text = "Nominal (Rp)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = rawAmount,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            rawAmount = if (digits.isNotBlank()) {
                                val parsed = digits.toLongOrNull() ?: 0L
                                Formatters.formatNumber(parsed)
                            } else {
                                ""
                            }
                        },
                        placeholder = { Text("Contoh: 50.000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )

                    // Category chips (Only for expense)
                    if (type == "out") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kategori Pengeluaran (Sesuai Panduan OJK)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (catMeta in BudgetCategoryHelper.ALL_CATEGORIES) {
                                val isSelected = selectedCategory == catMeta.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = catMeta.name },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${catMeta.emoji} ${catMeta.name}", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = catMeta.group.color.copy(alpha = if (isSelected) 0.35f else 0.15f)
                                            ) {
                                                Text(
                                                    text = catMeta.group.badge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected && isDark) Color.White else catMeta.group.color,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(99.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryButtonBg,
                                        selectedLabelColor = primaryButtonContent
                                    ),
                                    modifier = Modifier.testTag("chip_cat_${catMeta.name}")
                                )
                            }
                        }
                    }

                    // Account chips
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Akun / Dompet Penyimpanan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (acc in accounts) {
                            FilterChip(
                                selected = selectedAccount == acc.name,
                                onClick = { selectedAccount = acc.name },
                                label = { Text(acc.name) },
                                shape = RoundedCornerShape(99.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryButtonBg,
                                    selectedLabelColor = primaryButtonContent
                                ),
                                modifier = Modifier.testTag("chip_acc_${acc.name}")
                            )
                        }
                    }

                    // Date Picker trigger
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Tanggal Transaksi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Formatters.formatDateIndo(selectedDate),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pilih Tanggal")
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions: Batal & Simpan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_transaction_button")
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                val amountVal = Formatters.parseAmount(rawAmount)
                                val descVal = description.trim()
                                if (descVal.isBlank()) {
                                    errorMessage = "Isi keterangan transaksi dulu."
                                    return@Button
                                }
                                if (amountVal <= 0L) {
                                    errorMessage = "Isi nominal lebih dari 0."
                                    return@Button
                                }
                                if (selectedAccount.isBlank()) {
                                    errorMessage = "Pilih akun pembayaran."
                                    return@Button
                                }
                                errorMessage = null
                                onSave(
                                    type,
                                    descVal,
                                    amountVal,
                                    selectedAccount,
                                    if (type == "out") selectedCategory else "",
                                    selectedDate
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryButtonBg,
                                contentColor = primaryButtonContent
                            ),
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("save_transaction_button")
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        selectedDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showScanReceipt) {
        ScanReceiptDialog(
            accounts = accounts,
            onDismiss = { showScanReceipt = false },
            onConfirmExpense = { desc, amount, account, category, date ->
                type = "out"
                description = desc
                rawAmount = Formatters.formatRupiah(amount).replace("Rp", "").trim()
                selectedAccount = account
                selectedCategory = category
                selectedDate = date
                showScanReceipt = false
            }
        )
    }
}
