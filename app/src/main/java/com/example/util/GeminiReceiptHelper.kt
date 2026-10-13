package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiReceiptResult(
    val merchant: String,
    val amount: Long,
    val date: String,
    val category: String,
    val isAiPowered: Boolean,
    val notes: String = ""
)

object GeminiReceiptHelper {
    private const val TAG = "GeminiReceiptHelper"
    // gemini-3.8-flash is the active recommended model
    private const val PRIMARY_MODEL = "gemini-3.8-flash"
    private val FALLBACK_MODELS = listOf("gemini-3.8-flash", "gemini-2.5-flash-lite", "gemini-3.1-flash-lite-preview")

    // Fallback key if BuildConfig is empty or set to placeholder
    private const val FALLBACK_KEY = "AIzaSyCsoew3PclNgGAIY39MGpcY68B_rXPCj_c"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    suspend fun analyzeReceipt(context: Context, imageUri: Uri): GeminiReceiptResult = withContext(Dispatchers.IO) {
        // Step 1: Run On-Device ML Kit OCR first (100% offline, guaranteed fast)
        val ocrResult = try {
            ReceiptScanner.scanReceipt(context, imageUri)
        } catch (e: Exception) {
            Log.w(TAG, "On-device OCR failed: ${e.message}")
            ParsedReceipt(
                amount = 0L,
                merchantOrTitle = "Nota Belanja",
                date = Formatters.currentIsoDate(),
                category = "Makan",
                rawText = "",
                confidence = "Rendah"
            )
        }

        // Step 2: Try Cloud Gemini Vision for rich AI extraction
        val safeBitmap = ReceiptScanner.loadSafeBitmap(context, imageUri, 1024)
        val rawApiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        val apiKey = if (rawApiKey.isNotBlank() && rawApiKey != "your_api_key_here") {
            rawApiKey
        } else {
            FALLBACK_KEY
        }

        if (safeBitmap != null && apiKey.isNotBlank()) {
            for (model in FALLBACK_MODELS) {
                try {
                    val aiResult = callGeminiVision(apiKey, safeBitmap, ocrResult.rawText, model)
                    if (aiResult != null && (aiResult.amount > 0 || aiResult.merchant != "Nota Belanja")) {
                        // If AI detected 0 amount but local OCR found a positive amount, complement it!
                        val finalAmount = if (aiResult.amount > 0L) aiResult.amount else ocrResult.amount
                        val finalMerchant = if (aiResult.merchant.isNotBlank() && aiResult.merchant != "Nota Belanja") {
                            aiResult.merchant
                        } else {
                            ocrResult.merchantOrTitle
                        }

                        return@withContext aiResult.copy(
                            amount = finalAmount,
                            merchant = finalMerchant
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini vision with model $model failed: ${e.message}")
                }
            }
        }

        // Step 3: If Gemini failed or was unavailable, use local OCR result!
        if (ocrResult.amount > 0L || ocrResult.merchantOrTitle != "Nota Belanja" || ocrResult.rawText.isNotBlank()) {
            return@withContext GeminiReceiptResult(
                merchant = ocrResult.merchantOrTitle,
                amount = ocrResult.amount,
                date = ocrResult.date,
                category = ocrResult.category,
                isAiPowered = false,
                notes = if (ocrResult.amount > 0L) "Terbaca via On-Device OCR" else "Toko terbaca via OCR"
            )
        }

        // Step 4: Ultimate fallback if completely unreadable
        GeminiReceiptResult(
            merchant = "Nota Belanja",
            amount = 0L,
            date = Formatters.currentIsoDate(),
            category = "Makan",
            isAiPowered = false,
            notes = "Foto nota kurang terbaca jelas. Silakan masukkan nominal manual."
        )
    }

    private fun callGeminiVision(apiKey: String, bitmap: Bitmap, ocrSnippet: String, modelName: String): GeminiReceiptResult? {
        val base64Image = bitmapToBase64(bitmap)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val prompt = buildString {
            appendLine("Anda adalah asisten keuangan pribadi Indonesia. Analisis foto struk/nota belanja ini secara akurat.")
            if (ocrSnippet.isNotBlank()) {
                appendLine("Berikut potongan teks OCR yang terbaca dari nota ini sebagai petunjuk tambahan:")
                appendLine(ocrSnippet.take(500))
            }
            appendLine("Ekstrak data berikut dan kembalikan HANYA format JSON murni:")
            appendLine("{")
            appendLine("  \"merchant\": \"Nama toko / resto / merchant\",")
            appendLine("  \"totalAmount\": 45000,")
            appendLine("  \"date\": \"YYYY-MM-DD\",")
            appendLine("  \"category\": \"Makan\",")
            appendLine("  \"notes\": \"Ringkasan item belanjaan\"")
            appendLine("}")
            appendLine("Aturan:")
            appendLine("- \"totalAmount\" adalah TOTAL AKHIR / GRAND TOTAL / TOTAL BAYAR belanjaan yang harus dibayar (integer positif rupiah, bukan uang tunai diserahkan atau kembalian).")
            appendLine("- \"category\" pilih salah satu dari: \"Makan\", \"Kebutuhan Kos\", \"Belanja Bulanan\", \"Transportasi\", \"Kesehatan\", \"Cicilan & Kredit\", \"Donasi & Sedekah\", \"Hiburan & Rekreasi\", \"Pengeluaran Lainnya\".")
            appendLine("- \"date\" gunakan tanggal pada nota berformat YYYY-MM-DD jika terbaca, atau gunakan tanggal hari ini.")
            appendLine("- \"merchant\" adalah nama toko/warung/restoran di bagian atas struk.")
        }

        val jsonPayload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error HTTP ${response.code}: ${response.body?.string()}")
                return null
            }

            val bodyString = response.body?.string() ?: return null
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val fullTextBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                fullTextBuilder.append(part.optString("text", ""))
            }

            val textResponse = fullTextBuilder.toString()
            val startIndex = textResponse.indexOf('{')
            val endIndex = textResponse.lastIndexOf('}')
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                return null
            }

            val cleanJson = textResponse.substring(startIndex, endIndex + 1)
            val parsedObj = JSONObject(cleanJson)

            val merchant = parsedObj.optString("merchant", "Nota Belanja").ifBlank { "Nota Belanja" }
            val totalAmount = parsedObj.optLong("totalAmount", 0L)
            val date = parsedObj.optString("date", Formatters.currentIsoDate()).ifBlank { Formatters.currentIsoDate() }
            val category = parsedObj.optString("category", "Makan").ifBlank { "Makan" }
            val notes = parsedObj.optString("notes", "")

            return GeminiReceiptResult(
                merchant = merchant,
                amount = totalAmount,
                date = date,
                category = category,
                isAiPowered = true,
                notes = notes
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
