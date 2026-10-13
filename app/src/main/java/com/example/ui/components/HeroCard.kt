package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavySurfaceDark
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
    showSparklineCurve: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Animasi ringan dan cepat khusus untuk HP kentang (tanpa kalkulasi pegas berat)
    val animSpec = remember { tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing) }

    val animatedProgress by animateFloatAsState(
        targetValue = (targetProgressPct / 100f).coerceIn(0f, 1f),
        animationSpec = animSpec,
        label = "targetProgress"
    )

    val animatedTotalAsset by animateFloatAsState(
        targetValue = totalAsset.toFloat(),
        animationSpec = animSpec,
        label = "totalAsset"
    )

    val animatedLiquidAsset by animateFloatAsState(
        targetValue = liquidAsset.toFloat(),
        animationSpec = animSpec,
        label = "liquidAsset"
    )

    val animatedNonLiquidAsset by animateFloatAsState(
        targetValue = nonLiquidAsset.toFloat(),
        animationSpec = animSpec,
        label = "nonLiquidAsset"
    )

    var selectedAssetType by remember { mutableStateOf("liquid") } // "liquid" or "non_liquid"

    // Kontras warna tegas agar di tablet tidak menyatu dengan background layar
    val cardContainerColor = if (isDark) Color(0xFF141C34) else NavyHero
    val cardBorderColor = if (isDark) Color(0xFF2E3D66) else Color(0xFF25345C)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_total_asset_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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

            if (showSparklineCurve) {
                Spacer(modifier = Modifier.height(10.dp))

                // Mini Achievement Curve Canvas (Eye-catching lightweight sparkline)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val width = size.width
                        val height = size.height

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

                        drawPath(
                            path = path,
                            color = GoldTarget,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

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
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(6.dp))

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

            // User preference: Pilih salah satu saja antara Liquid Aset vs Non-Liquid Aset agar hemat ruang layar HP
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedAssetType == "liquid",
                    onClick = { selectedAssetType = "liquid" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color.White.copy(alpha = 0.22f),
                        activeContentColor = Color.White,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = Color(0xFFC7D0F5)
                    )
                ) {
                    Text("Liquid Aset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedAssetType == "non_liquid",
                    onClick = { selectedAssetType = "non_liquid" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color.White.copy(alpha = 0.22f),
                        activeContentColor = Color.White,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = Color(0xFFC7D0F5)
                    )
                ) {
                    Text("Non-Liquid Aset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display ONLY the selected asset breakdown (short, concise, non-cluttered)
            val isLiquid = selectedAssetType == "liquid"
            val displayAssetAmount = if (isLiquid) animatedLiquidAsset.toLong() else animatedNonLiquidAsset.toLong()
            val assetPercentage = if (totalAsset > 0) {
                ((displayAssetAmount.toFloat() / totalAsset.toFloat()) * 100f).toInt()
            } else 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLiquid) "Liquid Aset" else "Non-Liquid Aset",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E7FF),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldTarget.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$assetPercentage% aset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldTarget,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isLiquid) "Kas tunai, rekening bank, e-wallet" else "Gadget, kendaraan, barang berharga, aset fisik",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5B2E6),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatRupiah(displayAssetAmount),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
