package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CatEmergency
import com.example.ui.theme.CatFood
import com.example.ui.theme.CatKos
import com.example.ui.theme.CatOthers
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedDarkBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenDarkBg
import com.example.util.Formatters

@Composable
fun MonthlySummaryCard(
    selectedMonth: String,
    income: Long,
    expense: Long,
    net: Long,
    categoryBreakdown: List<Pair<String, Long>>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val incomeCardBg = if (isDark) IncomeGreenDarkBg else IncomeGreenBg
    val incomeTextColor = if (isDark) IncomeGreenDark else IncomeGreen
    val expenseCardBg = if (isDark) ExpenseRedDarkBg else ExpenseRedBg
    val expenseTextColor = if (isDark) ExpenseRedDark else ExpenseRed

    val animatedIncome by animateFloatAsState(
        targetValue = income.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animIncome"
    )

    val animatedExpense by animateFloatAsState(
        targetValue = expense.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animExpense"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header with month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledIconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("prev_month_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan sebelumnya"
                        )
                    }

                    AnimatedContent(
                        targetState = selectedMonth,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "monthTransition"
                    ) { monthTarget ->
                        Text(
                            text = Formatters.formatMonthYear(monthTarget),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("next_month_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan berikutnya"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Income and Expense summary boxes with smooth animated values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = incomeCardBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Pemasukan",
                            style = MaterialTheme.typography.labelSmall,
                            color = incomeTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatRupiah(animatedIncome.toLong()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = incomeTextColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = expenseCardBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.labelSmall,
                            color = expenseTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatRupiah(animatedExpense.toLong()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = expenseTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net surplus/deficit text
            val netText = if (income == 0L && expense == 0L) {
                "Belum ada pemasukan atau pengeluaran yang tercatat (Rp 0)"
            } else if (net >= 0) {
                "Bulan ini surplus ${Formatters.formatRupiah(net)}"
            } else {
                "Bulan ini defisit ${Formatters.formatRupiah(-net)}"
            }
            Text(
                text = netText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            if (income == 0L && expense == 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Kolom ini merangkum total arus kas Anda. Begitu Anda mencatat transaksi pemasukan atau pengeluaran, perbandingan dan persentase belanja per kategori akan otomatis dihitung di sini.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Categories breakdown
            if (categoryBreakdown.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((category, amount) in categoryBreakdown) {
                        val fraction = if (expense > 0) (amount.toFloat() / expense.toFloat()).coerceIn(0f, 1f) else 0f
                        val categoryColor = getCategoryColor(category)

                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "catFraction_$category"
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Formatters.formatRupiah(amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedFraction)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Belum ada pengeluaran di bulan ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Makan" -> CatFood
        "Kebutuhan Kos" -> CatKos
        "Pengeluaran Darurat" -> CatEmergency
        "Pembayaran Lainnya" -> CatOthers
        "Transportasi" -> Color(0xFF00B4D8)
        "Hiburan" -> Color(0xFFFF9F1C)
        "Belanja" -> Color(0xFFE056FD)
        "Kesehatan" -> Color(0xFF2EC4B6)
        else -> Color(0xFF6C757D)
    }
}
