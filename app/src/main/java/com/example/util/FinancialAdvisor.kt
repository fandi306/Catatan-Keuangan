package com.example.util

import java.util.Calendar

data class FinancialQuote(
    val quote: String,
    val author: String,
    val category: String, // "DEFICIT", "TARGET_ACHIEVED", "TARGET_NEAR", "SAVING", "OJK_FORMULA", "GENERAL"
    val tip: String,
    val isOnline: Boolean = false
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

    // Menggunakan pool masif 35+ quotes terkurasi dan sinkronisasi server online
    private val HOURLY_QUOTES get() = OnlineQuoteManager.CURATED_QUOTES

    fun evaluate(
        income: Long,
        expense: Long,
        totalAsset: Long,
        savingsTarget: Long,
        categoryBreakdown: List<Pair<String, Long>>
    ): FinancialAdvice {
        val warnings = mutableListOf<FinancialWarning>()

        val basicNeedsExpense = categoryBreakdown.filter {
            BudgetCategoryHelper.getGroupForCategory(it.first) == BudgetGroup.POKOK
        }.sumOf { it.second }

        val debtExpense = categoryBreakdown.filter {
            BudgetCategoryHelper.getGroupForCategory(it.first) == BudgetGroup.CICILAN
        }.sumOf { it.second }

        val directSavingsExpense = categoryBreakdown.filter {
            BudgetCategoryHelper.getGroupForCategory(it.first) == BudgetGroup.TABUNGAN
        }.sumOf { it.second }

        val socialExpense = categoryBreakdown.filter {
            BudgetCategoryHelper.getGroupForCategory(it.first) == BudgetGroup.SOSIAL
        }.sumOf { it.second }

        val net = income - expense

        val baseForPercent = if (income > 0) income else expense

        val foodExpense = categoryBreakdown.filter { it.first.contains("Makan", ignoreCase = true) }.sumOf { it.second }

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
        val basicPct = if (income > 0) {
            ((basicNeedsExpense.toFloat() / income.toFloat()) * 100f).toInt()
        } else if (expense > 0) {
            ((basicNeedsExpense.toFloat() / expense.toFloat()) * 100f).toInt()
        } else 0

        val debtPct = if (income > 0) {
            ((debtExpense.toFloat() / income.toFloat()) * 100f).toInt()
        } else if (expense > 0) {
            ((debtExpense.toFloat() / expense.toFloat()) * 100f).toInt()
        } else 0

        val effectiveSavings = (net.coerceAtLeast(0) + directSavingsExpense)
        val savingsEffPct = if (income > 0) {
            ((effectiveSavings.toFloat() / income.toFloat()) * 100f).toInt()
        } else 0

        val socialPct = if (income > 0) {
            ((socialExpense.toFloat() / income.toFloat()) * 100f).toInt()
        } else if (expense > 0) {
            ((socialExpense.toFloat() / expense.toFloat()) * 100f).toInt()
        } else 0

        val ojkTips = listOf(
            BudgetRuleTip(
                title = "Biaya Hidup & Kebutuhan Pokok",
                idealPercentage = "40%",
                currentPercentage = "$basicPct%",
                status = if (basicPct > 45) "OVER" else "OK",
                advice = "Kategori: Makan, Kos, Belanja Bulanan, Transport, Kesehatan. Terpakai: ${Formatters.formatRupiah(basicNeedsExpense)}. Rekomendasi maksimal 40% agar pos tabungan dan cicilan tetap aman.",
                formulaBadge = "40% Pokok"
            ),
            BudgetRuleTip(
                title = "Cicilan & Pembayaran Utang",
                idealPercentage = "Maksimal 30%",
                currentPercentage = "$debtPct%",
                status = if (debtPct > 30) "OVER" else "OK",
                advice = "Kategori: Cicilan & Kredit, Tagihan & Utang. Terpakai: ${Formatters.formatRupiah(debtExpense)}. Saran OJK: Batas maksimal seluruh cicilan adalah 30% dari pendapatan.",
                formulaBadge = "30% Cicilan"
            ),
            BudgetRuleTip(
                title = "Tabungan & Investasi",
                idealPercentage = "Minimal 20%",
                currentPercentage = "$savingsEffPct%",
                status = if (savingsEffPct >= 20 || income == 0L) "OK" else "BELOW",
                advice = "Kategori: Tabungan Masa Depan, Investasi, Dana Darurat. Terkumpul: ${Formatters.formatRupiah(effectiveSavings)}. Sisihkan minimal 20% langsung saat gajian.",
                formulaBadge = "20% Tabungan"
            ),
            BudgetRuleTip(
                title = "Donasi, Amal & Gaya Hidup",
                idealPercentage = "10%",
                currentPercentage = "$socialPct%",
                status = if (socialPct >= 5 || socialExpense > 0) "OK" else "BELOW",
                advice = "Kategori: Donasi, Sedekah, Hiburan, Sosial & Kado. Terpakai: ${Formatters.formatRupiah(socialExpense)}. Alokasikan hingga 10% untuk kepedulian sosial & rekreasi terukur.",
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
