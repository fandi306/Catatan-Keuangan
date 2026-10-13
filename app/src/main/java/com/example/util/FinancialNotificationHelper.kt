package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object FinancialNotificationHelper {
    private const val CHANNEL_ID = "catatan_keuangan_alerts"
    private const val CHANNEL_NAME = "Peringatan & Evaluasi Finansial"
    private const val CHANNEL_DESC = "Pemberitahuan penting batas anggaran, transaksi besar, dan sinkronisasi curhat AI"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun sendTestNotification(context: Context): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_money)
            .setContentTitle("🔔 Notifikasi Control Center Aktif!")
            .setContentText("Panel notifikasi siap memberi peringatan anggaran & hasil sinkronisasi Curhat AI.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Panel notifikasi sistem Anda sekarang aktif! Catatan Keuangan akan memberi kabar saat ada pengeluaran besar, peringatan overbudget, atau ketika transaksi curhatmu berhasil dicatat.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun sendBudgetWarningNotification(
        context: Context,
        currentExpense: Long,
        budgetTarget: Long
    ): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val pct = if (budgetTarget > 0) ((currentExpense.toDouble() / budgetTarget.toDouble()) * 100).toInt() else 100
        val expenseStr = Formatters.formatRupiah(currentExpense)
        val targetStr = Formatters.formatRupiah(budgetTarget)

        val title = if (pct >= 100) "🚨 Pengeluaran Melebihi Batas!" else "⚠️ Waspada Batas Anggaran ($pct%)"
        val message = "Pengeluaran bulan ini sudah $expenseStr dari batas anggaran $targetStr. Yuk rem belanja impulsif & pantau pos pengeluaranmu!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_money)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(1002, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendLargeExpenseNotification(
        context: Context,
        description: String,
        amount: Long,
        account: String
    ): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val amountStr = Formatters.formatRupiah(amount)
        val title = "💳 Transaksi Besar Dicatat"
        val message = "$description sebesar $amountStr telah dicatat pada akun $account. Sisa saldo otomatis diperbarui."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_money)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(1003, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendAiSyncSuccessNotification(
        context: Context,
        count: Int,
        totalAmount: Long,
        account: String
    ): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val totalStr = Formatters.formatRupiah(totalAmount)
        val title = "✨ Curhat Finansial Tersinkronkan"
        val message = "$count transaksi ($totalStr) berhasil dicatat otomatis ke akun $account via Asisten Curhat AI."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_money)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(1004, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendDailyCheckinNotification(context: Context): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val title = "🌙 Evaluasi Finansial Hari Ini"
        val message = "Ada pengeluaran atau uang masuk hari ini? Yuk luangkan 1 menit untuk catat atau curhat ke Finansia AI!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_money)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(1005, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
