package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceipt(
    val amount: Long,
    val merchantOrTitle: String,
    val date: String,
    val category: String,
    val rawText: String,
    val confidence: String
)

object ReceiptScanner {

    /**
     * Parses OCR text from a physical or digital receipt.
     * Extracts:
     * - Total nominal (Total / Grand Total / Jumlah / Bayar / Rp / Subtotal)
     * - Merchant or store name
     * - Date if present
     * - Suggested category (Makan, Belanja, Kesehatan, dll.)
     */
    fun parseReceiptText(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedReceipt(
                amount = 0L,
                merchantOrTitle = "Pengeluaran Nota",
                date = Formatters.currentIsoDate(),
                category = "Makan",
                rawText = "",
                confidence = "Rendah"
            )
        }

        // 1. Detect Merchant / Store Name
        // Usually the first 1-3 lines of a receipt contain the store or restaurant name
        var merchantName = "Nota Belanja"
        for (i in 0 until minOf(4, lines.size)) {
            val candidate = lines[i]
            val lower = candidate.lowercase(Locale.ROOT)
            if (!lower.contains("struk") &&
                !lower.contains("nota") &&
                !lower.contains("bill") &&
                !lower.contains("receipt") &&
                !lower.contains("welcome") &&
                !lower.contains("selamat datang") &&
                !lower.contains("tanggal") &&
                !lower.contains("date") &&
                candidate.length in 3..40
            ) {
                merchantName = candidate
                break
            }
        }

        // 2. Extract Total Amount
        var detectedAmount = 0L
        val totalKeywords = listOf(
            "grand total", "total akhir", "total bayar", "total", "sub total",
            "subtotal", "jumlah bayar", "jumlah", "tagihan", "netto", "tunai", "cash", "rp", "bayar"
        )

        // First pass: look for lines explicitly mentioning total keywords
        for (line in lines.reversed()) {
            val lower = line.lowercase(Locale.ROOT)
            for (kw in totalKeywords) {
                if (lower.contains(kw)) {
                    val extracted = extractLargestNumberFromLine(line)
                    if (extracted > 500L && extracted > detectedAmount) {
                        detectedAmount = extracted
                        break
                    }
                }
            }
            if (detectedAmount > 0) break
        }

        // Second pass: if keyword-based search didn't find, look for all numbers in text and pick the most reasonable maximum
        if (detectedAmount == 0L) {
            var maxCandidate = 0L
            for (line in lines) {
                val num = extractLargestNumberFromLine(line)
                // Filter out likely phone numbers or dates (e.g. 20241010, 08123456789)
                if (num in 1000L..50000000L && num > maxCandidate) {
                    val lower = line.lowercase(Locale.ROOT)
                    if (!lower.contains("telp") && !lower.contains("hp") && !lower.contains("wa") && !lower.contains("tgl")) {
                        maxCandidate = num
                    }
                }
            }
            detectedAmount = maxCandidate
        }

        // 3. Extract Date
        val detectedDate = extractDateFromLines(lines) ?: Formatters.currentIsoDate()

        // 4. Determine Category from text contents
        val fullLower = rawText.lowercase(Locale.ROOT)
        val category = when {
            fullLower.contains("resto") || fullLower.contains("cafe") || fullLower.contains("kopi") ||
            fullLower.contains("coffee") || fullLower.contains("nasi") || fullLower.contains("ayam") ||
            fullLower.contains("mie") || fullLower.contains("bakso") || fullLower.contains("food") ||
            fullLower.contains("minum") || fullLower.contains("makan") || fullLower.contains("warung") ||
            fullLower.contains("dapur") || fullLower.contains("kitchen") -> "Makan"

            fullLower.contains("indomaret") || fullLower.contains("alfamart") || fullLower.contains("supermarket") ||
            fullLower.contains("mart") || fullLower.contains("hypermart") || fullLower.contains("market") ||
            fullLower.contains("toko") || fullLower.contains("belanja") -> "Belanja"

            fullLower.contains("apotek") || fullLower.contains("klinik") || fullLower.contains("kimia farma") ||
            fullLower.contains("obat") || fullLower.contains("dokter") || fullLower.contains("hospital") -> "Kesehatan"

            fullLower.contains("spbu") || fullLower.contains("pertamina") || fullLower.contains("bensin") ||
            fullLower.contains("shell") || fullLower.contains("gojek") || fullLower.contains("grab") ||
            fullLower.contains("parkir") || fullLower.contains("tol") -> "Transportasi"

            fullLower.contains("kos") || fullLower.contains("listrik") || fullLower.contains("pln") ||
            fullLower.contains("pdam") || fullLower.contains("wifi") || fullLower.contains("indihome") -> "Kebutuhan Kos"

            fullLower.contains("bioskop") || fullLower.contains("cinema") || fullLower.contains("game") ||
            fullLower.contains("tiket") -> "Hiburan"

            else -> "Makan"
        }

        val confidence = if (detectedAmount > 0 && merchantName.isNotBlank()) "Tinggi" else "Sedang"

        return ParsedReceipt(
            amount = detectedAmount,
            merchantOrTitle = merchantName,
            date = detectedDate,
            category = category,
            rawText = rawText,
            confidence = confidence
        )
    }

    private fun extractLargestNumberFromLine(line: String): Long {
        // Strip out currencies and punctuation, e.g. "Rp. 45.000,00" -> "45000"
        // Indonesian formats: 45.000 or 45,000 or 45000.00
        val regex = Regex("""(?:Rp\.?|IDR)?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]+)""")
        val matches = regex.findAll(line)
        var maxVal = 0L

        for (match in matches) {
            val numStr = match.groupValues.getOrNull(1) ?: continue
            val clean = normalizeCurrencyString(numStr)
            if (clean > maxVal) {
                maxVal = clean
            }
        }
        return maxVal
    }

    private fun normalizeCurrencyString(raw: String): Long {
        try {
            var s = raw.trim()
            // If ends with ,00 or .00 (cents), remove them
            if (s.endsWith(",00") || s.endsWith(".00")) {
                s = s.substring(0, s.length - 3)
            }
            // Remove remaining thousand separators (dots or commas)
            val digitsOnly = s.replace(".", "").replace(",", "").replace(" ", "")
            return digitsOnly.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            return 0L
        }
    }

    private fun extractDateFromLines(lines: List<String>): String? {
        // Patterns: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD
        val patterns = listOf(
            Pattern.compile("""(\d{2})[-/](\d{2})[-/](\d{4})"""),
            Pattern.compile("""(\d{4})[-/](\d{2})[-/](\d{2})""")
        )

        for (line in lines) {
            for (p in patterns) {
                val m = p.matcher(line)
                if (m.find()) {
                    val g1 = m.group(1) ?: continue
                    val g2 = m.group(2) ?: continue
                    val g3 = m.group(3) ?: continue

                    return if (g1.length == 4) {
                        "$g1-$g2-$g3" // YYYY-MM-DD
                    } else {
                        "$g3-$g2-$g1" // DD-MM-YYYY -> YYYY-MM-DD
                    }
                }
            }
        }
        return null
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }
}
