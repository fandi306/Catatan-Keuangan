package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavyHeroDark
import com.example.util.ExportHelper
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportFormat {
    PDF, EXCEL_CSV
}

enum class ExportPeriod {
    MONTHLY, ALL_TIME
}

@Composable
fun ExportTransactionsDialog(
    allTransactions: List<Transaction>,
    currentMonth: String,
    userName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedPeriod by remember { mutableStateOf(ExportPeriod.MONTHLY) }
    var selectedTypeFilter by remember { mutableStateOf("all") } // "all", "out", "in"
    var isExporting by remember { mutableStateOf(false) }

    // Filter transactions based on period and type
    val periodTransactions = remember(allTransactions, selectedPeriod, currentMonth) {
        when (selectedPeriod) {
            ExportPeriod.MONTHLY -> allTransactions.filter { it.date.startsWith(currentMonth) }
            ExportPeriod.ALL_TIME -> allTransactions
        }
    }

    val filteredTransactions = remember(periodTransactions, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "in" -> periodTransactions.filter { it.type == "in" }
            "out" -> periodTransactions.filter { it.type == "out" }
            else -> periodTransactions
        }.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "in" }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "out" }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val periodLabel = remember(selectedPeriod, currentMonth) {
        when (selectedPeriod) {
            ExportPeriod.MONTHLY -> Formatters.formatMonthYear(currentMonth)
            ExportPeriod.ALL_TIME -> "Semua Periode (${allTransactions.size} Transaksi)"
        }
    }

    fun executeExport(action: String) { // "share" or "open"
        if (filteredTransactions.isEmpty()) {
            Toast.makeText(context, "Tidak ada transaksi untuk diekspor pada filter ini.", Toast.LENGTH_SHORT).show()
            return
        }

        isExporting = true
        coroutineScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    if (selectedFormat == ExportFormat.PDF) {
                        ExportHelper.generatePdfReport(
                            context = context,
                            transactions = filteredTransactions,
                            periodTitle = periodLabel,
                            userName = userName,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            netBalance = netBalance
                        )
                    } else {
                        ExportHelper.generateCsvReport(
                            context = context,
                            transactions = filteredTransactions,
                            periodTitle = periodLabel,
                            userName = userName,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            netBalance = netBalance
                        )
                    }
                }

                val mimeType = if (selectedFormat == ExportFormat.PDF) "application/pdf" else "text/csv"
                val subject = "Laporan Finansial $periodLabel"

                if (action == "share") {
                    ExportHelper.shareFile(context, file, mimeType, subject)
                } else {
                    ExportHelper.openFile(context, file, mimeType)
                }

                Toast.makeText(
                    context,
                    "Laporan ${if (selectedFormat == ExportFormat.PDF) "PDF" else "Excel/CSV"} berhasil dibuat!",
                    Toast.LENGTH_SHORT
                ).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuat laporan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isExporting = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("dialog_export_transactions")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.12f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = if (isDark) GoldTarget else NavyHero,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ekspor Catatan Finansial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Unduh & bagikan laporan rapi dalam format PDF atau Excel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Pilihan Format Dokumen
                Text(
                    text = "Pilih Format Berkas",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // PDF Option
                    val isPdf = selectedFormat == ExportFormat.PDF
                    FormatSelectionCard(
                        title = "Dokumen PDF",
                        subtitle = "Elegan, siap cetak & viewer",
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = ExpenseRed,
                        isSelected = isPdf,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedFormat = ExportFormat.PDF }
                    )

                    // Excel/CSV Option
                    val isCsv = selectedFormat == ExportFormat.EXCEL_CSV
                    FormatSelectionCard(
                        title = "Excel / Spreadsheet",
                        subtitle = "Kompatibel XLS/Sheets (.csv)",
                        icon = Icons.Default.TableChart,
                        iconTint = IncomeGreen,
                        isSelected = isCsv,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedFormat = ExportFormat.EXCEL_CSV }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Pilihan Periode
                Text(
                    text = "Periode Laporan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPeriod == ExportPeriod.MONTHLY,
                        onClick = { selectedPeriod = ExportPeriod.MONTHLY },
                        label = { Text("Bulan Ini (${Formatters.formatMonthYear(currentMonth)})") },
                        shape = RoundedCornerShape(99.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) GoldTarget else NavyHero,
                            selectedLabelColor = if (isDark) NavyHeroDark else Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedPeriod == ExportPeriod.ALL_TIME,
                        onClick = { selectedPeriod = ExportPeriod.ALL_TIME },
                        label = { Text("Semua Periode") },
                        shape = RoundedCornerShape(99.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) GoldTarget else NavyHero,
                            selectedLabelColor = if (isDark) NavyHeroDark else Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Filter Tipe Transaksi
                Text(
                    text = "Jenis Transaksi",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == "all",
                        onClick = { selectedTypeFilter = "all" },
                        label = { Text("Semua") },
                        shape = RoundedCornerShape(99.dp)
                    )
                    FilterChip(
                        selected = selectedTypeFilter == "out",
                        onClick = { selectedTypeFilter = "out" },
                        label = { Text("Pengeluaran Saja") },
                        shape = RoundedCornerShape(99.dp)
                    )
                    FilterChip(
                        selected = selectedTypeFilter == "in",
                        onClick = { selectedTypeFilter = "in" },
                        label = { Text("Pemasukan Saja") },
                        shape = RoundedCornerShape(99.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ringkasan Laporan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${filteredTransactions.size} Transaksi",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) GoldTarget else NavyHero,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pemasukan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen
                                )
                                Text(
                                    text = Formatters.formatRupiah(totalIncome),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengeluaran",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRed
                                )
                                Text(
                                    text = Formatters.formatRupiah(totalExpense),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Arus Kas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (netBalance >= 0) "+" else ""}${Formatters.formatRupiah(netBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress state
                AnimatedVisibility(visible = isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = if (isDark) GoldTarget else NavyHero
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sedang menyusun laporan finansial...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    OutlinedButton(
                        onClick = { executeExport("open") },
                        enabled = !isExporting && filteredTransactions.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { executeExport("share") },
                        enabled = !isExporting && filteredTransactions.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) GoldTarget else NavyHero,
                            contentColor = if (isDark) NavyHeroDark else Color.White
                        ),
                        modifier = Modifier
                            .weight(1.7f)
                            .testTag("btn_export_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bagikan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatSelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activeBorderColor = if (isDark) GoldTarget else NavyHero
    val activeBgColor = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.08f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) activeBgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) activeBorderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.14f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Terpilih",
                        tint = activeBorderColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
        }
    }
}
