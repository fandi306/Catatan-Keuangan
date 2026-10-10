package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    text = "Ringkasan Bulanan",
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

            // Income and Expense summary boxes
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

            // Net surplus/deficit status pill
            val netText = if (income == 0L && expense == 0L) {
                "Belum ada pemasukan atau pengeluaran tercatat (Rp 0)"
            } else if (net >= 0) {
                "Surplus Bersih +${Formatters.formatRupiah(net)}"
            } else {
                "Defisit Bersih -${Formatters.formatRupiah(-net)}"
            }

            val netPillBg = if (net >= 0) incomeTextColor.copy(alpha = 0.12f) else expenseTextColor.copy(alpha = 0.12f)
            val netPillColor = if (net >= 0) incomeTextColor else expenseTextColor

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = netPillBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = netText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = netPillColor
                    )
                    if (income > 0) {
                        val saveRatio = ((net.coerceAtLeast(0).toFloat() / income.toFloat()) * 100f).toInt()
                        Text(
                            text = "$saveRatio% tersimpan",
                            style = MaterialTheme.typography.labelSmall,
                            color = netPillColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Mode Selector: Porsi Donut vs Aliran Kas
            var chartViewMode by remember { mutableStateOf("donut") } // "donut" or "flow"

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = chartViewMode == "donut",
                    onClick = { chartViewMode = "donut" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Porsi Kategori", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = chartViewMode == "flow",
                    onClick = { chartViewMode = "flow" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Perbandingan Aliran Kas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryBreakdown.isNotEmpty() && expense > 0L) {
                val topCategory = categoryBreakdown.maxByOrNull { it.second }
                val topPct = if (topCategory != null && expense > 0) ((topCategory.second.toFloat() / expense.toFloat()) * 100f).toInt() else 0

                // Eye-catching Highlight Chip
                if (topCategory != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = getCategoryColor(topCategory.first).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pengeluaran Terbesar:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = topCategory.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = getCategoryColor(topCategory.first)
                                )
                            }
                            Text(
                                text = "$topPct% (${Formatters.formatRupiah(topCategory.second)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = getCategoryColor(topCategory.first)
                            )
                        }
                    }
                }

                if (chartViewMode == "donut") {
                    // Eye-catching Donut Visual Chart & Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Donut Chart Canvas with layered glow
                        Box(
                            modifier = Modifier.size(116.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(108.dp)) {
                                val strokeWidth = 15.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val center = Offset(size.width / 2, size.height / 2)
                                val arcSize = Size(radius * 2, radius * 2)
                                val topLeft = Offset(center.x - radius, center.y - radius)

                                // Background ring
                                drawCircle(
                                    color = Color.Gray.copy(alpha = 0.12f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )

                                var startAngle = -90f
                                for ((cat, amt) in categoryBreakdown) {
                                    val sweep = (amt.toFloat() / expense.toFloat()) * 360f
                                    drawArc(
                                        color = getCategoryColor(cat),
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - 2.5f).coerceAtLeast(1.5f),
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweep
                                }
                            }

                            // Center content
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Pos",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${categoryBreakdown.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kategori",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Top 3 Category Breakdown Badges
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoryBreakdown.take(4).forEach { (cat, amt) ->
                                val pct = ((amt.toFloat() / expense.toFloat()) * 100f).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(cat))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Flow Comparison Mode: Eye-catching side-by-side comparative bars
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val maxFlow = maxOf(income, expense, 1L).toFloat()
                        val incomeRatio = (income.toFloat() / maxFlow).coerceIn(0f, 1f)
                        val expenseRatio = (expense.toFloat() / maxFlow).coerceIn(0f, 1f)
                        val savingsRatio = (net.coerceAtLeast(0).toFloat() / maxFlow).coerceIn(0f, 1f)

                        // Income Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pemasukan", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                Text(Formatters.formatRupiah(income), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(incomeRatio)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen)
                                )
                            }
                        }

                        // Expense Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pengeluaran", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                Text(Formatters.formatRupiah(expense), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(expenseRatio)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed)
                                )
                            }
                        }

                        // Net Savings Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sisa Bersih / Tabungan", style = MaterialTheme.typography.labelSmall, color = if (net >= 0) IncomeGreen else ExpenseRed, fontWeight = FontWeight.Bold)
                                Text(Formatters.formatRupiah(net), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (net >= 0) IncomeGreen else ExpenseRed)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(savingsRatio)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(if (net >= 0) Color(0xFF2ECC71) else ExpenseRed)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bars for each category with percentage badges
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((category, amount) in categoryBreakdown) {
                        val fraction = if (expense > 0) (amount.toFloat() / expense.toFloat()).coerceIn(0f, 1f) else 0f
                        val categoryColor = getCategoryColor(category)
                        val catPct = (fraction * 100f).toInt()

                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "catFraction_$category"
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$catPct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Formatters.formatRupiah(amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
    return com.example.util.BudgetCategoryHelper.getColor(category)
}
