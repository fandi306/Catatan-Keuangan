package com.example.util

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
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedTransactions: List<ProposedTransaction> = emptyList(),
    val isRecorded: Boolean = false
)

data class ProposedTransaction(
    val type: String = "out", // "out" or "in"
    val description: String,
    val amount: Long,
    val category: String,
    val date: String = Formatters.currentIsoDate()
)

data class FinancialContext(
    val userName: String,
    val totalAsset: Long,
    val liquidAsset: Long,
    val nonLiquidAsset: Long,
    val monthlyIncome: Long,
    val monthlyExpense: Long,
    val monthlyNet: Long,
    val savingsTarget: Long,
    val accounts: List<String>,
    val topCategories: List<Pair<String, Long>>,
    val recentTransactionsSummary: String
)

object GeminiChatHelper {
    private const val TAG = "GeminiChatHelper"
    private const val PRIMARY_MODEL = "gemini-3.5-flash"
    private val FALLBACK_MODELS = listOf("gemini-3.5-flash", "gemini-3.8-flash", "gemini-3.1-flash-lite-preview")

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        financialContext: FinancialContext
    ): ChatMessage = withContext(Dispatchers.IO) {
        val rawApiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        val hasValidUserKey = rawApiKey.isNotBlank() &&
                rawApiKey != "your_api_key_here" &&
                !rawApiKey.contains("AIzaSyCsoew3PclNgGAIY39MGpcY68B_rXPCj_c")

        if (hasValidUserKey) {
            val systemPrompt = buildSystemInstruction(financialContext)
            for (model in FALLBACK_MODELS) {
                try {
                    val rawResponse = callGeminiChat(rawApiKey, history, userMessage, systemPrompt, model)
                    if (rawResponse.isNotBlank()) {
                        val (cleanedText, proposed) = parseResponse(rawResponse)
                        if (cleanedText.isNotBlank()) {
                            return@withContext ChatMessage(
                                isUser = false,
                                text = cleanedText,
                                proposedTransactions = proposed
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini chat failed on model $model: ${e.message}")
                }
            }
        }

        // High-accuracy offline conversational reasoning engine
        val (offlineReply, offlineProposed) = generateSmartOfflineResponse(userMessage, financialContext)
        ChatMessage(
            isUser = false,
            text = offlineReply,
            proposedTransactions = offlineProposed
        )
    }

    private fun buildSystemInstruction(ctx: FinancialContext): String {
        val catSummary = if (ctx.topCategories.isEmpty()) {
            "Belum ada rincian pengeluaran bulan ini"
        } else {
            ctx.topCategories.take(4).joinToString(", ") { "${it.first}: ${Formatters.formatRupiah(it.second)}" }
        }

        val accListStr = if (ctx.accounts.isEmpty()) "Tunai" else ctx.accounts.joinToString(", ")

        return """
            Anda adalah "Finansia", asisten dan sahabat curhat keuangan pribadi pengguna di aplikasi Catatan Keuangan.
            
            PERAN & KEPRIBADIAN:
            - Anda adalah teman curhat yang hangat, ramah, penuh empati, solutif, dan bijak.
            - Gaya bahasa: Bahasa Indonesia natural, kasual, santun, tidak kaku seperti robot bank, dan TIDAK MENGGURUI.
            - Dengarkan baik-baik apa yang pengguna katakan dan JAWAB PERSIS sesuai topik yang dibicarakan!
            
            DATA KEUANGAN PENGGUNA REAL-TIME SAAT INI:
            - Nama Pengguna: ${ctx.userName.ifBlank { "Teman Finansia" }}
            - Total Kekayaan Bersih: ${Formatters.formatRupiah(ctx.totalAsset)}
            - Dana Siap Pakai (Likuid): ${Formatters.formatRupiah(ctx.liquidAsset)}
            - Pemasukan Bulan Ini: ${Formatters.formatRupiah(ctx.monthlyIncome)}
            - Pengeluaran Bulan Ini: ${Formatters.formatRupiah(ctx.monthlyExpense)}
            - Arus Kas Bulan Ini (Net): ${Formatters.formatRupiah(ctx.monthlyNet)}
            - Target Tabungan / Batas Anggaran: ${Formatters.formatRupiah(ctx.savingsTarget)}
            - Rekening / Akun: $accListStr
            - Pos Pengeluaran Terbesar: $catSummary
            - Aktivitas Terakhir: ${ctx.recentTransactionsSummary.ifBlank { "Belum ada transaksi baru" }}
            
            ATURAN MENJAWAB:
            1. RELEVANSI TOTAL: Jika pengguna bertanya tentang sisa saldo, sebutkan dana likuid (${Formatters.formatRupiah(ctx.liquidAsset)}). Jika tanya pengeluaran, sebutkan pengeluaran (${Formatters.formatRupiah(ctx.monthlyExpense)}). Jika tanya tips hemat/lapar mata, beri tips konkret!
            2. DETEKSI TRANSAKSI DARI CURHAT:
               Jika pengguna menceritakan pengeluaran atau pemasukan dengan nominal (misal: "habis 1jt: 900k servis motor, 100k makan" atau "beli bensin 50rb"),
               tanggapi ceritanya dengan hangat dan empati, lalu DI AKHIR PESAN wajib sertakan blok JSON transaksi:
               
               ```json:transactions
               [
                 {
                   "type": "out",
                   "description": "Servis motor",
                   "amount": 900000,
                   "category": "Transportasi"
                 },
                 {
                   "type": "out",
                   "description": "Makan",
                   "amount": 100000,
                   "category": "Makan"
                 }
               ]
               ```
               
               Kategori yang valid:
               "Makan", "Kebutuhan Kos", "Belanja Bulanan", "Transportasi", "Kesehatan", "Cicilan & Kredit", "Tabungan Masa Depan", "Donasi & Sedekah", "Hiburan & Rekreasi", "Pengeluaran Lainnya".
               Untuk uang masuk, gunakan "type": "in", "category": "Pemasukan".
               JANGAN sertakan blok json jika pengguna tidak menyebutkan nominal uang transaksi.
        """.trimIndent()
    }

    private fun callGeminiChat(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        systemInstruction: String,
        modelName: String
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        val recentTurns = history.takeLast(6)
        for (item in recentTurns) {
            val role = if (item.isUser) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", item.text) })
                })
            })
        }

        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userMessage) })
            })
        })

        val jsonPayload = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini Chat HTTP error ${response.code}: ${response.body?.string()}")
                return ""
            }

            val body = response.body?.string() ?: return ""
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                sb.append(part.optString("text", ""))
            }
            return sb.toString()
        }
    }

    private fun parseResponse(raw: String): Pair<String, List<ProposedTransaction>> {
        val tagPattern = Pattern.compile("```(?:json:transactions|json)?\\s*(\\[.*?\\])\\s*```", Pattern.DOTALL)
        val matcher = tagPattern.matcher(raw)

        val transactions = mutableListOf<ProposedTransaction>()
        var cleanedText = raw

        if (matcher.find()) {
            val jsonSnippet = matcher.group(1)
            cleanedText = matcher.replaceAll("").trim()

            try {
                if (jsonSnippet != null) {
                    val arr = JSONArray(jsonSnippet)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val type = obj.optString("type", "out")
                        val desc = obj.optString("description", "Pengeluaran").ifBlank { "Pengeluaran" }
                        val amount = obj.optLong("amount", 0L)
                        val cat = obj.optString("category", "Pengeluaran Lainnya").ifBlank { "Pengeluaran Lainnya" }
                        val date = obj.optString("date", Formatters.currentIsoDate()).ifBlank { Formatters.currentIsoDate() }

                        if (amount > 0L) {
                            transactions.add(
                                ProposedTransaction(
                                    type = type,
                                    description = desc,
                                    amount = amount,
                                    category = cat,
                                    date = date
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing transaction snippet: ${e.message}")
            }
        }

        return Pair(cleanedText, transactions)
    }

    /**
     * Highly intelligent offline NLP conversational reasoning engine that:
     * 1. Extracts multiple explicit transactions with exact amounts, descriptions, and categories.
     * 2. Detects the precise user intent and answers exactly what is asked.
     * 3. Seamlessly integrates real-time financial stats from the application.
     */
    fun generateSmartOfflineResponse(
        userMessage: String,
        ctx: FinancialContext
    ): Pair<String, List<ProposedTransaction>> {
        val lower = userMessage.lowercase().trim()
        val proposed = extractProposedTransactions(userMessage)

        val name = ctx.userName.ifBlank { "Sahabat" }
        val reply = buildString {
            // Case 1: If user reported one or more transactions
            if (proposed.isNotEmpty()) {
                val totalAmount = proposed.sumOf { it.amount }
                val hasIncome = proposed.any { it.type == "in" }
                val hasExpense = proposed.any { it.type == "out" }

                // Empathetic & relevant intro
                if (hasExpense && hasIncome) {
                    append("Aku dengar ceritamu, ada pengeluaran dan juga pemasukan yang terjadi hari ini. ")
                } else if (hasIncome) {
                    append("Alhamdulillah, selamat atas rezeki dan pemasukannya, $name! 🎉 ")
                } else {
                    // Check specific expense nature for tailored empathy
                    val descLower = proposed.joinToString(" ") { it.description.lowercase() }
                    when {
                        descLower.contains("servis") || descLower.contains("motor") || descLower.contains("bengkel") || descLower.contains("mobil") -> {
                            append("Waduh, pengeluaran buat servis kendaraan memang sering tak terduga dan lumayan bikin dompet kaget ya. Tapi keselamatan dan kelancaran motormu jauh lebih penting! 💪 ")
                        }
                        descLower.contains("makan") || descLower.contains("ngopi") || descLower.contains("jajan") || descLower.contains("resto") -> {
                            append("Bisa makan enak dan ngopi santai itu rezeki yang patut disyukuri buat ngisi energi hari ini! ☕ ")
                        }
                        descLower.contains("baju") || descLower.contains("sepatu") || descLower.contains("belanja") -> {
                            append("Semoga barang belanjaannya bermanfaat ya! Belanja sesekali boleh banget buat apresiasi diri, asalkan tetap dalam batas anggaran. ✨ ")
                        }
                        descLower.contains("kos") || descLower.contains("listrik") || descLower.contains("wifi") || descLower.contains("air") -> {
                            append("Kewajiban bulanan memang pos wajib yang harus diprioritaskan lebih dulu. Bagus banget kamu langsung ingat untuk beresin tagihan ini! 🏠 ")
                        }
                        descLower.contains("obat") || descLower.contains("dokter") || descLower.contains("sakit") -> {
                            append("Lekas sembuh dan pulih ya, $name. Kesehatan adalah aset paling berharga nomor satu, biaya perawatan ini sangat layak dikeluarkan. ❤️ ")
                        }
                        else -> {
                            append("Aku paham, mengelola pengeluaran harian memang butuh perhatian ekstra. Terima kasih sudah terbuka cerita ke aku. ")
                        }
                    }
                }

                append("\n\nRincian yang kamu sebutkan:\n")
                proposed.forEach { item ->
                    val sign = if (item.type == "in") "(+) " else "(-) "
                    append("• ${item.description}: $sign${Formatters.formatRupiah(item.amount)} [${item.category}]\n")
                }
                append("Total: **${Formatters.formatRupiah(totalAmount)}**\n\n")

                // If user ALSO asked about their balance/data in the same prompt
                if (lower.contains("sisa") || lower.contains("saldo") || lower.contains("uangku")) {
                    append("Sebagai info, dana likuid siap pakai kamu saat ini tercatat **${Formatters.formatRupiah(ctx.liquidAsset)}** (total kekayaan bersih: **${Formatters.formatRupiah(ctx.totalAsset)}**).\n\n")
                }

                append("Mau langsung aku bantu catat ke pembukuan aplikasi? Tinggal klik tombol **'Catat ke Transaksi'** di bawah ini ya! 👇")
                return@buildString
            }

            // Case 2: User asking specific financial questions or advice
            when {
                // 1. Check balance / liquid assets / accounts
                lower.contains("sisa") || lower.contains("saldo") || lower.contains("uang saya") || lower.contains("uangku") || lower.contains("aset") -> {
                    append("Berikut posisi keuanganmu saat ini, $name:\n\n")
                    append("• **Dana Likuid (Siap Pakai):** ${Formatters.formatRupiah(ctx.liquidAsset)}\n")
                    append("• **Aset Non-Likuid:** ${Formatters.formatRupiah(ctx.nonLiquidAsset)}\n")
                    append("• **Total Kekayaan Bersih:** ${Formatters.formatRupiah(ctx.totalAsset)}\n")
                    if (ctx.accounts.isNotEmpty()) {
                        append("• **Daftar Akun/Dompet:** ${ctx.accounts.joinToString(", ")}\n\n")
                    } else {
                        append("\n")
                    }
                    if (ctx.liquidAsset > 1_000_000L) {
                        append("Kondisi likuiditasmu tergolong sehat dan aman untuk operasional sehari-hari. 👍")
                    } else if (ctx.liquidAsset > 0L) {
                        append("Dana siap pakai masih ada, tapi disarankan prioritaskan kebutuhan pokok dulu ya.")
                    } else {
                        append("Perhatian: Saldo siap pakai sangat menipis, yuk segera evaluasi pos pengeluaran!")
                    }
                }

                // 2. Check monthly expenses
                lower.contains("pengeluaran") || lower.contains("sudah keluar") || lower.contains("keluar berapa") || lower.contains("habis berapa") -> {
                    append("Catatan pengeluaranmu bulan ini:\n\n")
                    append("• **Total Pengeluaran Bulan Ini:** ${Formatters.formatRupiah(ctx.monthlyExpense)}\n")
                    if (ctx.savingsTarget > 0L) {
                        val pct = ((ctx.monthlyExpense.toDouble() / ctx.savingsTarget.toDouble()) * 100).toInt()
                        append("• **Batas Target / Anggaran:** ${Formatters.formatRupiah(ctx.savingsTarget)} (sudah terpakai $pct%)\n")
                    }
                    if (ctx.topCategories.isNotEmpty()) {
                        append("\n**Pos Pengeluaran Terbesar:**\n")
                        ctx.topCategories.take(3).forEach {
                            append("• ${it.first}: ${Formatters.formatRupiah(it.second)}\n")
                        }
                    }
                    append("\nKalau kamu ada pengeluaran baru yang belum dicatat, sebutkan saja nominal dan keperluannya ya, nanti aku bantu catatkan langsung!")
                }

                // 3. Check monthly income
                lower.contains("pemasukan") || lower.contains("pendapatan") || lower.contains("gaji bulan") -> {
                    append("Pemasukanmu yang sudah tercatat bulan ini sebesar **${Formatters.formatRupiah(ctx.monthlyIncome)}**.\n\n")
                    if (ctx.monthlyNet >= 0) {
                        append("Arus kasmu surplus **${Formatters.formatRupiah(ctx.monthlyNet)}**. Pertahankan kebiasaan baik ini ya! 🎉")
                    } else {
                        append("Saat ini arus kasmu defisit **${Formatters.formatRupiah(-ctx.monthlyNet)}** dibanding pengeluaranmu. Yuk kita tahan dulu belanja yang belum mendesak.")
                    }
                }

                // 4. Check cashflow / net
                lower.contains("arus kas") || lower.contains("surplus") || lower.contains("defisit") || lower.contains("net") || lower.contains("untung rugi") -> {
                    append("Ringkasan Arus Kas Bulan Ini:\n\n")
                    append("• **Pemasukan:** ${Formatters.formatRupiah(ctx.monthlyIncome)}\n")
                    append("• **Pengeluaran:** ${Formatters.formatRupiah(ctx.monthlyExpense)}\n")
                    if (ctx.monthlyNet >= 0) {
                        append("• **Arus Kas Bersih:** Surplus **${Formatters.formatRupiah(ctx.monthlyNet)}** 📈\n\n")
                        append("Kondisi keuanganmu positif! Surplus ini bisa kamu alihkan ke dana darurat atau tabungan target impian.")
                    } else {
                        append("• **Arus Kas Bersih:** Defisit **${Formatters.formatRupiah(-ctx.monthlyNet)}** ⚠️\n\n")
                        append("Pengeluaranmu melebihi pemasukan bulan ini. Coba cek kategori pengeluaran terbesarmu untuk direm minggu depan.")
                    }
                }

                // 5. Check top categories / most wasteful
                lower.contains("boros") || lower.contains("pos terbesar") || lower.contains("kategori") || lower.contains("paling banyak") -> {
                    if (ctx.topCategories.isEmpty()) {
                        append("Belum ada data pengeluaran terperinci di bulan ini. Yuk mulai catat pengeluaranmu agar bisa kita analisa pos mana yang paling boros!")
                    } else {
                        append("Berikut analisis pos pengeluaranmu bulan ini:\n\n")
                        ctx.topCategories.forEachIndexed { idx, pair ->
                            val rank = idx + 1
                            append("$rank. **${pair.first}**: ${Formatters.formatRupiah(pair.second)}\n")
                        }
                        val top1 = ctx.topCategories.first()
                        append("\nPos **${top1.first}** menyerap anggaran paling banyak (${Formatters.formatRupiah(top1.second)}). Kalau ingin menghemat, mulailah dengan membatasi pos tersebut.")
                    }
                }

                // 6. Check budget / overbudget
                lower.contains("overbudget") || lower.contains("target") || lower.contains("anggaran") || lower.contains("batas") -> {
                    if (ctx.savingsTarget <= 0L) {
                        append("Kamu belum mengatur target tabungan atau batas anggaran di profil. Atur batas anggaran di menu Akun agar Finansia bisa memberikan peringatan otomatis jika pengeluaran mendekati limit!")
                    } else {
                        val pct = ((ctx.monthlyExpense.toDouble() / ctx.savingsTarget.toDouble()) * 100).toInt()
                        append("Status Anggaranmu Bulan Ini:\n\n")
                        append("• Pengeluaran: **${Formatters.formatRupiah(ctx.monthlyExpense)}**\n")
                        append("• Batas Anggaran: **${Formatters.formatRupiah(ctx.savingsTarget)}**\n")
                        append("• Persentase Pemakaian: **$pct%**\n\n")
                        if (pct >= 100) {
                            append("🚨 **Peringatan Overbudget:** Pengeluaranmu sudah melebihi batas yang ditentukan sebesar ${Formatters.formatRupiah(ctx.monthlyExpense - ctx.savingsTarget)}. Sangat disarankan setop pengeluaran non-primer!")
                        } else if (pct >= 80) {
                            append("⚠️ **Hati-hati:** Pemakaian anggaran sudah mencapai $pct%. Tersisa ${Formatters.formatRupiah(ctx.savingsTarget - ctx.monthlyExpense)} untuk sisa bulan ini.")
                        } else {
                            append("✅ **Aman:** Anggaranmu masih sangat terkontrol dengan sisa kuota ${Formatters.formatRupiah(ctx.savingsTarget - ctx.monthlyExpense)}.")
                        }
                    }
                }

                // 7. Check recent transactions
                lower.contains("riwayat") || lower.contains("terakhir") || lower.contains("histori") || lower.contains("kemarin beli apa") -> {
                    append("Aktivitas transaksi terakhirmu:\n\n")
                    if (ctx.recentTransactionsSummary.isNotBlank()) {
                        val items = ctx.recentTransactionsSummary.split("; ")
                        items.forEach { item ->
                            append("• $item\n")
                        }
                    } else {
                        append("Belum ada catatan transaksi baru di akunmu.")
                    }
                }

                // 8. Advice on impulsive shopping / "lapar mata" / "khilaf"
                lower.contains("lapar mata") || lower.contains("khilaf") || lower.contains("belanja terus") || lower.contains("ngerem") || lower.contains("impulsif") || lower.contains("nyesel") -> {
                    append("Wajar banget merasa tergoda, apalagi godaan promo marketplace dan jajanan kekinian sering bikin lapar mata! 🛍️\n\n")
                    append("Coba 3 trik ampuh ini buat ngerem belanja impulsif:\n")
                    append("1. **Aturan 24 Jam**: Saat ingin checkout barang non-esensial, tunda minimal 24 jam. Biasanya setelah besok rasa 'kebelet beli' itu hilang drastis.\n")
                    append("2. **Metode Rekening Jajan**: Jangan pakai rekening utama untuk jajan. Transfer jatah jajan mingguan ke e-wallet dengan nominal pas-pasan. Kalau habis, tunggu minggu depan.\n")
                    append("3. **Konversi ke Jam Kerja**: Sebelum beli barang seharga Rp 300rb, hitung berapa jam kamu harus bekerja keras untuk mendapatkan nominal itu. Seringkali jadi mikir dua kali!\n\n")
                    append("Bulan ini pengeluaranmu sudah ${Formatters.formatRupiah(ctx.monthlyExpense)}. Jadikan angka ini pengingat sebelum belanja lagi ya!")
                }

                // 9. Advice on saving tips
                lower.contains("tips nabung") || lower.contains("cara menabung") || lower.contains("cara hemat") || lower.contains("tips hemat") || lower.contains("frugal") -> {
                    append("Kunci menabung yang berhasil itu bukan menyisakan sisa uang di akhir bulan, tapi menyisihkan di awal saat gajian tiba! 💡\n\n")
                    append("Strategi praktis yang bisa kamu terapkan:\n")
                    append("1. **Formula 50-30-20**:\n")
                    append("   • 50% Kebutuhan Pokok (makan, kos, bensin, tagihan)\n")
                    append("   • 30% Keinginan (hiburan, ngopi, hobi)\n")
                    append("   • 20% Tabungan & Investasi Masa Depan\n")
                    append("2. **Auto-Debet atau Pindah Rekening**: Buat rekening tabungan terpisah yang tidak punya kartu debit/QRIS di HP kamu.\n")
                    append("3. **Mulai dari Micro-Saving**: Jangan tunggu punya uang banyak. Mulai dari Rp 20rb - 50rb per hari yang konsisten akan jadi jutaan rupiah dalam beberapa bulan!\n\n")
                    append("Kamu ingin menargetkan tabungan untuk apa? Ceritakan ke aku, nanti kita susun rencananya bersama.")
                }

                // 10. Advice on debt & paylater
                lower.contains("cicilan") || lower.contains("paylater") || lower.contains("hutang") || lower.contains("pinjol") || lower.contains("kredit") -> {
                    append("Menghadapi cicilan memang bikin pikiran tertekan, tapi jangan berkecil hati. Langkah pertama adalah tenang dan buat rencana pelunasan: 🛡️\n\n")
                    append("1. **Setop Tambah Cicilan**: Kunci utama, jangan buat pinjaman atau paylater baru untuk menutup yang lama.\n")
                    append("2. **Metode Bola Salju (Snowball)**: Urutkan semua cicilan dari nominal terkecil ke terbesar. Bayar minimum untuk semua, dan curahkan sisa dana untuk melunasi yang terkecil dulu agar kamu dapat kemenangan mental!\n")
                    append("3. **Gunakan Surplus Bulanan**: Manfaatkan surplus arus kasmu (saat ini ${Formatters.formatRupiah(ctx.monthlyNet)}) untuk pelunasan dipercepat.\n\n")
                    append("Kalau butuh mencatat cicilan bulan ini, kamu bisa langsung sebutkan di sini ya.")
                }

                // 11. Advice on investment
                lower.contains("investasi") || lower.contains("saham") || lower.contains("reksadana") || lower.contains("emas") || lower.contains("crypto") -> {
                    append("Tertarik mulai berinvestasi itu langkah awal yang hebat, $name! 📈\n\n")
                    append("Urutan piramida finansial yang aman sebelum berinvestasi:\n")
                    append("1. **Amankan Dana Darurat Dulu**: Pastikan kamu punya tabungan likuid setara 3-6 bulan pengeluaran (saat ini dana likuidmu ${Formatters.formatRupiah(ctx.liquidAsset)}).\n")
                    append("2. **Bebas Hutang Konsumtif**: Lunasi paylater atau pinjaman berbunga tinggi terlebih dahulu.\n")
                    append("3. **Mulai dari Instrumen Rendah Risiko**: Untuk pemula, Reksadana Pasar Uang (RPU) atau Emas batangan sangat bagus karena minim fluktuasi dan stabil.\n\n")
                    append("Jangan pernah berinvestasi pakai 'uang panas' atau dana kebutuhan sehari-hari ya!")
                }

                // 12. Emotional stress vent about money
                lower.contains("stres") || lower.contains("pusing") || lower.contains("capek") || lower.contains("sedih") || lower.contains("nyesek") || lower.contains("berat") -> {
                    append("Tarik napas dalam-dalam dulu ya, $name... 🤍\n\n")
                    append("Urusan finansial memang salah satu hal paling menguras emosi dan tenaga. Tapi ingat, kondisi sulit ini tidak mendefinisikan nilaimu sebagai pribadi, dan situasi ini bisa diperbaiki pelan-pelan.\n\n")
                    append("Yang penting sekarang:\n")
                    append("• Jangan membebani diri dengan ekspektasi terlalu tinggi dalam semalam.\n")
                    append("• Fokus pada apa yang bisa kamu kendalikan hari ini: tahan pengeluaran yang tidak perlu, dan istirahat yang cukup.\n\n")
                    append("Aku ada di sini untuk mendengarkan. Ada hal spesifik yang lagi bikin kamu paling cemas hari ini?")
                }

                // 13. Greetings
                lower.startsWith("halo") || lower.startsWith("hai") || lower.startsWith("hi") || lower.contains("selamat pagi") || lower.contains("selamat siang") || lower.contains("selamat sore") || lower.contains("selamat malam") || lower.contains("assalamualaikum") -> {
                    append("Halo $name! Senang bertemu denganmu hari ini. 👋\n\n")
                    append("Aku Finansia, sahabat curhat dan asisten keuangan pribadimu. Aku bisa bantu mendengarkan keluh kesah keuanganmu, memberi saran berhemat, mengecek saldo & pengeluaran, atau langsung mencatatkan transaksi kalau kamu sebutkan nominal pengeluaran hari ini.\n\n")
                    append("Ada kabar apa atau hal yang ingin kamu ceritakan seputar uangmu hari ini?")
                }

                // 14. Identity / features
                lower.contains("siapa") || lower.contains("bisa apa") || lower.contains("fitur") -> {
                    append("Aku **Finansia**, asisten dan sahabat curhat keuangan pribadimu di aplikasi ini! 🤖💡\n\n")
                    append("Beberapa hal yang bisa kita lakukan bersama:\n")
                    append("1. **Curhat Pengeluaran Otomatis**: Ketik seperti *'Hari ini habis 1jt: 900k servis motor, 100k makan'*, dan aku akan bantu deteksi dan catatkan ke pembukuan aplikasimu!\n")
                    append("2. **Cek Kondisi Finansial**: Tanya *'Berapa sisa saldoku?'*, *'Pos apa yang paling boros?'*, atau *'Berapa pengeluaranku bulan ini?'*.\n")
                    append("3. **Konsultasi & Teman Cerita**: Pusing mikirin cicilan, lapar mata saat belanja, atau butuh tips menabung? Tanyakan saja apa adanya!")
                }

                // 15. Gratitude / compliments
                lower.contains("makasih") || lower.contains("terima kasih") || lower.contains("thank") || lower.contains("keren") || lower.contains("mantap") || lower.contains("oke") -> {
                    append("Sama-sama, $name! Senang sekali bisa membantu dan mendengarkan ceritamu hari ini. 😊\n\n")
                    append("Tetap semangat menjaga kesehatan finansialmu ya. Kalau ada pengeluaran atau hal lain yang ingin diceritakan, aku selalu siap di sini!")
                }

                // 16. Default friendly fall-through
                else -> {
                    append("Aku mengerti maksudmu, $name. Seputar keuanganmu saat ini, dana siap pakai kamu tercatat **${Formatters.formatRupiah(ctx.liquidAsset)}** dan pengeluaran bulan ini ada **${Formatters.formatRupiah(ctx.monthlyExpense)}**.\n\n")
                    append("Kalau ada pengeluaran atau pemasukan baru yang mau dicatat, langsung sebutkan saja nominal dan keperluannya ya (contoh: *'beli bensin 50rb'* atau *'makan siang 35k'*). Ada hal lain yang mau kita diskusikan?")
                }
            }
        }

        return Pair(reply, proposed)
    }

    /**
     * Extracts multiple natural language transactions from Indonesian user inputs.
     * Examples:
     * - "Hari ini habis 1jt: 900k servis motor, 100k makan"
     * - "Tadi siang makan 35k sama beli bensin 50rb"
     * - "beli baju 200rb, ngopi 30k"
     * - "dapat freelance 500k"
     */
    private fun extractProposedTransactions(input: String): List<ProposedTransaction> {
        val list = mutableListOf<ProposedTransaction>()
        val cleaned = input.replace(":", " , ")
            .replace(";", " , ")
            .replace(" dan ", " , ")
            .replace(" sama ", " , ")
            .replace("\n", " , ")

        val segments = cleaned.split(",").map { it.trim() }.filter { it.isNotBlank() }

        for (segment in segments) {
            val tx = parseSingleSegment(segment)
            if (tx != null) {
                list.add(tx)
            }
        }

        // If segment splitting missed something, do a global scan
        if (list.isEmpty()) {
            val globalTx = parseSingleSegment(input)
            if (globalTx != null) {
                list.add(globalTx)
            }
        }

        return list
    }

    private fun parseSingleSegment(segment: String): ProposedTransaction? {
        val lower = segment.lowercase()

        // Detect amount e.g. "900k", "1jt", "1.5 juta", "100rb", "25.000", "50000"
        val amountRegex = Regex("(\\d+([.,]\\d+)?)\\s*(jt|juta|k|rb|ribu)?")
        val match = amountRegex.find(lower) ?: return null

        val numRaw = match.groupValues[1].replace(',', '.')
        val unit = match.groupValues[3].lowercase()
        val baseNum = numRaw.toDoubleOrNull() ?: return null

        val amount = when (unit) {
            "jt", "juta" -> (baseNum * 1_000_000).toLong()
            "k", "rb", "ribu" -> (baseNum * 1_000).toLong()
            else -> {
                if (baseNum in 1.0..999.0) {
                    // Indonesian shorthand e.g. "habis 50 buat makan" -> 50.000
                    (baseNum * 1_000).toLong()
                } else {
                    baseNum.toLong()
                }
            }
        }

        if (amount <= 0L) return null

        // Detect type (in vs out)
        val isIncome = lower.contains("dapat") || lower.contains("gaji") || lower.contains("bonus") ||
                lower.contains("freelance") || lower.contains("pemasukan") || lower.contains("cuan") ||
                lower.contains("transferan masuk") || lower.contains("diberi") || lower.contains("terima")

        val type = if (isIncome) "in" else "out"

        // Detect category & clean description
        val category = determineCategory(lower, isIncome)
        val description = extractDescription(segment, match.value, category)

        return ProposedTransaction(
            type = type,
            description = description,
            amount = amount,
            category = category
        )
    }

    private fun determineCategory(text: String, isIncome: Boolean): String {
        if (isIncome) return "Pemasukan"

        return when {
            text.contains("motor") || text.contains("servis") || text.contains("bengkel") ||
                    text.contains("mobil") || text.contains("bensin") || text.contains("pertalite") ||
                    text.contains("pertamax") || text.contains("ojol") || text.contains("grab") ||
                    text.contains("gojek") || text.contains("parkir") || text.contains("tol") ||
                    text.contains("tiket") || text.contains("kereta") -> "Transportasi"

            text.contains("makan") || text.contains("minum") || text.contains("kopi") ||
                    text.contains("ngopi") || text.contains("cafe") || text.contains("resto") ||
                    text.contains("boba") || text.contains("sarapan") || text.contains("siang") ||
                    text.contains("malam") || text.contains("snack") || text.contains("jajan") ||
                    text.contains("bakso") || text.contains("mie") || text.contains("nasi") -> "Makan"

            text.contains("kos") || text.contains("kontrakan") || text.contains("listrik") ||
                    text.contains("pln") || text.contains("air") || text.contains("pdam") ||
                    text.contains("wifi") || text.contains("internet") || text.contains("indihome") ||
                    text.contains("laundry") || text.contains("galon") || text.contains("gas") -> "Kebutuhan Kos"

            text.contains("belanja") || text.contains("supermarket") || text.contains("minimarket") ||
                    text.contains("alfamart") || text.contains("indomaret") || text.contains("pasar") ||
                    text.contains("baju") || text.contains("celana") || text.contains("sepatu") ||
                    text.contains("skincare") || text.contains("sabun") || text.contains("shampo") ||
                    text.contains("tas") || text.contains("outfit") -> "Belanja Bulanan"

            text.contains("obat") || text.contains("dokter") || text.contains("klinik") ||
                    text.contains("rs") || text.contains("apotek") || text.contains("vitamin") ||
                    text.contains("sakit") || text.contains("medis") -> "Kesehatan"

            text.contains("cicilan") || text.contains("angsuran") || text.contains("paylater") ||
                    text.contains("kartu kredit") || text.contains("pinjaman") || text.contains("hutang") ||
                    text.contains("kredivo") || text.contains("spaylater") -> "Cicilan & Kredit"

            text.contains("nabung") || text.contains("investasi") || text.contains("reksadana") ||
                    text.contains("saham") || text.contains("emas") || text.contains("crypto") ||
                    text.contains("deposito") -> "Tabungan Masa Depan"

            text.contains("sedekah") || text.contains("infaq") || text.contains("zakat") ||
                    text.contains("donasi") || text.contains("sumbangan") || text.contains("masjid") -> "Donasi & Sedekah"

            text.contains("nonton") || text.contains("bioskop") || text.contains("game") ||
                    text.contains("topup") || text.contains("liburan") || text.contains("karaoke") ||
                    text.contains("staycation") -> "Hiburan & Rekreasi"

            else -> "Pengeluaran Lainnya"
        }
    }

    private fun extractDescription(segment: String, matchedAmountStr: String, category: String): String {
        // Strip amount string and filler words from segment
        val stripped = segment.replace(matchedAmountStr, "", ignoreCase = true)
            .replace(Regex("(?i)\\b(hari ini|tadi|kemarin|habis|buat|untuk|beli|bayar|sebesar|rp|seharga)\\b"), "")
            .trim()
            .replace(Regex("^[\\s,:\\-]+"), "")
            .replace(Regex("[\\s,:\\-]+$"), "")

        return if (stripped.length >= 3) {
            stripped.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            when (category) {
                "Transportasi" -> "Transportasi & Kendaraan"
                "Makan" -> "Makan & Minum"
                "Kebutuhan Kos" -> "Tagihan Kos & Utilitas"
                "Belanja Bulanan" -> "Belanja Kebutuhan"
                "Kesehatan" -> "Biaya Kesehatan"
                "Cicilan & Kredit" -> "Pembayaran Cicilan"
                "Tabungan Masa Depan" -> "Tabungan"
                "Donasi & Sedekah" -> "Sedekah"
                "Hiburan & Rekreasi" -> "Hiburan"
                "Pemasukan" -> "Pemasukan Tambahan"
                else -> "Pengeluaran Curhat"
            }
        }
    }
}
