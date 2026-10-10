package com.example.util

import androidx.compose.ui.graphics.Color

enum class BudgetGroup(
    val title: String,
    val idealPercentage: String,
    val badge: String,
    val color: Color
) {
    POKOK(
        title = "Kebutuhan Pokok (Ideal 40%)",
        idealPercentage = "40%",
        badge = "40% Pokok",
        color = Color(0xFF2EC4B6)
    ),
    CICILAN(
        title = "Cicilan & Utang (Maksimal 30%)",
        idealPercentage = "30%",
        badge = "30% Cicilan",
        color = Color(0xFFFF9F1C)
    ),
    TABUNGAN(
        title = "Tabungan & Investasi (Minimal 20%)",
        idealPercentage = "20%",
        badge = "20% Tabungan",
        color = Color(0xFF2ECC71)
    ),
    SOSIAL(
        title = "Donasi & Sosial (Alokasi 10%)",
        idealPercentage = "10%",
        badge = "10% Donasi",
        color = Color(0xFF4D96FF)
    )
}

data class CategoryMeta(
    val name: String,
    val group: BudgetGroup,
    val emoji: String
)

object BudgetCategoryHelper {
    val ALL_CATEGORIES = listOf(
        // Pos 40% Kebutuhan Pokok
        CategoryMeta("Makan", BudgetGroup.POKOK, "🍜"),
        CategoryMeta("Kebutuhan Kos", BudgetGroup.POKOK, "🏠"),
        CategoryMeta("Belanja Bulanan", BudgetGroup.POKOK, "🛒"),
        CategoryMeta("Transportasi", BudgetGroup.POKOK, "🛵"),
        CategoryMeta("Kesehatan", BudgetGroup.POKOK, "💊"),

        // Pos 30% Cicilan & Utang
        CategoryMeta("Cicilan & Kredit", BudgetGroup.CICILAN, "💳"),
        CategoryMeta("Tagihan & Utang", BudgetGroup.CICILAN, "📄"),

        // Pos 20% Tabungan & Investasi
        CategoryMeta("Tabungan Masa Depan", BudgetGroup.TABUNGAN, "💰"),
        CategoryMeta("Investasi & Dana Darurat", BudgetGroup.TABUNGAN, "📈"),

        // Pos 10% Donasi & Sosial / Gaya Hidup
        CategoryMeta("Donasi & Sedekah", BudgetGroup.SOSIAL, "🤲"),
        CategoryMeta("Sosial & Kado", BudgetGroup.SOSIAL, "🎁"),
        CategoryMeta("Hiburan & Rekreasi", BudgetGroup.SOSIAL, "🎮"),
        CategoryMeta("Pengeluaran Lainnya", BudgetGroup.SOSIAL, "📦")
    )

    val CATEGORY_NAMES = ALL_CATEGORIES.map { it.name }

    fun getGroupForCategory(categoryName: String): BudgetGroup {
        val found = ALL_CATEGORIES.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        if (found != null) return found.group

        val lower = categoryName.lowercase(java.util.Locale.ROOT)
        return when {
            lower.contains("cicil") || lower.contains("utang") || lower.contains("kredit") || lower.contains("tagihan") -> BudgetGroup.CICILAN
            lower.contains("tabung") || lower.contains("invest") || lower.contains("darurat") -> BudgetGroup.TABUNGAN
            lower.contains("donasi") || lower.contains("sedekah") || lower.contains("sosial") || lower.contains("hibur") || lower.contains("kado") -> BudgetGroup.SOSIAL
            else -> BudgetGroup.POKOK
        }
    }

    fun getColor(categoryName: String): Color {
        val lower = categoryName.lowercase(java.util.Locale.ROOT)
        return when {
            lower.contains("makan") -> Color(0xFFFF6B6B)
            lower.contains("kos") -> Color(0xFF38B2AC)
            lower.contains("belanja") -> Color(0xFF9B51E0)
            lower.contains("transport") -> Color(0xFF00C9A7)
            lower.contains("sehat") -> Color(0xFF2EC4B6)
            lower.contains("cicil") -> Color(0xFFFF9F1C)
            lower.contains("utang") || lower.contains("tagihan") -> Color(0xFFE63946)
            lower.contains("tabung") -> Color(0xFF2ECC71)
            lower.contains("invest") || lower.contains("darurat") -> Color(0xFF3498DB)
            lower.contains("donasi") || lower.contains("sedekah") || lower.contains("sosial") -> Color(0xFF1ABC9C)
            lower.contains("hibur") -> Color(0xFFFFB142)
            else -> Color(0xFF708090)
        }
    }
}
