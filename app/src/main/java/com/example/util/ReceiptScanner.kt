package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ParsedReceipt(
    val amount: Long,
    val merchantOrTitle: String,
    val date: String,
    val category: String,
    val rawText: String,
    val confidence: String
)

object ReceiptScanner {
    private const val TAG = "ReceiptScanner"

    /**
     * Performs On-Device ML Kit Text Recognition on the given image URI.
     * Works 100% offline without API keys or network connection.
     */
    suspend fun scanReceipt(context: Context, uri: Uri): ParsedReceipt = withContext(Dispatchers.IO) {
        val rawText = performOcr(context, uri)
        parseReceiptText(rawText)
    }

    suspend fun performOcr(context: Context, uri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromFilePath(context, uri)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    Log.d(TAG, "ML Kit OCR recognized ${text.length} chars")
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit OCR direct filePath failed, trying bitmap fallback: ${e.message}")
                    val fallbackBitmap = loadSafeBitmap(context, uri, 1200)
                    if (fallbackBitmap != null) {
                        try {
                            val bitmapImage = InputImage.fromBitmap(fallbackBitmap, 0)
                            recognizer.process(bitmapImage)
                                .addOnSuccessListener { visionText ->
                                    continuation.resume(visionText.text)
                                }
                                .addOnFailureListener {
                                    continuation.resume("")
                                }
                        } catch (_: Exception) {
                            continuation.resume("")
                        }
                    } else {
                        continuation.resume("")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating ML Kit OCR: ${e.message}")
            continuation.resume("")
        }
    }

    /**
     * Parses OCR text from a physical or digital receipt.
     */
    fun parseReceiptText(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedReceipt(
                amount = 0L,
                merchantOrTitle = "Nota Belanja",
                date = Formatters.currentIsoDate(),
                category = "Makan",
                rawText = "",
                confidence = "Rendah"
            )
        }

        val fullLower = rawText.lowercase(Locale.ROOT)

        // 1. Detect Merchant / Store Name
        var merchantName = detectKnownBrand(fullLower)

        if (merchantName == null) {
            // Check top lines for reasonable store name
            for (i in 0 until minOf(6, lines.size)) {
                val candidate = lines[i]
                val lower = candidate.lowercase(Locale.ROOT)
                if (!lower.contains("struk") &&
                    !lower.contains("nota") &&
                    !lower.contains("bill") &&
                    !lower.contains("receipt") &&
                    !lower.contains("welcome") &&
                    !lower.contains("selamat datang") &&
                    !lower.contains("tanggal") &&
                    !lower.contains("tgl") &&
                    !lower.contains("date") &&
                    !lower.contains("kasir") &&
                    !lower.contains("cashier") &&
                    !lower.contains("pos") &&
                    !lower.contains("npwp") &&
                    !lower.contains("telp") &&
                    !lower.contains("phone") &&
                    !lower.contains("jl.") &&
                    !lower.contains("jalan") &&
                    !lower.contains("no.") &&
                    !lower.contains("www.") &&
                    !lower.contains("http") &&
                    candidate.length in 3..40
                ) {
                    merchantName = candidate.trim()
                    break
                }
            }
        }

        if (merchantName.isNullOrBlank()) {
            merchantName = "Nota Belanja"
        }

        // 2. Extract Total Amount
        var detectedAmount = 0L

        // Primary total keywords
        val primaryTotalKeywords = listOf(
            "total akhir", "grand total", "total bayar", "jumlah bayar", "tot. bayar",
            "tot bayar", "total belanja", "total tagihan", "harga total", "netto",
            "total :", "total:", "total"
        )
        val secondaryKeywords = listOf(
            "sub total", "subtotal", "tagihan", "jumlah", "bayar"
        )

        // Pass 1: Look for explicit primary total keywords
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase(Locale.ROOT)
            if (lower.contains("kembali") || lower.contains("change") || lower.contains("kembalian")) continue
            if (lower.contains("diskon") || lower.contains("discount") || lower.contains("hemat")) continue

            for (kw in primaryTotalKeywords) {
                if (lower.contains(kw) && !lower.contains("item") && !lower.contains("qty")) {
                    var extracted = extractLargestNumberFromLine(line)
                    // If no number on this line, check the next line! (Common OCR split: line 1 = "TOTAL", line 2 = "45.000")
                    if (extracted == 0L && i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        val nextLower = nextLine.lowercase(Locale.ROOT)
                        if (!nextLower.contains("kembali") && !nextLower.contains("change") && !nextLower.contains("diskon")) {
                            extracted = extractLargestNumberFromLine(nextLine)
                        }
                    }

                    if (extracted > 500L && extracted > detectedAmount) {
                        detectedAmount = extracted
                    }
                }
            }
        }

        // Pass 2: Secondary keywords if no primary total found
        if (detectedAmount == 0L) {
            for (i in lines.indices) {
                val line = lines[i]
                val lower = line.lowercase(Locale.ROOT)
                if (lower.contains("kembali") || lower.contains("change") || lower.contains("kembalian")) continue
                if (lower.contains("diskon") || lower.contains("discount") || lower.contains("hemat")) continue

                for (kw in secondaryKeywords) {
                    if (lower.contains(kw)) {
                        var extracted = extractLargestNumberFromLine(line)
                        if (extracted == 0L && i + 1 < lines.size) {
                            val nextLine = lines[i + 1]
                            val nextLower = nextLine.lowercase(Locale.ROOT)
                            if (!nextLower.contains("kembali") && !nextLower.contains("change")) {
                                extracted = extractLargestNumberFromLine(nextLine)
                            }
                        }
                        if (extracted > 500L && extracted > detectedAmount) {
                            detectedAmount = extracted
                        }
                    }
                }
            }
        }

        // Pass 3: Lines containing "Rp" or currency
        if (detectedAmount == 0L) {
            for (line in lines) {
                val lower = line.lowercase(Locale.ROOT)
                if (lower.contains("kembali") || lower.contains("change") || lower.contains("kembalian")) continue
                if (lower.contains("rp") || lower.contains("idr")) {
                    val extracted = extractLargestNumberFromLine(line)
                    if (extracted in 1000L..50000000L && extracted > detectedAmount) {
                        detectedAmount = extracted
                    }
                }
            }
        }

        // Pass 4: Look for largest reasonable number on lines without noise
        if (detectedAmount == 0L) {
            var maxCandidate = 0L
            for (line in lines) {
                val lower = line.lowercase(Locale.ROOT)
                if (lower.contains("kembali") || lower.contains("change") || lower.contains("telp") ||
                    lower.contains("hp") || lower.contains("wa") || lower.contains("tgl") ||
                    lower.contains("npwp") || lower.contains("kasir") || lower.contains("meja") ||
                    lower.contains("order") || lower.contains("trx") || lower.contains("no.")
                ) {
                    continue
                }
                val num = extractLargestNumberFromLine(line)
                if (num in 1000L..50000000L && num > maxCandidate) {
                    maxCandidate = num
                }
            }
            detectedAmount = maxCandidate
        }

        // 3. Extract Date
        val detectedDate = extractDateFromLines(lines) ?: Formatters.currentIsoDate()

        // 4. Determine Category
        val category = determineCategory(merchantName, fullLower)

        val confidence = if (detectedAmount > 0 && merchantName != "Nota Belanja") "Tinggi" else if (detectedAmount > 0) "Sedang" else "Rendah"

        return ParsedReceipt(
            amount = detectedAmount,
            merchantOrTitle = merchantName,
            date = detectedDate,
            category = category,
            rawText = rawText,
            confidence = confidence
        )
    }

    private fun detectKnownBrand(fullLower: String): String? {
        val brands = listOf(
            "indomaret" to "Indomaret",
            "alfamart" to "Alfamart",
            "alfamidi" to "Alfamidi",
            "superindo" to "Super Indo",
            "super indo" to "Super Indo",
            "transmart" to "Transmart",
            "hypermart" to "Hypermart",
            "lotte mart" to "Lotte Mart",
            "hero" to "Hero Supermarket",
            "familymart" to "FamilyMart",
            "circle k" to "Circle K",
            "lawson" to "Lawson",
            "kfc" to "KFC",
            "mcdonald" to "McDonald's",
            "mcd" to "McDonald's",
            "burger king" to "Burger King",
            "hokben" to "HokBen",
            "hoka hoka bento" to "HokBen",
            "pizza hut" to "Pizza Hut",
            "domino" to "Domino's Pizza",
            "solaria" to "Solaria",
            "mie gacoan" to "Mie Gacoan",
            "gacoan" to "Mie Gacoan",
            "kopi kenangan" to "Kopi Kenangan",
            "janji jiwa" to "Kopi Janji Jiwa",
            "fore coffee" to "Fore Coffee",
            "starbucks" to "Starbucks",
            "point coffee" to "Point Coffee",
            "mixue" to "Mixue",
            "chatime" to "Chatime",
            "tomoro coffee" to "Tomoro Coffee",
            "tomoro" to "Tomoro Coffee",
            "kopi soe" to "Kopi Soe",
            "j.co" to "J.CO Donuts",
            "jco" to "J.CO Donuts",
            "breadtalk" to "BreadTalk",
            "mako" to "Mako Bakery",
            "roti'o" to "Roti'O",
            "rotio" to "Roti'O",
            "holland bakery" to "Holland Bakery",
            "richeese" to "Richeese Factory",
            "yoshinoya" to "Yoshinoya",
            "marugame" to "Marugame Udon",
            "bakmi gm" to "Bakmi GM",
            "guardian" to "Guardian",
            "watsons" to "Watsons",
            "century" to "Century Healthcare",
            "kimia farma" to "Kimia Farma",
            "k-24" to "Apotek K-24",
            "apotek k24" to "Apotek K-24",
            "pertamina" to "SPBU Pertamina",
            "spbu" to "SPBU",
            "shell" to "SPBU Shell",
            "bp-akr" to "SPBU BP",
            "gojek" to "Gojek",
            "gofood" to "GoFood",
            "grab" to "Grab",
            "grabfood" to "GrabFood",
            "shopeefood" to "ShopeeFood",
            "shopee" to "Shopee",
            "tokopedia" to "Tokopedia",
            "miniso" to "Miniso",
            "mr diy" to "MR. D.I.Y.",
            "mr. diy" to "MR. D.I.Y.",
            "ace hardware" to "ACE Hardware",
            "informa" to "Informa",
            "ikea" to "IKEA",
            "cinema xxi" to "Cinema XXI",
            "cgv" to "CGV Cinemas",
            "cinepolis" to "Cinépolis"
        )

        for ((keyword, brandName) in brands) {
            if (fullLower.contains(keyword)) {
                return brandName
            }
        }
        return null
    }

    private fun determineCategory(merchant: String, fullLower: String): String {
        val mLower = merchant.lowercase(Locale.ROOT)
        return when {
            // Food & Drink
            mLower.contains("resto") || mLower.contains("cafe") || mLower.contains("kopi") ||
            mLower.contains("coffee") || mLower.contains("nasi") || mLower.contains("ayam") ||
            mLower.contains("mie") || mLower.contains("bakso") || mLower.contains("food") ||
            mLower.contains("makan") || mLower.contains("warung") || mLower.contains("dapur") ||
            mLower.contains("kitchen") || mLower.contains("kfc") || mLower.contains("mcd") ||
            mLower.contains("hokben") || mLower.contains("solaria") || mLower.contains("gacoan") ||
            mLower.contains("starbucks") || mLower.contains("bakery") || mLower.contains("roti") ||
            fullLower.contains("makan") || fullLower.contains("minum") || fullLower.contains("resto") ||
            fullLower.contains("cafe") || fullLower.contains("kopi") || fullLower.contains("nasi") -> "Makan"

            // Supermarket & Groceries
            mLower.contains("indomaret") || mLower.contains("alfamart") || mLower.contains("alfamidi") ||
            mLower.contains("super indo") || mLower.contains("superindo") || mLower.contains("transmart") ||
            mLower.contains("hypermart") || mLower.contains("mart") || mLower.contains("market") ||
            mLower.contains("supermarket") || mLower.contains("swalayan") || mLower.contains("toko") ||
            fullLower.contains("indomaret") || fullLower.contains("alfamart") || fullLower.contains("belanja") -> "Belanja Bulanan"

            // Pharmacy & Health
            mLower.contains("apotek") || mLower.contains("apotik") || mLower.contains("klinik") ||
            mLower.contains("kimia farma") || mLower.contains("k-24") || mLower.contains("k24") ||
            mLower.contains("guardian") || mLower.contains("watsons") || mLower.contains("century") ||
            mLower.contains("rs") || mLower.contains("rumah sakit") || mLower.contains("obat") ||
            fullLower.contains("apotek") || fullLower.contains("obat") || fullLower.contains("dokter") -> "Kesehatan"

            // Transportation
            mLower.contains("spbu") || mLower.contains("pertamina") || mLower.contains("shell") ||
            mLower.contains("bensin") || mLower.contains("parkir") || mLower.contains("tol") ||
            mLower.contains("gojek") || mLower.contains("grab") || mLower.contains("bluebird") ||
            fullLower.contains("bensin") || fullLower.contains("spbu") || fullLower.contains("parkir") -> "Transportasi"

            // Housing / Kos
            mLower.contains("pln") || mLower.contains("listrik") || mLower.contains("pdam") ||
            mLower.contains("kos") || mLower.contains("kost") || mLower.contains("laundry") ||
            mLower.contains("indihome") || mLower.contains("wifi") ||
            fullLower.contains("listrik") || fullLower.contains("token") || fullLower.contains("pdam") -> "Kebutuhan Kos"

            // Entertainment
            mLower.contains("cinema") || mLower.contains("xxi") || mLower.contains("cgv") ||
            mLower.contains("cinepolis") || mLower.contains("game") || mLower.contains("tiket") ||
            fullLower.contains("bioskop") || fullLower.contains("cinema") -> "Hiburan & Rekreasi"

            else -> "Makan"
        }
    }

    private fun extractLargestNumberFromLine(line: String): Long {
        // Strip out currencies, colons, and punctuation, e.g. "Total : Rp. 45.000,00" -> "45000"
        val regex = Regex("""(?:Rp\.?|IDR)?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{3,})""")
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
            if (s.endsWith(",-") || s.endsWith(".-")) {
                s = s.substring(0, s.length - 2)
            }
            if (s.endsWith(",00") || s.endsWith(".00")) {
                s = s.substring(0, s.length - 3)
            }
            val digitsOnly = s.replace(".", "").replace(",", "").replace(" ", "").replace(":", "")
            return digitsOnly.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            return 0L
        }
    }

    private fun extractDateFromLines(lines: List<String>): String? {
        val patterns = listOf(
            Pattern.compile("""(\d{2})[-/.](\d{2})[-/.](\d{4})"""),
            Pattern.compile("""(\d{4})[-/.](\d{2})[-/.](\d{2})""")
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

    /**
     * Loads a downsampled software bitmap, preventing any hardware bitmap crashes
     * and respecting EXIF orientation tags.
     */
    fun loadSafeBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while (origWidth / sampleSize > maxDimension || origHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedStream = context.contentResolver.openInputStream(uri)
            val rawBitmap = BitmapFactory.decodeStream(decodedStream, null, decodeOptions)
            decodedStream?.close()

            if (rawBitmap == null) return null

            // Handle EXIF orientation rotation
            val rotationDegrees = getExifRotationDegrees(context, uri)
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading safe bitmap: ${e.message}")
            null
        }
    }

    private fun getExifRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }
}
