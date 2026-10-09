package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.NavySurfaceDark
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.NavyHero
import com.example.util.Formatters

@Composable
fun HeroCard(
    totalAsset: Long,
    liquidAsset: Long,
    nonLiquidAsset: Long,
    savingsTarget: Long,
    targetProgressPct: Float,
    onEditTargetClick: () -> Unit,
    onViewAnalyticsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val animatedProgress by animateFloatAsState(
        targetValue = (targetProgressPct / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "targetProgress"
    )

    val animatedTotalAsset by animateFloatAsState(
        targetValue = totalAsset.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "totalAsset"
    )

    val animatedLiquidAsset by animateFloatAsState(
        targetValue = liquidAsset.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "liquidAsset"
    )

    val animatedNonLiquidAsset by animateFloatAsState(
        targetValue = nonLiquidAsset.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "nonLiquidAsset"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_total_asset_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NavySurfaceDark else NavyHero
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total aset",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB9C2E0)
                )

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    val targetLabel = if (savingsTarget > 0L) {
                        "Target: ${Formatters.formatRupiah(savingsTarget)}"
                    } else {
                        "Target: Belum diatur"
                    }
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldTarget,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = Formatters.formatRupiah(animatedTotalAsset.toLong()),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )

            if (totalAsset == 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Saldo Anda saat ini Rp 0. Ketuk '+ Catat transaksi' di bawah untuk mulai mengisi saldo rekening atau kas Anda.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE2E7FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini Achievement Curve Canvas (Eye-catching lightweight sparkline)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height

                    // Dynamic control points representing achievement slope
                    val progressRatio = animatedProgress.coerceIn(0.05f, 1f)
                    val p0 = Offset(0f, height * 0.85f)
                    val p1 = Offset(width * 0.35f, height * 0.70f)
                    val p2 = Offset(width * 0.70f, height * (0.85f - (0.50f * progressRatio)))
                    val p3 = Offset(width, height * (0.90f - (0.80f * progressRatio)))

                    val path = Path().apply {
                        moveTo(p0.x, p0.y)
                        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
                    }

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    // Soft gradient under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GoldTarget.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Curve stroke
                    drawPath(
                        path = path,
                        color = GoldTarget,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // End indicator pulse point
                    drawCircle(
                        color = GoldTarget,
                        radius = 4.5.dp.toPx(),
                        center = p3
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = p3
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(GoldTarget)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val targetText = if (savingsTarget > 0L) {
                    "${String.format(java.util.Locale.US, "%.1f", targetProgressPct).replace('.', ',')}% dari target tercapai"
                } else {
                    "Target belum diatur (0%)"
                }
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                TextButton(
                    onClick = onEditTargetClick,
                    modifier = Modifier.testTag("edit_target_button")
                ) {
                    Text(
                        text = if (savingsTarget > 0L) "Ubah Target" else "+ Atur Target",
                        color = GoldTarget,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Split 2 columns: Liquid and Non-Liquid assets with in-column explanations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "Aset Lancar / Liquid Asset",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6DCFC),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(Cash, tabungan, e-wallet, dll.)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5B2E6),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatRupiah(animatedLiquidAsset.toLong()),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "Aset Tdk Lancar / Non-Liquid",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6DCFC),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(Handphone, emas, aset, dll.)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5B2E6),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatRupiah(animatedNonLiquidAsset.toLong()),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
