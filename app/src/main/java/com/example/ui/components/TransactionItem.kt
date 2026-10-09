package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.util.Formatters

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.isIncome
    val iconBg = if (isIncome) IncomeGreenBg else ExpenseRedBg
    val iconColor = if (isIncome) IncomeGreen else ExpenseRed

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("transaction_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon +/-
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isIncome) "+" else "−",
                color = iconColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and subtitles
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val metaText = buildString {
                if (transaction.category.isNotBlank()) {
                    append(transaction.category)
                    append(" · ")
                }
                append(transaction.account)
                append(" · ")
                append(Formatters.formatDateIndo(transaction.date))
            }

            Text(
                text = metaText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount & Red Delete Icon Button
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (if (isIncome) "+" else "-") + Formatters.formatRupiah(transaction.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ExpenseRed.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_transaction_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus transaksi",
                        tint = ExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
