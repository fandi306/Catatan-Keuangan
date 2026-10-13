package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.util.OnlineQuoteManager
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.ExpenseRedDarkBg
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.IncomeGreenDarkBg
import com.example.ui.theme.NavyHero
import com.example.util.BudgetRuleTip
import com.example.util.FinancialAdvice
import com.example.util.FinancialQuote
import com.example.util.FinancialWarning
import com.example.util.WarningSeverity

@Composable
fun WarningBannersSection(
    warnings: List<FinancialWarning>,
    modifier: Modifier = Modifier
) {
    if (warnings.isEmpty()) return
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warning_banners_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (warning in warnings) {
            WarningBannerCard(warning = warning, isDark = isDark)
        }
    }
}

@Composable
fun WarningBannerCard(
    warning: FinancialWarning,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, iconTint, textColor) = when (warning.severity) {
        WarningSeverity.CRITICAL -> {
            val bg = if (isDark) ExpenseRedDarkBg else ExpenseRedBg
            Tuple4(bg, ExpenseRed, ExpenseRed, MaterialTheme.colorScheme.onSurface)
        }
        WarningSeverity.WARNING -> {
            val bg = if (isDark) Color(0xFF332A15) else Color(0xFFFFF8E1)
            Tuple4(bg, Color(0xFFFFA000), Color(0xFFFFA000), MaterialTheme.colorScheme.onSurface)
        }
        WarningSeverity.SUCCESS -> {
            val bg = if (isDark) IncomeGreenDarkBg else IncomeGreenBg
            Tuple4(bg, IncomeGreen, IncomeGreen, MaterialTheme.colorScheme.onSurface)
        }
        WarningSeverity.INFO -> {
            val bg = if (isDark) Color(0xFF1E2842) else Color(0xFFEEF3FF)
            Tuple4(bg, Color(0xFF3D72ED), Color(0xFF3D72ED), MaterialTheme.colorScheme.onSurface)
        }
    }

    val iconVector = when (warning.iconType) {
        "warning_deficit" -> Icons.Default.Warning
        "warning_near_limit" -> Icons.Default.Warning
        "warning_food" -> Icons.Default.Restaurant
        "target_achieved" -> Icons.Default.CheckCircle
        "target_near" -> Icons.Default.AutoAwesome
        else -> Icons.Default.Info
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = warning.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = warning.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun RotatableFinancialQuoteCard(
    advice: FinancialAdvice,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val currentOnlineQuote by OnlineQuoteManager.currentQuote.collectAsState()

    // Gunakan kutipan relevan dari status keuangan saat defisit/target tercapai, atau kutipan terkurasi
    val activeQuote: FinancialQuote = if (advice.hourlyQuote.category == "DEFICIT" || advice.hourlyQuote.category == "TARGET_ACHIEVED") {
        advice.hourlyQuote
    } else {
        currentOnlineQuote
    }

    val cardBg = if (isDark) Color(0xFF1B233D) else Color(0xFFF1F5FD)
    val accentColor = when (activeQuote.category) {
        "DEFICIT" -> ExpenseRed
        "TARGET_ACHIEVED" -> GoldTarget
        "TARGET_NEAR" -> Color(0xFF4F7CFF)
        "SAVING" -> IncomeGreen
        "OJK_FORMULA" -> Color(0xFF009688)
        else -> GoldTarget
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kutipan Finansial",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "“${activeQuote.quote}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "— ${activeQuote.author}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (activeQuote.tip.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = GoldTarget,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeQuote.tip,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OjkBudgetFormulaCard(
    tips: List<BudgetRuleTip>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = IncomeGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Rumus 40-30-20-10 (Saran OJK)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Panduan Resmi untuk Pengendalian Utang & Stabilitas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (tip in tips) {
                    OjkBudgetFormulaItem(tip = tip, isDark = isDark)
                }
            }
        }
    }
}

@Composable
private fun OjkBudgetFormulaItem(
    tip: BudgetRuleTip,
    isDark: Boolean
) {
    val statusColor = when (tip.status) {
        "OK" -> IncomeGreen
        "OVER" -> ExpenseRed
        else -> Color(0xFFFFA000)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Baris atas: Kotak badge rumus di kiri & Kotak status capaian saat ini di kanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) NavyHero.copy(alpha = 0.45f) else GoldTarget.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = tip.formulaBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) GoldTarget else NavyHero,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Saat ini: ${tip.currentPercentage}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Judul Kategori Panduan OJK (Legeh, tidak berebut ruang horizontal)
            Text(
                text = tip.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Catatan dan saran alokasi
            Text(
                text = tip.advice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
