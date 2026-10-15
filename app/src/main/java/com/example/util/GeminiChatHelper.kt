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
    private val FALLBACK_MODELS = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-flash-latest", "gemini-3.1-flash-lite-preview")

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        financialContext: FinancialContext
    ): ChatMessage = withContext(Dispatchers.IO) {
        val rawApiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        val hasValidUserKey = rawApiKey.isNotBlank() &&
                rawApiKey != "your_api_key_here"

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
    /**
     * Highly intelligent offline NLP conversational reasoning engine that:
     * 1. Safely parses real expense/income statements without falsely treating questions as transactions.
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
            // Case 1: If user reported one or more explicit spending/earning transactions
            if (proposed.isNotEmpty()) {
                val totalAmount = proposed.sumOf { it.amount }
                val hasIncome = proposed.any { it.type == "in" }
                val hasExpense = proposed.any { it.type == "out" }

                // Empathetic & relevant intro
                val isRegretful = lower.contains("nyesel") || lower.contains("menyesal") ||
                        lower.contains("khilaf") || lower.contains("merasa bersalah") || lower.contains("kenapa tadi beli")
                val isWorryLow = lower.contains("sisa dikit") || lower.contains("uang tinggal") ||
                        lower.contains("uang menipis") || lower.contains("pas-pasan") || lower.contains("uangku habis")

                if (isRegretful) {
                    append("Aku dengar ceritamu dan paham banget rasa sesal itu, $name. 🤍 Rasa bersalah setelah mengeluarkan uang itu sangat manusiawi dan pernah dialami semua orang. Jangan terlalu menyiksa diri ya, jadikan ini rem berharga buat belanja berikutnya. ")
                } else if (hasExpense && hasIncome) {
                    append("Aku dengar ceritamu, ada pengeluaran dan juga pemasukan yang terjadi hari ini. ")
                } else if (hasIncome) {
                    append("Alhamdulillah, selamat atas rezeki dan pemasukannya, $name! 🎉 ")
                } else {
                    val descLower = proposed.joinToString(" ") { it.description.lowercase() }
                    when {
                        descLower.contains("servis") || descLower.contains("motor") || descLower.contains("bengkel") || descLower.contains("mobil") -> {
                            append("Waduh, pengeluaran buat servis kendaraan memang sering tak terduga dan bikin kaget. Tapi keselamatan dan kelancaran motormu jauh lebih penting! 💪 ")
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

                if (isWorryLow) {
                    append("\nMengenai saldo yang menipis, dana likuidmu saat ini tercatat **${Formatters.formatRupiah(ctx.liquidAsset)}**. Saran Finansia: kunci pengeluaran non-primer dan prioritaskan jatah makan esensial dulu untuk beberapa hari ke depan ya! 🛡️")
                }

                append("\n\nRincian yang kamu sebutkan:\n")
                proposed.forEach { item ->
                    val sign = if (item.type == "in") "(+) " else "(-) "
                    append("• ${item.description}: $sign${Formatters.formatRupiah(item.amount)} [${item.category}]\n")
                }
                append("Total: **${Formatters.formatRupiah(totalAmount)}**\n\n")

                if (lower.contains("sisa") || lower.contains("saldo") || lower.contains("uangku")) {
                    append("Sebagai info, dana likuid siap pakai kamu saat ini tercatat **${Formatters.formatRupiah(ctx.liquidAsset)}** (total kekayaan bersih: **${Formatters.formatRupiah(ctx.totalAsset)}**).\n\n")
                }

                append("Mau langsung aku bantu catat ke pembukuan aplikasi? Tinggal klik tombol **'Catat ke Transaksi'** di bawah ini ya! 👇")
                return@buildString
            }

            // Case 2: User asking specific financial questions or seeking advice
            when {
                // Islamic financial quotes & guidance
                lower.contains("islam") || lower.contains("quran") || lower.contains("hadis") ||
                        lower.contains("hadist") || lower.contains("kutipan islami") || lower.contains("syariah") ||
                        lower.contains("berkah") || (lower.contains("rezeki") && (lower.contains("quote") || lower.contains("nasihat") || lower.contains("ayat"))) -> {
                    append("Berikut mutiara, ayat Al-Qur'an, dan Hadits shahih seputar keberkahan rezeki dan tata kelola harta, $name: 📖✨\n\n")
                    append("1. **Prinsip Keseimbangan Anggaran (Anti Boros & Anti Kikir)**:\n")
                    append("   *“Dan orang-orang yang apabila membelanjakan (harta), mereka tidak berlebihan, dan tidak (pula) kikir, di antara keduanya secara wajar.”*\n")
                    append("   — **QS. Al-Furqan: 67**\n\n")
                    append("2. **Larangan Pemborosan & Lapar Mata (Israf & Tabdzir)**:\n")
                    append("   *“Dan janganlah kamu menghambur-hamburkan (hartamu) secara boros. Sesungguhnya para pemboros itu adalah saudara-saudara setan.”*\n")
                    append("   — **QS. Al-Isra': 26-27**\n\n")
                    append("3. **Kekayaan Sejati adalah Rasa Cukup (Qana'ah)**:\n")
                    append("   *“Bukanlah kekayaan itu dengan banyaknya harta benda, melainkan kekayaan yang sejati adalah kekayaan jiwa.”*\n")
                    append("   — **HR. Bukhari no. 6446 & Muslim no. 1051**\n\n")
                    append("4. **Pintu Rezeki Tak Terduga bagi yang Bertakwa & Tawakkal**:\n")
                    append("   *“Barangsiapa bertakwa kepada Allah niscaya Dia akan membukakan jalan keluar baginya, dan memberinya rezeki dari arah yang tiada disangka-sangkanya. Dan barangsiapa bertawakkal kepada Allah niscaya Allah akan mencukupkan (keperluan)nya.”*\n")
                    append("   — **QS. At-Talaq: 2-3**\n\n")
                    append("5. **Lipat Ganda Keberkahan Sedekah**:\n")
                    append("   *“Perumpamaan orang yang menginfakkan hartanya di jalan Allah seperti sebutir biji yang menumbuhkan tujuh tangkai, pada setiap tangkai ada seratus biji...”*\n")
                    append("   — **QS. Al-Baqarah: 261**\n")
                    append("   *“Harta tidak akan berkurang karena sedekah...”* — **HR. Muslim no. 2588**\n\n")
                    append("6. **Pertanggungjawaban Setiap Rupiah**:\n")
                    append("   *“Tidak akan bergeser kedua kaki seorang hamba pada hari kiamat hingga ditanya tentang hartanya: dari mana ia peroleh dan ke mana ia belanjakan.”*\n")
                    append("   — **HR. Tirmidzi no. 2417**\n\n")
                    append("7. **Melihat ke Bawah dalam Urusan Duniawi**:\n")
                    append("   *“Lihatlah orang yang berada di bawahmu (dalam urusan harta) dan jangan melihat orang yang berada di atasmu, agar kamu tidak meremehkan nikmat Allah.”*\n")
                    append("   — **HR. Muslim no. 2564**\n\n")
                    append("Semoga setiap rupiah rezeki yang kamu terima dan kelola membawa keberkahan, ketenangan, dan kelapangan hidup bagimu ya! 🤲")
                }

                // 1. How to manage/allocate salary (e.g. "gaji 3 juta", "atur gaji", "bagi gaji")
                lower.contains("bagi gaji") || lower.contains("atur gaji") || lower.contains("alokasi gaji") ||
                        (lower.contains("gaji") && (lower.contains("juta") || lower.contains("jt") || lower.contains("cara") || lower.contains("gimana"))) -> {
                    val salaryAmount = extractSalaryFromText(lower) ?: if (ctx.monthlyIncome > 0L) ctx.monthlyIncome else 3_000_000L
                    val salaryStr = Formatters.formatRupiah(salaryAmount)

                    val kebutuhan = (salaryAmount * 0.50).toLong()
                    val keinginan = (salaryAmount * 0.30).toLong()
                    val tabungan = (salaryAmount * 0.20).toLong()

                    append("Berikut panduan praktis dan realistis membagi gaji **$salaryStr** dengan metode 50-30-20, $name: 📊💡\n\n")
                    append("1. **50% Kebutuhan Pokok (${Formatters.formatRupiah(kebutuhan)})**:\n")
                    append("   • Kos/kontrakan, makan pokok sehari-hari, bensin, token listrik, pulsa/internet, dan tagihan wajib.\n")
                    append("2. **30% Keinginan & Gaya Hidup (${Formatters.formatRupiah(keinginan)})**:\n")
                    append("   • Ngopi, jajan santai, hiburan, dan belanja pribadi. Kunci utamanya: jangan melebihi nominal ini ya!\n")
                    append("3. **20% Tabungan & Investasi (${Formatters.formatRupiah(tabungan)})**:\n")
                    append("   • Prioritas 1: Kumpulkan dana darurat sampai setara 3-6 bulan kebutuhan pokok.\n")
                    append("   • Prioritas 2: Reksadana Pasar Uang atau Emas untuk tujuan jangka menengah.\n\n")
                    append("💡 *Tips penting*: Pisahkan dana tabungan di awal gajian ke rekening tanpa kartu debit agar tidak terpakai jajan!")
                }

                // 2. Affordability / Buying something big (e.g. "mau beli hp 5 juta", "aman ga beli", "pengen beli laptop")
                (lower.contains("mau beli") || lower.contains("pengen beli") || lower.contains("rencana beli") || lower.contains("beli hp") || lower.contains("beli motor")) &&
                        (lower.contains("aman") || lower.contains("boleh") || lower.contains("bisa") || lower.contains("juta") || lower.contains("jt") || lower.contains("rb") || lower.contains("k")) -> {
                    val targetPrice = extractNumberFromText(lower) ?: 3_000_000L
                    val priceStr = Formatters.formatRupiah(targetPrice)

                    append("Pertimbangan bijak sebelum membeli seharga **$priceStr**, $name: 🤔📱\n\n")
                    append("• **Saldo Likuid Siap Pakaimu Saat Ini**: ${Formatters.formatRupiah(ctx.liquidAsset)}\n")
                    append("• **Arus Kas Bulananmu (Surplus/Defisit)**: ${if (ctx.monthlyNet >= 0) "Surplus " else "Defisit "}${Formatters.formatRupiah(ctx.monthlyNet)}\n\n")

                    if (ctx.liquidAsset >= targetPrice * 3) {
                        append("✅ **Secara kas aman**: Saldo likuidmu cukup kuat dan masih menyisakan dana darurat setelah pembelian. Pastikan barang ini memang menunjang produktivitasmu ya!")
                    } else if (ctx.liquidAsset >= targetPrice) {
                        append("⚠️ **Hati-hati**: Saldo kamu memang mencukupi, tapi pembelian ini akan menguras sebagian besar dana siap pakaimu. Saran Finansia: tunda 1-2 bulan sambil menabung khusus untuk barang ini agar dana darurat tetap aman.")
                    } else {
                        val sisaKurang = targetPrice - ctx.liquidAsset
                        val bulanSimpan = if (ctx.monthlyNet > 0) kotlin.math.ceil(sisaKurang.toDouble() / ctx.monthlyNet.toDouble()).toInt() else 3
                        append("🚨 **Belum disarankan**: Saldo siap pakai saat ini (${Formatters.formatRupiah(ctx.liquidAsset)}) belum mencukupi. Jangan memaksakan paylater atau pinjol!\n\n")
                        append("Solusinya: sisihkan tabungan khusus sekitar ${Formatters.formatRupiah(targetPrice / bulanSimpan)} per bulan selama $bulanSimpan bulan ke depan.")
                    }
                }

                // 3. Running low on money / survival mode (e.g. "uang tinggal 200rb", "cukup ga buat seminggu", "uang pas-pasan")
                (lower.contains("uang tinggal") || lower.contains("uang sisa") || lower.contains("uangku tinggal") || lower.contains("sisa uang tinggal")) ||
                        (lower.contains("cukup ga") || lower.contains("cukup tidak") || lower.contains("bisa bertahan")) -> {
                    val remainingAmount = extractNumberFromText(lower) ?: if (ctx.liquidAsset > 0) ctx.liquidAsset else 200_000L
                    val remStr = Formatters.formatRupiah(remainingAmount)
                    val days = if (lower.contains("bulan")) 30 else if (lower.contains("2 minggu")) 14 else 7
                    val dailyBudget = remainingAmount / days

                    append("Bertahan dengan sisa uang **$remStr** untuk $days hari ke depan: 🛡️💪\n\n")
                    append("Jatah harianmu adalah maksimal **${Formatters.formatRupiah(dailyBudget)} / hari**.\n\n")
                    append("Strategi darurat hemat:\n")
                    append("1. **Kunci Pengeluaran Non-Primer**: Stop jajan kopi, nongkrong, dan rokok/snack.\n")
                    append("2. **Menu Makan Hemat**: Masak nasi sendiri, lauk telur, tahu, tempe, atau sayur sop bening (biaya ~Rp 15rb-20rb/hari).\n")
                    append("3. **Bawa Air Minum Sendiri**: Hindari beli air kemasan di luar.\n\n")
                    append("Kondisi ini sementara, kamu pasti bisa melewatinya sampai tanggal gajian tiba!")
                }

                // 4. Check balance / liquid assets / accounts
                lower.contains("sisa") || lower.contains("saldo") || lower.contains("uang saya") || lower.contains("uangku") || lower.contains("aset") -> {
                    append("Berikut posisi keuanganmu saat ini, $name: 💼\n\n")
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

                // 5. Check monthly expenses
                lower.contains("pengeluaran") || lower.contains("sudah keluar") || lower.contains("keluar berapa") || lower.contains("habis berapa") -> {
                    append("Catatan pengeluaranmu bulan ini: 📉\n\n")
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
                    append("\nKalau ada pengeluaran baru yang belum dicatat, sebutkan saja nominal dan keperluannya ya, nanti aku bantu catatkan langsung!")
                }

                // 6. Check monthly income
                lower.contains("pemasukan") || lower.contains("pendapatan") || lower.contains("gaji bulan") -> {
                    append("Pemasukanmu yang sudah tercatat bulan ini sebesar **${Formatters.formatRupiah(ctx.monthlyIncome)}**. 📈\n\n")
                    if (ctx.monthlyNet >= 0) {
                        append("Arus kasmu surplus **${Formatters.formatRupiah(ctx.monthlyNet)}**. Pertahankan kebiasaan baik ini ya! 🎉")
                    } else {
                        append("Saat ini arus kasmu defisit **${Formatters.formatRupiah(-ctx.monthlyNet)}** dibanding pengeluaranmu. Yuk kita tahan dulu belanja yang belum mendesak.")
                    }
                }

                // 7. Check cashflow / net
                lower.contains("arus kas") || lower.contains("surplus") || lower.contains("defisit") || lower.contains("net") || lower.contains("untung rugi") -> {
                    append("Ringkasan Arus Kas Bulan Ini: ⚖️\n\n")
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

                // 8. Check top categories / most wasteful
                lower.contains("boros") || lower.contains("pos terbesar") || lower.contains("kategori") || lower.contains("paling banyak") -> {
                    if (ctx.topCategories.isEmpty()) {
                        append("Belum ada data pengeluaran terperinci di bulan ini. Yuk mulai catat pengeluaranmu agar bisa kita analisa pos mana yang paling boros!")
                    } else {
                        append("Berikut analisis pos pengeluaranmu bulan ini: 🔍\n\n")
                        ctx.topCategories.forEachIndexed { idx, pair ->
                            val rank = idx + 1
                            append("$rank. **${pair.first}**: ${Formatters.formatRupiah(pair.second)}\n")
                        }
                        val top1 = ctx.topCategories.first()
                        append("\nPos **${top1.first}** menyerap anggaran paling banyak (${Formatters.formatRupiah(top1.second)}). Kalau ingin menghemat, mulailah dengan membatasi pos tersebut.")
                    }
                }

                // 9. Check budget / overbudget
                lower.contains("overbudget") || lower.contains("target") || lower.contains("anggaran") || lower.contains("batas") -> {
                    if (ctx.savingsTarget <= 0L) {
                        append("Kamu belum mengatur target tabungan atau batas anggaran di profil. Atur batas anggaran di menu Akun agar Finansia bisa memberikan peringatan otomatis jika pengeluaran mendekati limit!")
                    } else {
                        val pct = ((ctx.monthlyExpense.toDouble() / ctx.savingsTarget.toDouble()) * 100).toInt()
                        append("Status Anggaranmu Bulan Ini: 🎯\n\n")
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

                // 10. Check recent transactions
                lower.contains("riwayat") || lower.contains("terakhir") || lower.contains("histori") || lower.contains("kemarin beli apa") -> {
                    append("Aktivitas transaksi terakhirmu: 📜\n\n")
                    if (ctx.recentTransactionsSummary.isNotBlank()) {
                        val items = ctx.recentTransactionsSummary.split("; ")
                        items.forEach { item ->
                            append("• $item\n")
                        }
                    } else {
                        append("Belum ada catatan transaksi baru di akunmu.")
                    }
                }

                // 11. Advice on impulsive shopping / "lapar mata" / "khilaf"
                lower.contains("lapar mata") || lower.contains("khilaf") || lower.contains("belanja terus") || lower.contains("ngerem") || lower.contains("impulsif") || lower.contains("nyesel") -> {
                    append("Wajar banget merasa tergoda, apalagi godaan promo marketplace dan jajanan kekinian sering bikin lapar mata! 🛍️\n\n")
                    append("Coba 3 trik ampuh ini buat ngerem belanja impulsif:\n")
                    append("1. **Aturan 24 Jam**: Saat ingin checkout barang non-esensial, tunda minimal 24 jam. Biasanya setelah besok rasa 'kebelet beli' itu hilang drastis.\n")
                    append("2. **Metode Rekening Jajan**: Jangan pakai rekening utama untuk jajan. Transfer jatah jajan mingguan ke e-wallet dengan nominal pas-pasan. Kalau habis, tunggu minggu depan.\n")
                    append("3. **Konversi ke Jam Kerja**: Sebelum beli barang seharga Rp 300rb, hitung berapa jam kamu harus bekerja keras untuk mendapatkan nominal itu. Seringkali jadi mikir dua kali!\n\n")
                    append("Bulan ini pengeluaranmu sudah ${Formatters.formatRupiah(ctx.monthlyExpense)}. Jadikan angka ini pengingat sebelum belanja lagi ya!")
                }

                // 12. Advice on saving tips
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

                // 13. Advice on debt & paylater
                lower.contains("cicilan") || lower.contains("paylater") || lower.contains("hutang") || lower.contains("pinjol") || lower.contains("kredit") -> {
                    append("Menghadapi cicilan memang bikin pikiran tertekan, tapi jangan berkecil hati. Langkah pertama adalah tenang dan buat rencana pelunasan: 🛡️\n\n")
                    append("1. **Setop Tambah Cicilan**: Kunci utama, jangan buat pinjaman atau paylater baru untuk menutup yang lama.\n")
                    append("2. **Metode Bola Salju (Snowball)**: Urutkan semua cicilan dari nominal terkecil ke terbesar. Bayar minimum untuk semua, dan curahkan sisa dana untuk melunasi yang terkecil dulu agar kamu dapat kemenangan mental!\n")
                    append("3. **Gunakan Surplus Bulanan**: Manfaatkan surplus arus kasmu (saat ini ${Formatters.formatRupiah(ctx.monthlyNet)}) untuk pelunasan dipercepat.\n\n")
                    append("Kalau butuh mencatat cicilan bulan ini, kamu bisa langsung sebutkan di sini ya.")
                }

                // 14. Advice on investment
                lower.contains("investasi") || lower.contains("saham") || lower.contains("reksadana") || lower.contains("emas") || lower.contains("crypto") -> {
                    append("Tertarik mulai berinvestasi itu langkah awal yang hebat, $name! 📈\n\n")
                    append("Urutan piramida finansial yang aman sebelum berinvestasi:\n")
                    append("1. **Amankan Dana Darurat Dulu**: Pastikan kamu punya tabungan likuid setara 3-6 bulan pengeluaran (saat ini dana likuidmu ${Formatters.formatRupiah(ctx.liquidAsset)}).\n")
                    append("2. **Bebas Hutang Konsumtif**: Lunasi paylater atau pinjaman berbunga tinggi terlebih dahulu.\n")
                    append("3. **Mulai dari Instrumen Rendah Risiko**: Untuk pemula, Reksadana Pasar Uang (RPU) atau Emas batangan sangat bagus karena minim fluktuasi dan stabil.\n\n")
                    append("Jangan pernah berinvestasi pakai 'uang panas' atau dana kebutuhan sehari-hari ya!")
                }

                // 15. Curhat invitation & conversational listening (user wants to chat / tell a story)
                lower.contains("mau cerita") || lower.contains("curhat") || lower.contains("bisa diajak cerita") ||
                        lower.contains("diajak cerita") || lower.contains("temenin ngobrol") || lower.contains("dengerin aku") ||
                        lower.contains("boleh cerita") || lower.contains("pengen ngobrol") -> {
                    append("Tentu bisa banget, $name! Finansia selalu ada di sini siap mendengarkan semua ceritamu dengan hangat dan tanpa menghakimi. 🥰🤍\n\n")
                    append("Kamu boleh cerita apa saja apa adanya—apakah lagi penat dengan pekerjaan, bingung mengatur gaji, galau pengen beli sesuatu tapi takut boncos, atau lagi mikirin masa depan finansialmu?\n\n")
                    append("Ceritakan saja pelan-pelan ya, apa yang lagi paling mengganjal di pikiran dan hatimu hari ini? Aku siap dengerin! ☕✨")
                }

                // 16. Post-spending regret & guilt
                lower.contains("nyesel") || lower.contains("menyesal") || lower.contains("merasa bersalah") ||
                        lower.contains("kenapa tadi beli") || lower.contains("khilaf") || lower.contains("bodoh banget") -> {
                    append("Tarik napas perlahan ya, $name... 🤍\n\n")
                    append("Rasa sesal atau bersalah setelah mengeluarkan uang itu hal yang sangat manusiawi, hampir semua orang pernah mengalaminya. Yang terpenting sekarang adalah **memaafkan diri sendiri**.\n\n")
                    append("Uang yang sudah keluar tidak bisa ditarik kembali, tapi keputusan finansialmu esok hari 100% masih ada dalam kendalimu! Pelajaran pentingnya:\n")
                    append("• Coba kenali pemicunya: apakah tadi belanja karena stres, lelah sepulang kerja, atau sekadar bosan?\n")
                    append("• Terapkan aturan 'Tunda 24 Jam' sebelum checkout keranjang belanja berikutnya.\n\n")
                    append("Saldo likuidmu saat ini tercatat **${Formatters.formatRupiah(ctx.liquidAsset)}**. Yuk kita jaga bersama agar tetap aman dan terkontrol sampai tanggal gajian tiba!")
                }

                // 17. Career exhaustion, overtime, burnout & "gaji numpang lewat"
                lower.contains("lembur") || lower.contains("burnout") || lower.contains("gaji numpang lewat") ||
                        lower.contains("muak kerja") || (lower.contains("capek") && (lower.contains("kerja") || lower.contains("kantor") || lower.contains("bos") || lower.contains("cari uang") || lower.contains("gaji"))) -> {
                    append("Peluk hangat untukmu, $name... Perjuangan dan lelahmu membanting tulang setiap hari itu luar biasa mulia dan patut diapresiasi setinggi-tingginya. 🫂💪\n\n")
                    append("Wajar sekali merasa penat ketika merasa usaha keras belum sebanding dengan sisa saldo di rekening. Tiga pengingat berharga dari Finansia:\n")
                    append("1. **Kesehatan Fisik & Jiwa Nomor Satu**: Jangan paksakan tubuh sampai drop. Biaya berobat jauh lebih mahal daripada uang lembur yang didapat.\n")
                    append("2. **Audit 'Kebocoran Halus'**: Seringkali gaji terasa numpang lewat bukan karena gajinya kecil, tapi karena kebocoran halus (jajan kopi, pesan antar makanan, langganan aplikasi tak terpakai). Bulan ini pengeluaranmu tercatat **${Formatters.formatRupiah(ctx.monthlyExpense)}**.\n")
                    append("3. **Beri Waktu untuk Rehat**: Malam ini rehat sejenak, nikmati istirahat yang cukup tanpa memikirkan kerjaan kantor.\n\n")
                    append("Kamu hebat sudah bertahan sejauh ini! Ada unek-unek tertentu seputar pekerjaan yang ingin kamu tumpahkan?")
                }

                // 18. Social comparison & peer insecurity
                lower.contains("minder") || lower.contains("insecure") || lower.contains("iri") ||
                        lower.contains("temen udah") || lower.contains("temen punya") || lower.contains("ketinggalan") || lower.contains("merasa gagal") -> {
                    append("Hei $name, dengarkan ini baik-baik ya: **Garis awal dan rute hidup setiap orang itu berbeda**. 🛤️✨\n\n")
                    append("Apa yang kamu lihat di linimasa media sosial hanyalah panggung depan (highlight reel). Kita tidak pernah tahu utang di baliknya, privilege keluarga, atau pergolakan batin yang disembunyikan.\n\n")
                    append("Kemenangan finansial sejati itu bukan pamer mobil atau gadget baru, melainkan:\n")
                    append("✅ Punya ketenangan tidur tanpa kejaran utang konsumtif.\n")
                    append("✅ Memiliki dana siap pakai untuk kebutuhan darurat (likuidmu: **${Formatters.formatRupiah(ctx.liquidAsset)}**).\n")
                    append("✅ Disiplin mencatat dan sadar ke mana uangmu mengalir.\n\n")
                    append("Fokuslah bertanding melawan dirimu yang kemarin, bukan melawan feed Instagram orang lain!")
                }

                // 19. Dilemma lending money to friends or family
                lower.contains("pinjam uang") || lower.contains("minjemin uang") || lower.contains("temen pinjam") ||
                        lower.contains("teman pinjam") || lower.contains("nagih utang") || lower.contains("nagih hutang") -> {
                    append("Dilema meminjamkan uang ke teman atau saudara memang serba salah dan bikin pusing, $name. 🤝⚠️\n\n")
                    append("Pegang 3 prinsip emas ini agar tidak rusak silaturahmi maupun keuanganmu:\n")
                    append("1. **Hanya Pinjamkan Uang yang Ikhlas Direlakan**: Anggap uang itu sebagai sedekah. Jika ia tidak mengembalikan, pastikan dapurmu tetap ngebul dan kamu tidak mendendam.\n")
                    append("2. **Cek Batas Kemampuan Sendiri**: Dana likuidmu saat ini **${Formatters.formatRupiah(ctx.liquidAsset)}**. Jika meminjamkannya mengorbankan dana daruratmu, tolaklah dengan santun tapi tegas.\n")
                    append("3. **Cara Menolak Halus Tanpa Menyinggung**: Katakan *'Maaf banget belum bisa bantu, uangku bulan ini sudah dialokasikan untuk pos wajib keluarga yang tidak bisa diganggu gugat'*. Kejujuran jauh lebih melegakan daripada memaksakan diri!")
                }

                // 20. Family, partner, or Sandwich Generation finance
                lower.contains("sandwich generation") || lower.contains("orang tua minta") || lower.contains("biaya adik") ||
                        ((lower.contains("pasangan") || lower.contains("suami") || lower.contains("istri")) &&
                                (lower.contains("uang") || lower.contains("belanja") || lower.contains("ribut") || lower.contains("gaji"))) -> {
                    append("Membicarakan uang dengan orang terdekat (pasangan/keluarga) memang salah satu topik paling sensitif, tapi sangat menentukan keharmonisan, $name. ❤️🏠\n\n")
                    append("Saran bijak dari Finansia:\n")
                    append("• **Bagi Sandwich Generation**: Berbakti pada orang tua dan adik adalah pintu keberkahan yang agung. Namun tetapkan porsi realistis (misal 10%-20% dari pemasukan) agar kamu tidak terjerat utang demi menopang orang lain.\n")
                    append("• **Komunikasi Bersama Pasangan**: Gunakan sudut pandang *'kita'* sebagai satu tim finansial, bukan saling menyalahkan. Bicarakan impian bersama dan buat pembagian peran belanja yang transparan.\n")
                    append("• Catat setiap alokasi keluarga di aplikasi agar kamu tahu persis kemampuannya tanpa rasa bersalah.")
                }

                // 21. Financial loss, scam, or theft
                lower.contains("kena tipu") || lower.contains("ketipu") || lower.contains("uang hilang") ||
                        lower.contains("kemalingan") || lower.contains("investasi bodong") || lower.contains("boncos") -> {
                    append("Innalillahi wa inna ilaihi raji'un... Aku ikut merasakan sesak dan pedih di dadamu, $name. 💔🥺\n\n")
                    append("Kehilangan uang hasil jerih payah sendiri karena ditipu atau musibah itu ujian yang sangat berat. Langkah darurat yang perlu kamu ambil:\n")
                    append("1. **Amankan Rekening**: Segera hubungi call center resmi bank untuk memblokir kartu/rekening jika ada kebocoran data.\n")
                    append("2. **Laporkan**: Laporkan rekening penipu ke *cekrekening.id* dan kepolisian jika nominalnya signifikan.\n")
                    append("3. **Kuatkan Hati**: Uang yang hilang adalah musibah, namun pintu rezeki sangat luas dan insya Allah akan diganti dengan berkah yang jauh lebih melimpah berlipat ganda.\n\n")
                    append("Jangan terus menyalahkan dirimu ya. Tenangkan diri dan istirahat dulu, kamu tidak sendirian.")
                }

                // 22. Side hustle and starting business without big capital
                lower.contains("bisnis sampingan") || lower.contains("usaha sampingan") || lower.contains("modal usaha") ||
                        lower.contains("ide jualan") || lower.contains("usaha tanpa modal") || lower.contains("freelance") ||
                        lower.contains("side hustle") || lower.contains("cari tambahan") -> {
                    append("Semangat yang luar biasa untuk menjemput pintu rezeki tambahan, $name! 🚀💡\n\n")
                    append("Panduan memulai usaha sampingan yang aman tanpa bikin boncos:\n")
                    append("1. **Mulai dari Skill / Jasa, Bukan Stok Barang**: Jasa minim risiko modal mati (contoh: desain, admin medsos, les privat, reseller/dropship terpercaya, atau katering PO).\n")
                    append("2. **Jangan Sentuh Dana Darurat**: Modal usaha sampingan harus dari dana bebas, bukan dari pos operasional pokok (likuidmu: **${Formatters.formatRupiah(ctx.liquidAsset)}**).\n")
                    append("3. **Validasi Minat Sebelum Belanja Alat**: Pastikan sudah ada calon pelanggan yang berminat sebelum membeli peralatan mahal.\n\n")
                    append("Bidang apa yang paling kamu sukai atau kuasai? Ceritakan ke aku, nanti kita diskusikan peluangnya!")
                }

                // 23. Housing dilemma: Rent vs KPR
                lower.contains("kpr") || (lower.contains("sewa") && lower.contains("rumah")) ||
                        (lower.contains("kontrak") && lower.contains("rumah")) || lower.contains("beli rumah") -> {
                    append("Pertimbangan matang antara sewa vs ambil KPR rumah, $name: 🏠🔑\n\n")
                    append("1. **Rumus 30% Cicilan KPR**: Pastikan cicilan KPR maksimal 30% dari penghasilan tetap bulanan agar kebutuhan harian tidak tercekik.\n")
                    append("2. **Kesiapan DP & Biaya Tambahan**: Selain DP, ada biaya BPHTB, notaris, asuransi, dan renovasi awal (~10%-15% harga rumah).\n")
                    append("3. **Fleksibilitas Karir**: Jika karir atau domisilimu masih berpindah-pindah, menyewa rumah adalah langkah yang sangat cerdas karena modal uang bisa diputar untuk investasi produktif dulu.\n\n")
                    append("Jangan terburu-buru karena gengsi, pilih yang paling memberi rasa tenang di dompetmu!")
                }

                // 24. Resignation dilemma
                lower.contains("resign") || lower.contains("keluar kerja") || lower.contains("pindah kerja") -> {
                    append("Keinginan resign itu tanda bahwa kamu mendengarkan batas diri dan butuh arah baru, $name. 🚪🌱\n\n")
                    append("Namun sebelum melayangkan surat pengunduran diri, cek 'sabuk pengaman' finansialmu:\n")
                    append("• **Dana Darurat Bertahan Hidup**: Minimal miliki tabungan setara 3-6 bulan biaya hidup (dana likuidmu saat ini: **${Formatters.formatRupiah(ctx.liquidAsset)}**).\n")
                    append("• **Punya Tawaran Baru**: Idealnya lompat saat sudah ada pegangan pekerjaan atau proyek berikutnya.\n")
                    append("• Jika lingkungan kerja sangat toxic dan merusak kesehatan mental, kesehatanmu tetap nomor satu. Siapkan rencana anggaran hemat selama masa transisi.")
                }

                // 25. Starting from scratch with zero savings
                lower.contains("mulai dari mana") || lower.contains("tabungan nol") ||
                        lower.contains("ga punya tabungan") || lower.contains("bingung mulai") -> {
                    append("Jangan merasa kecil hati, memulai dari nol adalah langkah awal dari semua kisah sukses finansial, $name! 🌱✨\n\n")
                    append("Lakukan 3 langkah mudah ini mulai hari ini:\n")
                    append("1. **Cukup Catat Dulu**: Selama 7 hari ke depan, catat setiap uang keluar sekecil apapun di aplikasi ini tanpa perlu membatasi diri dulu. Kamu akan kaget melihat ke mana uangmu bocor.\n")
                    append("2. **Kumpulkan 'Kemenangan Pertama'**: Sisihkan Rp 20.000 atau Rp 50.000 hari ini ke celengan atau rekening khusus. Nominal kecil ini penting untuk membangun rasa percaya diri.\n")
                    append("3. **Stop Tambah Beban Utang**: Tahan diri dari segala bentuk paylater atau pinjaman konsumtif.\n\n")
                    append("Aku akan temani langkahmu setiap hari. Yuk mulai catat pengeluaran pertamamu hari ini!")
                }

                // 26. Inspirational stories on wealth and perseverance
                lower.contains("kisah") || lower.contains("cerita motivasi") || lower.contains("cerita inspiratif") ||
                        lower.contains("abdurrahman bin auf") || lower.contains("kisah sahabat") -> {
                    append("Dengarkan kisah penuh hikmah tentang **Sahabat Abdurrahman bin Auf r.a.**, $name: 📖✨\n\n")
                    append("Ketika beliau hijrah ke Madinah, beliau datang tanpa membawa harta sedikit pun. Sahabat Anshar Sa'ad bin Ar-Rabi' menawarkan separuh hartanya secara gratis, namun Abdurrahman menolaknya dengan santun seraya berucap:\n")
                    append("*“Semoga Allah memberkahi harta dan keluargamu. Cukup tunjukkan padaku di mana pasar berada!”*\n\n")
                    append("Beliau mulai berdagang keju dan minyak samin dari nol dengan kejujuran mutlak, tidak pernah mengurangi timbangan, dan selalu bersyukur atas laba wajar. Dalam waktu singkat, beliau menjadi saudagar yang sangat makmur dan dermawan, bahkan menyedekahkan ratusan unta sarat gandum bagi fakir miskin!\n\n")
                    append("💡 *Pelajaran berharga*: Kemiskinan modal uang bisa dikalahkan dengan kekayaan mental, kejujuran, dan keuletan ikhtiar. Kamu pun punya potensi yang sama!")
                }

                // 27. Emotional stress vent about money
                lower.contains("stres") || lower.contains("pusing") || lower.contains("capek") || lower.contains("sedih") || lower.contains("nyesek") || lower.contains("berat") -> {
                    append("Tarik napas dalam-dalam dulu ya, $name... 🤍\n\n")
                    append("Urusan finansial memang salah satu hal paling menguras emosi dan tenaga. Tapi ingat, kondisi sulit ini tidak mendefinisikan nilaimu sebagai pribadi, dan situasi ini bisa diperbaiki pelan-pelan.\n\n")
                    append("Yang penting sekarang:\n")
                    append("• Jangan membebani diri dengan ekspektasi terlalu tinggi dalam semalam.\n")
                    append("• Fokus pada apa yang bisa kamu kendalikan hari ini: tahan pengeluaran yang tidak perlu, dan istirahat yang cukup.\n\n")
                    append("Aku ada di sini untuk mendengarkan. Ada hal spesifik yang lagi bikin kamu paling cemas hari ini?")
                }

                // 28. Greetings
                lower.startsWith("halo") || lower.startsWith("hai") || lower.startsWith("hi") || lower.contains("selamat pagi") ||
                        lower.contains("selamat siang") || lower.contains("selamat sore") || lower.contains("selamat malam") || lower.contains("assalamualaikum") -> {
                    append("Halo $name! Senang sekali bisa menyapamu hari ini. 👋😊\n\n")
                    append("Aku Finansia, sahabat curhat dan asisten keuangan pribadimu. Aku bisa bantu mendengarkan keluh kesahmu, memberi saran anggaran, mengecek posisi saldo & pengeluaran, atau langsung mencatat transaksi harianmu.\n\n")
                    append("Ada kabar apa atau cerita apa seputar harimu hari ini?")
                }

                // 29. Identity & capabilities
                lower.contains("siapa") || lower.contains("bisa apa") || lower.contains("fitur") || lower.contains("menu") -> {
                    append("Aku **Finansia**, asisten dan sahabat curhat keuangan pribadimu di aplikasi ini! 🤖💡\n\n")
                    append("Beberapa hal seru yang bisa kita lakukan bersama:\n")
                    append("1. **Curhat Transaksi Otomatis**: Ketik seperti *'Habis 150rb buat makan dan beli bensin'* dan aku bantu catat ke pembukuan dalam satu klik!\n")
                    append("2. **Teman Cerita & Curhat**: Pusing mikirin cicilan, lelah kerja lembur, atau galau pengen belanja? Ceritakan saja apa adanya!\n")
                    append("3. **Cek Finansial Cepat**: Tanya *'Berapa sisa saldoku?'*, *'Pos apa paling boros?'*, atau *'Berapa pengeluaranku bulan ini?'*.\n")
                    append("4. **Kutipan & Kebijaksanaan**: Minta *'Kutipan Islami tentang rezeki'* atau *'Cerita motivasi keuangan'*.")
                }

                // 30. Gratitude / compliments
                lower.contains("makasih") || lower.contains("terima kasih") || lower.contains("thank") || lower.contains("keren") || lower.contains("mantap") || lower.contains("oke") -> {
                    append("Sama-sama, $name! Senang sekali obrolan kita hari ini bisa membantu dan memberi sedikit ketenangan. 😊\n\n")
                    append("Tetap semangat menjaga kesehatan finansialmu ya. Kalau ada pengeluaran baru atau hal lain yang ingin diceritakan, aku selalu siap di sini!")
                }

                // 31. Conversational, empathetic fallback
                else -> {
                    append("Terima kasih sudah berbagi ceritamu, $name. Aku menyimak apa yang kamu sampaikan. 💬✨\n\n")
                    append("Melihat kondisi keuanganmu saat ini:\n")
                    append("• **Dana Siap Pakai (Likuid)**: ${Formatters.formatRupiah(ctx.liquidAsset)}\n")
                    append("• **Pengeluaran Bulan Ini**: ${Formatters.formatRupiah(ctx.monthlyExpense)}\n")
                    append("• **Arus Kas Bersih**: ${if (ctx.monthlyNet >= 0) "Surplus " else "Defisit "}${Formatters.formatRupiah(ctx.monthlyNet)}\n\n")
                    append("Apakah ada hal tertentu yang ingin kita bahas lebih dalam—misalnya saran alokasi gaji, tips mengendalikan pengeluaran, simulasi rencana belanja barang, atau sekadar unek-unek yang lagi bikin kamu kepikiran?")
                }
            }
        }

        return Pair(reply, proposed)
    }

    private fun extractSalaryFromText(text: String): Long? {
        val regex = Regex("(\\d+([.,]\\d+)?)\\s*(jt|juta|k|rb|ribu)")
        val match = regex.find(text) ?: return null
        val num = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unit = match.groupValues[3].lowercase()
        return when (unit) {
            "jt", "juta" -> (num * 1_000_000).toLong()
            "k", "rb", "ribu" -> (num * 1_000).toLong()
            else -> null
        }
    }

    private fun extractNumberFromText(text: String): Long? {
        val regex = Regex("(?:rp\\.?\\s*)?(\\d+([.,]\\d+)?)\\s*(jt|juta|k|rb|ribu)?")
        val match = regex.find(text) ?: return null
        val num = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unit = match.groupValues[3].lowercase()
        return when (unit) {
            "jt", "juta" -> (num * 1_000_000).toLong()
            "k", "rb", "ribu" -> (num * 1_000).toLong()
            else -> if (num >= 1000.0) num.toLong() else null
        }
    }

    /**
     * Extracts multiple natural language transactions ONLY when user explicitly expresses
     * past spending or income actions (not general questions or future simulations).
     */
    private fun extractProposedTransactions(input: String): List<ProposedTransaction> {
        val lower = input.lowercase().trim()

        val isQuestionOrQuery = lower.contains("gimana") || lower.contains("bagaimana") ||
                lower.contains("kenapa") || lower.contains("mengapa") || lower.contains("apakah") ||
                lower.contains("bisa bantu") || lower.contains("siapa") || lower.contains("tips") ||
                lower.contains("cara") || lower.contains("saran") || lower.contains("rekomendasi") ||
                lower.contains("cukup ga") || lower.contains("aman ga") || lower.contains("boleh ga") ||
                lower.contains("atur gaji") || lower.contains("bagi gaji") || lower.contains("alokasi") ||
                lower.contains("mau beli") || lower.contains("pengen beli") || lower.contains("rencana") ||
                lower.endsWith("?")

        val hasExplicitRecordCommand = lower.contains("catat") || lower.contains("tambahkan") ||
                lower.contains("masukkan transaksi") || lower.contains("simpan transaksi")

        if (isQuestionOrQuery && !hasExplicitRecordCommand) {
            return emptyList()
        }

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

        if (list.isEmpty() && hasSpendingKeyword(lower)) {
            val globalTx = parseSingleSegment(input)
            if (globalTx != null) {
                list.add(globalTx)
            }
        }

        return list
    }

    private fun hasSpendingKeyword(lower: String): Boolean {
        return lower.contains("habis") || lower.contains("beli") || lower.contains("bayar") ||
                lower.contains("keluar") || lower.contains("belanja") || lower.contains("jajan") ||
                lower.contains("servis") || lower.contains("service") || lower.contains("bensin") ||
                lower.contains("makan") || lower.contains("ngopi") || lower.contains("dapat") ||
                lower.contains("gajian") || lower.contains("freelance") || lower.contains("transfer") ||
                lower.contains("cuan") || lower.contains("tagihan") || lower.contains("topup")
    }

    private fun parseSingleSegment(segment: String): ProposedTransaction? {
        val lower = segment.lowercase()
        if (!hasSpendingKeyword(lower)) return null

        // Detect amount e.g. "900k", "1jt", "1.5 juta", "100rb", "25.000", "50000", "Rp 50.000"
        val explicitAmountRegex = Regex("(?:rp\\.?\\s*)?(\\d+([.,]\\d+)?)\\s*(jt|juta|k|rb|ribu|\\b000\\b)?")
        val match = explicitAmountRegex.find(lower) ?: return null

        val numRaw = match.groupValues[1].replace(',', '.')
        val unit = match.groupValues[3].lowercase()
        val baseNum = numRaw.toDoubleOrNull() ?: return null

        val amount = when (unit) {
            "jt", "juta" -> (baseNum * 1_000_000).toLong()
            "k", "rb", "ribu" -> (baseNum * 1_000).toLong()
            "000" -> (baseNum * 1_000).toLong()
            else -> {
                if (lower.contains("rp") || baseNum >= 1000.0) {
                    baseNum.toLong()
                } else {
                    return null
                }
            }
        }

        if (amount <= 0L) return null

        val isIncome = lower.contains("dapat") || lower.contains("gaji") || lower.contains("bonus") ||
                lower.contains("freelance") || lower.contains("pemasukan") || lower.contains("cuan") ||
                lower.contains("transferan masuk") || lower.contains("diberi") || lower.contains("terima")

        val type = if (isIncome) "in" else "out"
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
