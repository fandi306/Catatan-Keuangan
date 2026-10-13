package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN_X = 36f
    private const val MARGIN_RIGHT = 559f // PAGE_WIDTH - MARGIN_X

    // Brand Colors
    private const val COLOR_NAVY = 0xFF17203A.toInt()
    private const val COLOR_GOLD = 0xFFF2B53D.toInt()
    private const val COLOR_GREEN = 0xFF0B8F6F.toInt()
    private const val COLOR_GREEN_BG = 0xFFF0FDF4.toInt()
    private const val COLOR_GREEN_BORDER = 0xFFBBF7D0.toInt()
    private const val COLOR_RED = 0xFFD8404F.toInt()
    private const val COLOR_RED_BG = 0xFFFEF2F2.toInt()
    private const val COLOR_RED_BORDER = 0xFFFECACA.toInt()
    private const val COLOR_SLATE_BG = 0xFFF8FAFC.toInt()
    private const val COLOR_BORDER = 0xFFE2E8F0.toInt()
    private const val COLOR_MUTED = 0xFF64748B.toInt()
    private const val COLOR_DARK_TEXT = 0xFF0F172A.toInt()

    fun generatePdfReport(
        context: Context,
        transactions: List<Transaction>,
        periodTitle: String,
        userName: String,
        totalIncome: Long,
        totalExpense: Long,
        netBalance: Long
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val safeSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val pdfFile = File(exportDir, "Laporan_Keuangan_$safeSuffix.pdf")

        val document = PdfDocument()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        val printDateStr = SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())

        // Calculate pages
        val rowsPage1 = 24
        val rowsSubsequent = 29
        val totalPages = if (transactions.isEmpty() || transactions.size <= rowsPage1) {
            1
        } else {
            1 + kotlin.math.ceil((transactions.size - rowsPage1).toDouble() / rowsSubsequent).toInt()
        }

        var currentTxIndex = 0
        var pageNum = 1

        while (pageNum <= totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Top Accent Ribbon (Gold)
            fillPaint.color = COLOR_GOLD
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, fillPaint)

            var currentY: Float

            if (pageNum == 1) {
                // Header Page 1
                fillPaint.color = COLOR_NAVY
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 18f
                textPaint.color = COLOR_NAVY
                canvas.drawText("LAPORAN KEUANGAN PRIBADI", MARGIN_X, 36f, textPaint)

                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.DEFAULT
                textPaint.color = COLOR_MUTED
                canvas.drawText("Catatan Keuangan App · Periode: $periodTitle", MARGIN_X, 50f, textPaint)

                // Right metadata
                val nameText = if (userName.isNotBlank()) "Pengguna: $userName" else "Pengguna Terdaftar"
                textPaint.color = COLOR_DARK_TEXT
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val nameWidth = textPaint.measureText(nameText)
                canvas.drawText(nameText, MARGIN_RIGHT - nameWidth, 36f, textPaint)

                textPaint.typeface = Typeface.DEFAULT
                textPaint.color = COLOR_MUTED
                textPaint.textSize = 8.5f
                val dateWidth = textPaint.measureText("Cetak: $printDateStr")
                canvas.drawText("Cetak: $printDateStr", MARGIN_RIGHT - dateWidth, 50f, textPaint)

                // Divider line
                strokePaint.color = COLOR_BORDER
                strokePaint.strokeWidth = 1f
                canvas.drawLine(MARGIN_X, 62f, MARGIN_RIGHT, 62f, strokePaint)

                // 2. Executive Summary Cards (3 cards)
                val cardY = 74f
                val cardHeight = 52f
                val cardSpacing = 10f
                val totalWidth = MARGIN_RIGHT - MARGIN_X
                val cardWidth = (totalWidth - (cardSpacing * 2)) / 3f

                // Card 1: Pemasukan
                drawSummaryCard(
                    canvas = canvas,
                    x = MARGIN_X,
                    y = cardY,
                    w = cardWidth,
                    h = cardHeight,
                    bgColor = COLOR_GREEN_BG,
                    borderColor = COLOR_GREEN_BORDER,
                    title = "TOTAL PEMASUKAN",
                    titleColor = COLOR_GREEN,
                    value = "+ ${Formatters.formatRupiah(totalIncome)}",
                    valueColor = COLOR_GREEN,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint
                )

                // Card 2: Pengeluaran
                drawSummaryCard(
                    canvas = canvas,
                    x = MARGIN_X + cardWidth + cardSpacing,
                    y = cardY,
                    w = cardWidth,
                    h = cardHeight,
                    bgColor = COLOR_RED_BG,
                    borderColor = COLOR_RED_BORDER,
                    title = "TOTAL PENGELUARAN",
                    titleColor = COLOR_RED,
                    value = "- ${Formatters.formatRupiah(totalExpense)}",
                    valueColor = COLOR_RED,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint
                )

                // Card 3: Saldo Bersih
                val netColor = if (netBalance >= 0) COLOR_GREEN else COLOR_RED
                drawSummaryCard(
                    canvas = canvas,
                    x = MARGIN_X + (cardWidth + cardSpacing) * 2,
                    y = cardY,
                    w = cardWidth,
                    h = cardHeight,
                    bgColor = COLOR_SLATE_BG,
                    borderColor = COLOR_BORDER,
                    title = "SALDO BERSIH (ARUS KAS)",
                    titleColor = COLOR_NAVY,
                    value = (if (netBalance >= 0) "+ " else "") + Formatters.formatRupiah(netBalance),
                    valueColor = netColor,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint
                )

                currentY = 142f
            } else {
                // Continuation Header for Page 2+
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 12f
                textPaint.color = COLOR_NAVY
                canvas.drawText("LAPORAN KEUANGAN PRIBADI (Lanjutan)", MARGIN_X, 32f, textPaint)

                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 9f
                textPaint.color = COLOR_MUTED
                canvas.drawText("Periode: $periodTitle · Halaman $pageNum dari $totalPages", MARGIN_X, 45f, textPaint)

                strokePaint.color = COLOR_BORDER
                strokePaint.strokeWidth = 1f
                canvas.drawLine(MARGIN_X, 54f, MARGIN_RIGHT, 54f, strokePaint)

                currentY = 64f
            }

            // 3. Transactions Table Header
            val tableHeaderHeight = 22f
            fillPaint.color = COLOR_NAVY
            canvas.drawRoundRect(
                RectF(MARGIN_X, currentY, MARGIN_RIGHT, currentY + tableHeaderHeight),
                4f,
                4f,
                fillPaint
            )

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.color = Color.WHITE

            val colNoX = MARGIN_X + 6f
            val colDateX = MARGIN_X + 30f
            val colCatX = MARGIN_X + 98f
            val colAccX = MARGIN_X + 175f
            val colDescX = MARGIN_X + 245f
            val colAmountX = MARGIN_RIGHT - 6f

            val headerBaseline = currentY + 14.5f
            canvas.drawText("No", colNoX, headerBaseline, textPaint)
            canvas.drawText("Tanggal", colDateX, headerBaseline, textPaint)
            canvas.drawText("Kategori", colCatX, headerBaseline, textPaint)
            canvas.drawText("Dompet", colAccX, headerBaseline, textPaint)
            canvas.drawText("Keterangan", colDescX, headerBaseline, textPaint)

            val amountLabel = "Nominal (Rp)"
            val amountLabelWidth = textPaint.measureText(amountLabel)
            canvas.drawText(amountLabel, colAmountX - amountLabelWidth, headerBaseline, textPaint)

            currentY += tableHeaderHeight + 2f

            // 4. Render Table Rows for this page
            val maxRowsThisPage = if (pageNum == 1) rowsPage1 else rowsSubsequent
            var rowsDrawnThisPage = 0

            if (transactions.isEmpty()) {
                // Empty state card
                fillPaint.color = COLOR_SLATE_BG
                canvas.drawRoundRect(
                    RectF(MARGIN_X, currentY + 10f, MARGIN_RIGHT, currentY + 60f),
                    8f,
                    8f,
                    fillPaint
                )
                textPaint.color = COLOR_MUTED
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.DEFAULT
                val emptyMsg = "Tidak ada catatan transaksi untuk periode $periodTitle."
                val emptyWidth = textPaint.measureText(emptyMsg)
                canvas.drawText(emptyMsg, (PAGE_WIDTH - emptyWidth) / 2f, currentY + 38f, textPaint)
            } else {
                while (currentTxIndex < transactions.size && rowsDrawnThisPage < maxRowsThisPage) {
                    val tx = transactions[currentTxIndex]
                    val rowHeight = 22f

                    // Alternating background
                    if (rowsDrawnThisPage % 2 == 1) {
                        fillPaint.color = COLOR_SLATE_BG
                        canvas.drawRect(MARGIN_X, currentY, MARGIN_RIGHT, currentY + rowHeight, fillPaint)
                    }

                    // Bottom thin line
                    strokePaint.color = 0xFFF1F5F9.toInt()
                    strokePaint.strokeWidth = 0.5f
                    canvas.drawLine(MARGIN_X, currentY + rowHeight, MARGIN_RIGHT, currentY + rowHeight, strokePaint)

                    val rowBaseline = currentY + 14.5f

                    // No
                    textPaint.typeface = Typeface.DEFAULT
                    textPaint.textSize = 8f
                    textPaint.color = COLOR_MUTED
                    canvas.drawText("${currentTxIndex + 1}", colNoX, rowBaseline, textPaint)

                    // Tanggal
                    val formattedDate = Formatters.formatDateIndo(tx.date)
                    textPaint.color = COLOR_DARK_TEXT
                    canvas.drawText(truncateText(formattedDate, 12), colDateX, rowBaseline, textPaint)

                    // Kategori
                    val catText = if (tx.category.isNotBlank()) tx.category else "Lainnya"
                    textPaint.color = COLOR_NAVY
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(truncateText(catText, 14), colCatX, rowBaseline, textPaint)

                    // Dompet/Akun
                    textPaint.typeface = Typeface.DEFAULT
                    textPaint.color = COLOR_MUTED
                    val accText = if (tx.account.isNotBlank()) tx.account else "-"
                    canvas.drawText(truncateText(accText, 13), colAccX, rowBaseline, textPaint)

                    // Keterangan
                    textPaint.color = COLOR_DARK_TEXT
                    val descText = if (tx.description.isNotBlank()) tx.description else "-"
                    canvas.drawText(truncateText(descText, 25), colDescX, rowBaseline, textPaint)

                    // Nominal
                    val isIncome = tx.type == "in"
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.color = if (isIncome) COLOR_GREEN else COLOR_RED
                    val amountStr = "${if (isIncome) "+" else "-"} ${Formatters.formatRupiah(tx.amount)}"
                    val amountWidth = textPaint.measureText(amountStr)
                    canvas.drawText(amountStr, colAmountX - amountWidth, rowBaseline, textPaint)

                    currentY += rowHeight
                    currentTxIndex++
                    rowsDrawnThisPage++
                }
            }

            // 5. Footer on bottom of each page
            val footerY = 812f
            strokePaint.color = COLOR_BORDER
            strokePaint.strokeWidth = 0.8f
            canvas.drawLine(MARGIN_X, footerY, MARGIN_RIGHT, footerY, strokePaint)

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 7.5f
            textPaint.color = COLOR_MUTED
            canvas.drawText(
                "Catatan Keuangan App · Laporan Finansial Pribadi Resmi · Dokumen dibuat otomatis",
                MARGIN_X,
                footerY + 14f,
                textPaint
            )

            val pageStr = "Halaman $pageNum dari $totalPages"
            val pageStrWidth = textPaint.measureText(pageStr)
            canvas.drawText(pageStr, MARGIN_RIGHT - pageStrWidth, footerY + 14f, textPaint)

            document.finishPage(page)
            pageNum++
        }

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        bgColor: Int,
        borderColor: Int,
        title: String,
        titleColor: Int,
        value: String,
        valueColor: Int,
        fillPaint: Paint,
        strokePaint: Paint,
        textPaint: Paint
    ) {
        val rect = RectF(x, y, x + w, y + h)

        // Background
        fillPaint.color = bgColor
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)

        // Border
        strokePaint.color = borderColor
        strokePaint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

        // Title
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = titleColor
        canvas.drawText(title, x + 8f, y + 17f, textPaint)

        // Value
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10.5f
        textPaint.color = valueColor
        canvas.drawText(value, x + 8f, y + 36f, textPaint)
    }

    fun generateCsvReport(
        context: Context,
        transactions: List<Transaction>,
        periodTitle: String,
        userName: String,
        totalIncome: Long,
        totalExpense: Long,
        netBalance: Long
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val safeSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val csvFile = File(exportDir, "Laporan_Keuangan_$safeSuffix.csv")

        val printDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        FileOutputStream(csvFile).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                // Write UTF-8 BOM so Microsoft Excel automatically recognizes Indonesian accents and UTF-8 encoding
                writer.write("\uFEFF")

                // Header Metadata
                writer.write("\"LAPORAN KEUANGAN PRIBADI\"\n")
                writer.write("\"Aplikasi:\",\"Catatan Keuangan\"\n")
                writer.write("\"Pengguna:\",\"${escapeCsv(userName)}\"\n")
                writer.write("\"Periode:\",\"${escapeCsv(periodTitle)}\"\n")
                writer.write("\"Tanggal Ekspor:\",\"$printDateStr\"\n")
                writer.write("\"Total Transaksi:\",\"${transactions.size}\"\n")
                writer.write("\"Total Pemasukan (Rp):\",$totalIncome,\"${Formatters.formatRupiah(totalIncome)}\"\n")
                writer.write("\"Total Pengeluaran (Rp):\",$totalExpense,\"${Formatters.formatRupiah(totalExpense)}\"\n")
                writer.write("\"Saldo Bersih (Rp):\",$netBalance,\"${Formatters.formatRupiah(netBalance)}\"\n")
                writer.write("\n")

                // Column Headers
                writer.write("\"No\",\"Tanggal\",\"Tipe\",\"Kategori\",\"Akun/Dompet\",\"Keterangan\",\"Nominal (Angka)\",\"Nominal (Format Rupiah)\"\n")

                // Rows
                transactions.forEachIndexed { index, tx ->
                    val no = index + 1
                    val date = tx.date
                    val type = if (tx.type == "in") "Pemasukan" else "Pengeluaran"
                    val cat = escapeCsv(if (tx.category.isNotBlank()) tx.category else "Lainnya")
                    val acc = escapeCsv(if (tx.account.isNotBlank()) tx.account else "-")
                    val desc = escapeCsv(if (tx.description.isNotBlank()) tx.description else "-")
                    val amountRaw = if (tx.type == "in") tx.amount else -tx.amount
                    val amountFormatted = escapeCsv("${if (tx.type == "in") "+" else "-"} ${Formatters.formatRupiah(tx.amount)}")

                    writer.write("$no,\"$date\",\"$type\",\"$cat\",\"$acc\",\"$desc\",$amountRaw,\"$amountFormatted\"\n")
                }
            }
        }

        return csvFile
    }

    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"")
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else "${text.take(maxLength - 1)}…"
    }

    fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah lampiran $subject dari aplikasi Catatan Keuangan.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Bagikan $subject")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(intent, "Buka Berkas Laporan")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
