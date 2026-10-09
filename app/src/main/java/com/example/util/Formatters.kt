package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val localeId = Locale("id", "ID")
    private val numberFormat = NumberFormat.getNumberInstance(localeId)

    fun formatRupiah(amount: Long, withPrefix: Boolean = true): String {
        val prefix = if (withPrefix) "Rp " else ""
        return if (amount < 0) {
            "-${prefix}${numberFormat.format(kotlin.math.abs(amount))}"
        } else {
            "${prefix}${numberFormat.format(amount)}"
        }
    }

    fun formatNumber(number: Long): String {
        return numberFormat.format(number)
    }

    fun parseAmount(input: String): Long {
        val digitsOnly = input.replace(Regex("\\D"), "")
        return digitsOnly.toLongOrNull() ?: 0L
    }

    fun formatDateIndo(dateStr: String): String {
        // Expects YYYY-MM-DD
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateStr) ?: return dateStr
            val formatter = SimpleDateFormat("d MMM yyyy", localeId)
            formatter.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatTodayFull(): String {
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", localeId)
        return formatter.format(Date())
    }

    fun getGreeting(fullName: String): String {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
        val firstName = fullName.trim().split(" ").firstOrNull()?.ifBlank { "Fandi" } ?: "Fandi"
        return "$timeGreeting, $firstName"
    }

    fun formatMonthYear(yearMonth: String): String {
        // Expects YYYY-MM
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = parser.parse(yearMonth) ?: return yearMonth
            val formatter = SimpleDateFormat("MMMM yyyy", localeId)
            formatter.format(date)
        } catch (_: Exception) {
            yearMonth
        }
    }

    fun currentYearMonth(): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Date())
    }

    fun currentIsoDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }
}
