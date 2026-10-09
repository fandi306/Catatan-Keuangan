package com.example.util

import java.util.Calendar

data class FinancialQuote(
    val quote: String,
    val author: String,
    val category: String, // "DEFICIT", "TARGET_ACHIEVED", "TARGET_NEAR", "SAVING", "OJK_FORMULA", "GENERAL"
    val tip: String
)

data class FinancialWarning(
    val title: String,
    val message: String,
    val severity: WarningSeverity,
    val iconType: String
)

enum class WarningSeverity {
    INFO,
    WARNING,
    CRITICAL,
    SUCCESS
}

data class FinancialAdvice(
    val hourlyQuote: FinancialQuote,
    val dailyQuote: FinancialQuote,
    val warnings: List<FinancialWarning>,
    val foodExpensePercentage: Float,
    val savingsPercentage: Float,
    val ojkBudgetTips: List<BudgetRuleTip>
)

data class BudgetRuleTip(
    val title: String,
    val idealPercentage: String,
    val currentPercentage: String,
    val status: String, // "OK", "OVER", "BELOW"
    val advice: String,
    val formulaBadge: String
)

object FinancialAdvisor {

    // Pool quotes terkurasi untuk rotasi harian & per jam
    private val HOURLY_QUOTES = listOf(
        FinancialQuote(
            quote = "Jangan menabung apa yang tersisa setelah dibelanjakan, tapi belanjakan apa yang tersisa setelah menabung.",
            author = "Warren Buffett",
            category = "SAVING",
            tip = "Gunakan metode 'Pay Yourself First': sisihkan tabungan di awal gajian sebelum mulai membelanjakannya."
        ),
        FinancialQuote(
            quote = "Waspadalah terhadap pengeluaran kecil; kebocoran kecil bisa menenggelamkan kapal yang besar.",
            author = "Benjamin Franklin",
            category = "GENERAL",
            tip = "Jajan kopi dan delivery harian seringkali menjadi pengeluaran terbesar yang tidak disadari."
        ),
        FinancialQuote(
            quote = "Kemenangan finansial bukanlah tentang sekali lompatan besar, melainkan serangkaian kebiasaan kecil yang konsisten.",
            author = "James Clear",
            category = "TARGET_NEAR",
            tip = "Konsistensi mencatat setiap rupiah adalah langkah awal kestabilan keuangan pribadi."
        ),
        FinancialQuote(
            quote = "Disiplin finansial hari ini adalah kemerdekaan finansial di masa depan.",
            author = "Dave Ramsey",
            category = "TARGET_ACHIEVED",
            tip = "Rayakan setiap capaian keuangan dengan memperkuat dana darurat dan investasi."
        ),
        FinancialQuote(
            quote = "Bukan berapa banyak uang yang kamu hasilkan, tapi berapa banyak uang yang kamu simpan.",
            author = "Robert Kiyosaki",
            category = "SAVING",
            tip = "Kenaikan penghasilan hendaknya diikuti kenaikan tabungan, bukan kenaikan gaya hidup semata."
        ),
        FinancialQuote(
            quote = "Kekayaan sejati bukanlah memiliki banyak harta benda, melainkan memiliki sedikit keinginan konsumtif.",
            author = "Epictetus",
            category = "GENERAL",
            tip = "Terapkan jeda 24 jam sebelum checkout keranjang belanja online untuk menghindari impulse buying."
        ),
        FinancialQuote(
            quote = "Anggaran bukanlah rantai yang membelenggu, melainkan kompas yang memberi kebebasan finansial tanpa rasa bersalah.",
            author = "John C. Maxwell",
            category = "OJK_FORMULA",
            tip = "Patuhi rumus 40-30-20-10 OJK agar hidup tenang tanpa jeratan beban utang berlebihan."
        ),
        FinancialQuote(
            quote = "Orang cerdas mengendalikan uangnya, orang lalai dikendalikan oleh uang dan nafsu belanjanya.",
            author = "Pribahasa Finansial",
            category = "DEFICIT",
            tip = "Jika bulan ini defisit, segera audit pengeluaran non-primer dan tunda pembelian barang tersier."
        ),
        FinancialQuote(
            quote = "Investasi terbaik yang bisa kamu lakukan adalah investasi pada ketenangan pikiran finansialmu.",
            author = "Charlie Munger",
            category = "SAVING",
            tip = "Alokasikan 20% penghasilan ke tabungan dan instrumen investasi yang terdaftar resmi di OJK."
        ),
        FinancialQuote(
            quote = "Tangan di atas lebih baik daripada tangan di bawah; berbagi tidak akan membuat hartamu berkurang.",
            author = "Pepatah Bijak",
            category = "OJK_FORMULA",
            tip = "Alokasikan 10% untuk donasi, zakat, amal, atau kegiatan sosial demi keberkahan rezeki."
        ),
        FinancialQuote(
            quote = "Utang konsumtif membeli kesenangan sesaat dengan menggadaikan masa depan.",
            author = "Morgan Housel",
            category = "OJK_FORMULA",
            tip = "Pastikan cicilan atau utang maksimal 30% dari pemasukan agar arus kas bulanan tetap sehat."
        ),
        FinancialQuote(
            quote = "Mengetahui ke mana uangmu pergi jauh lebih melegakan daripada bertanya-tanya ke mana ia hilang.",
            author = "Anonim",
            category = "GENERAL",
            tip = "Buka aplikasi Catatan Keuangan secara rutin setiap kali selesai bertransaksi."
        )
    )

    fun evaluate(
        income: Long,
        expense: Long,
        totalAsset: Long,
        savingsTarget: Long,
        categoryBreakdown: List<Pair<String, Long>>
    ): FinancialAdvice {
        val warnings = mutableListOf<FinancialWarning>()

        val foodExpense = categoryBreakdown.find { it.first.equals("Makan", ignoreCase = true) }?.second ?: 0L
        val debtExpense = categoryBreakdown.filter {
            it.first.contains("Cicilan", ignoreCase = true) ||
            it.first.contains("Utang", ignoreCase = true) ||
            it.first.contains("Tagihan", ignoreCase = true)
        }.sumOf { it.second }
        val socialExpense = categoryBreakdown.filter {
            it.first.contains("Donasi", ignoreCase = true) ||
            it.first.contains("Amal", ignoreCase = true) ||
            it.first.contains("Sosial", ignoreCase = true) ||
            it.first.contains("Sedekah", ignoreCase = true)
        }.sumOf { it.second }

        val net = income - expense

        val baseForPercent = if (income > 0) income else expense

        val foodPct = if (baseForPercent > 0) {
            (foodExpense.toFloat() / baseForPercent.toFloat()) * 100f
        } else 0f

        val savingsPct = if (income > 0) {
            ((income - expense).coerceAtLeast(0).toFloat() / income.toFloat()) * 100f
        } else 0f

        val targetProgress = if (savingsTarget > 0) {
            (totalAsset.toFloat() / savingsTarget.toFloat()) * 100f
        } else 0f

        // 1. Peringatan Defisit (Pengeluaran > Pemasukan)
        if (expense > income && income > 0) {
            val deficitAmount = expense - income
            warnings.add(
                FinancialWarning(
                    title = "Perhatian: Pengeluaran Melebihi Pemasukan!",
                    message = "Pengeluaran Anda bulan ini lebih besar ${Formatters.formatRupiah(deficitAmount)} dari pemasukan. Mohon rem pengeluaran non-primer dan tunda belanja barang yang belum mendesak agar tabungan tidak terkuras.",
                    severity = WarningSeverity.CRITICAL,
                    iconType = "warning_deficit"
                )
            )
        } else if (income > 0 && expense > (income * 0.85).toLong() && expense <= income) {
            warnings.add(
                FinancialWarning(
                    title = "Waspada: Arus Kas Mendekati Batas",
                    message = "Pengeluaran sudah mencapai lebih dari 85% dari pemasukan. Sisakan margin aman untuk dana darurat tak terduga minggu ini.",
                    severity = WarningSeverity.WARNING,
                    iconType = "warning_near_limit"
                )
            )
        }

        // 2. Evaluasi Pengeluaran Makan
        if (foodPct > 35f && baseForPercent > 0) {
            warnings.add(
                FinancialWarning(
                    title = "Pengeluaran Makan Tinggi (${foodPct.toInt()}%)",
                    message = "Porsi belanja makanan Anda sudah ${foodPct.toInt()}% dari anggaran. Idealnya makan berkisar 20%-30% agar pos biaya hidup lainnya tidak tertekan.",
                    severity = WarningSeverity.WARNING,
                    iconType = "warning_food"
                )
            )
        }

        // 3. Evaluasi Target Tabungan
        if (savingsTarget > 0 && totalAsset >= savingsTarget) {
            warnings.add(
                FinancialWarning(
                    title = "Selamat! Target Tabungan Tercapai 🎉",
                    message = "Total aset Anda sudah mencapai ${Formatters.formatRupiah(totalAsset)} dari target ${Formatters.formatRupiah(savingsTarget)}. Luar biasa, terus pertahankan konsistensi finansial ini!",
                    severity = WarningSeverity.SUCCESS,
                    iconType = "target_achieved"
                )
            )
        } else if (savingsTarget > 0 && targetProgress >= 80f) {
            val remaining = savingsTarget - totalAsset
            warnings.add(
                FinancialWarning(
                    title = "Hampir Sampai di Sasaran! (${targetProgress.toInt()}%)",
                    message = "Tinggal ${Formatters.formatRupiah(remaining)} lagi untuk mencapai target impianmu. Sedikit dorongan lagi!",
                    severity = WarningSeverity.INFO,
                    iconType = "target_near"
                )
            )
        }

        // Rotasi Quotes Dinamis:
        // Jam berganti setiap jam (0..23)
        // Hari berganti setiap hari (day of year)
        val cal = Calendar.getInstance()
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val hourlyQuote = if (expense > income && income > 0) {
            FinancialQuote(
                quote = "Jangan menabung apa yang tersisa setelah dibelanjakan, tapi belanjakan apa yang tersisa setelah menabung.",
                author = "Warren Buffett",
                category = "DEFICIT",
                tip = "Pengeluaran sedang melebihi pemasukan. Rem transaksi gaya hidup dan prioritaskan kebutuhan esensial."
            )
        } else if (savingsTarget > 0 && totalAsset >= savingsTarget) {
            FinancialQuote(
                quote = "Disiplin finansial hari ini adalah kemerdekaan finansial di masa depan. Selamat atas pencapaian targetmu!",
                author = "Dave Ramsey",
                category = "TARGET_ACHIEVED",
                tip = "Saat target tercapai, pertimbangkan untuk menaikkan target baru atau mengamankannya di instrumen berisiko rendah."
            )
        } else {
            HOURLY_QUOTES[hourOfDay % HOURLY_QUOTES.size]
        }

        val dailyQuote = HOURLY_QUOTES[dayOfYear % HOURLY_QUOTES.size]

        // 4. Panduan & Tips Rumus 40-30-20-10 (Saran OJK / untuk Pengendalian Utang)
        val ojkTips = listOf(
            BudgetRuleTip(
                title = "Biaya Hidup & Kebutuhan Pokok",
                idealPercentage = "40%",
                currentPercentage = if (income > 0) "${((expense - debtExpense - socialExpense).coerceAtLeast(0).toFloat() / income.toFloat() * 100f).toInt()}%" else "${foodPct.toInt()}%",
                status = if (income > 0 && (expense - debtExpense - socialExpense) > income * 0.45f) "OVER" else "OK",
                advice = "Mencakup makanan, tempat tinggal/kos, listrik, air, dan transportasi harian. Porsi maksimal 40% agar pos tabungan dan cicilan aman.",
                formulaBadge = "40% Pokok"
            ),
            BudgetRuleTip(
                title = "Cicilan & Pembayaran Utang",
                idealPercentage = "Maksimal 30%",
                currentPercentage = if (income > 0 && debtExpense > 0) "${(debtExpense.toFloat() / income.toFloat() * 100f).toInt()}%" else "0%",
                status = if (income > 0 && debtExpense > income * 0.30f) "OVER" else "OK",
                advice = "Saran OJK: Batas maksimal seluruh cicilan atau utang adalah 30% dari penghasilan bulanan untuk mencegah gagal bayar.",
                formulaBadge = "30% Cicilan"
            ),
            BudgetRuleTip(
                title = "Tabungan & Investasi",
                idealPercentage = "Minimal 20%",
                currentPercentage = "${savingsPct.toInt()}%",
                status = if (savingsPct >= 20f || income == 0L) "OK" else "BELOW",
                advice = "Sisihkan langsung di awal gajian untuk dana darurat, tabungan masa depan, dan portofolio investasi terpercaya.",
                formulaBadge = "20% Tabungan"
            ),
            BudgetRuleTip(
                title = "Donasi, Amal & Kegiatan Sosial",
                idealPercentage = "10%",
                currentPercentage = if (income > 0 && socialExpense > 0) "${(socialExpense.toFloat() / income.toFloat() * 100f).toInt()}%" else "Tercatat",
                status = "OK",
                advice = "Alokasikan 10% untuk kegiatan sosial, zakat, infak, atau berbagi kepada sesama demi menjaga keberkahan dan kepedulian.",
                formulaBadge = "10% Donasi"
            )
        )

        return FinancialAdvice(
            hourlyQuote = hourlyQuote,
            dailyQuote = dailyQuote,
            warnings = warnings,
            foodExpensePercentage = foodPct,
            savingsPercentage = savingsPct,
            ojkBudgetTips = ojkTips
        )
    }
}
