package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object OnlineQuoteManager {
    private const val TAG = "OnlineQuoteManager"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Koleksi masif 50+ kutipan finansial dan kebijaksanaan terkurasi
    val CURATED_QUOTES = listOf(
        FinancialQuote(
            quote = "Dan orang-orang yang apabila membelanjakan (harta), mereka tidak berlebihan, dan tidak (pula) kikir, di antara keduanya secara wajar.",
            author = "QS. Al-Furqan: 67",
            category = "OJK_FORMULA",
            tip = "Prinsip washatiyah (keseimbangan): kelola anggaran agar tidak berlebihan melampaui kemampuan dan tidak pula kikir."
        ),
        FinancialQuote(
            quote = "Dan janganlah kamu menghambur-hamburkan (hartamu) secara boros. Sesungguhnya para pemboros itu adalah saudara-saudara setan.",
            author = "QS. Al-Isra': 26-27",
            category = "GENERAL",
            tip = "Jauhi belanja impulsif dan lapar mata; dahulukan kebutuhan primer daripada sekadar gengsi."
        ),
        FinancialQuote(
            quote = "Bukanlah kekayaan itu dengan banyaknya harta benda, melainkan kekayaan yang sejati adalah kekayaan jiwa (hati yang qana'ah).",
            author = "HR. Bukhari no. 6446 & Muslim no. 1051",
            category = "GENERAL",
            tip = "Kekayaan hakiki terletak pada rasa cukup dan bersyukur, bukan perlombaan gaya hidup."
        ),
        FinancialQuote(
            quote = "Sesungguhnya jika kamu bersyukur, niscaya Aku akan menambah (nikmat) kepadamu.",
            author = "QS. Ibrahim: 7",
            category = "TARGET_ACHIEVED",
            tip = "Syukuri setiap rezeki dengan mencatatnya rapi dan membaginya dengan bijak."
        ),
        FinancialQuote(
            quote = "Tangan yang di atas (memberi) lebih baik daripada tangan yang di bawah. Dan mulailah dari orang yang menjadi tanggunganmu.",
            author = "HR. Bukhari no. 1429",
            category = "OJK_FORMULA",
            tip = "Sisihkan porsi sedekah dan nafkah keluarga; sedekah tidak akan mengurangi berkah harta."
        ),
        FinancialQuote(
            quote = "Ikatlah untamu terlebih dahulu, kemudian bertawakkallah kepada Allah.",
            author = "HR. Tirmidzi no. 2517",
            category = "SAVING",
            tip = "Mencatat anggaran dan menyiapkan dana darurat adalah ikhtiar nyata sebelum berserah diri."
        ),
        FinancialQuote(
            quote = "Barangsiapa berutang dengan niat sungguh-sungguh untuk melunasinya, niscaya Allah akan memudahkannya.",
            author = "HR. Bukhari no. 2387",
            category = "OJK_FORMULA",
            tip = "Jaga cicilan maksimal 30% dari pemasukan dan prioritaskan pelunasan utang agar hidup tenang."
        ),
        FinancialQuote(
            quote = "Wahai orang-orang yang beriman! Apabila kamu bermuamalah tidak secara tunai, hendaklah kamu menuliskannya.",
            author = "QS. Al-Baqarah: 282",
            category = "GENERAL",
            tip = "Mencatat transaksi dan piutang secara teratur adalah perintah langsung Al-Qur'an untuk menjaga amanah."
        ),
        FinancialQuote(
            quote = "Harta tidak akan berkurang karena sedekah, dan Allah tidak menambah bagi orang yang memaafkan melainkan kemuliaan.",
            author = "HR. Muslim no. 2588",
            category = "SAVING",
            tip = "Sedekah membuka pintu keberkahan rezeki yang tak terduga."
        ),
        FinancialQuote(
            quote = "Barangsiapa bertakwa kepada Allah niscaya Dia akan membukakan jalan keluar baginya, dan memberinya rezeki dari arah yang tiada disangka-sangkanya.",
            author = "QS. At-Talaq: 2-3",
            category = "GENERAL",
            tip = "Ketakwaan dan tawakkal yang sungguh-sungguh adalah kunci terbukanya rezeki halal yang tak terduga."
        ),
        FinancialQuote(
            quote = "Perumpamaan orang yang menginfakkan hartanya di jalan Allah seperti sebutir biji yang menumbuhkan tujuh tangkai, pada setiap tangkai ada seratus biji.",
            author = "QS. Al-Baqarah: 261",
            category = "OJK_FORMULA",
            tip = "Berbagi dan bersedekah melipatgandakan nilai dan keberkahan setiap rupiah yang kita miliki."
        ),
        FinancialQuote(
            quote = "Sungguh beruntung orang yang masuk Islam, diberi rezeki yang mencukupi, dan Allah menjadikannya qana'ah (merasa cukup) atas apa yang diberikan.",
            author = "HR. Muslim no. 1054",
            category = "SAVING",
            tip = "Sifat qana'ah melindungi diri dari perlombaan gengsi duniawi dan gaya hidup konsumtif."
        ),
        FinancialQuote(
            quote = "Makan dan minumlah, tetapi janganlah berlebih-lebihan. Sesungguhnya Allah tidak menyukai orang-orang yang berlebih-lebihan.",
            author = "QS. Al-A'raf: 31",
            category = "DEFICIT",
            tip = "Hindari perilaku israf (boros melampaui batas); belanjalah sesuai kebutuhan, bukan sekadar lapar mata."
        ),
        FinancialQuote(
            quote = "Seandainya kalian bertawakkal kepada Allah dengan sebenar-benar tawakkal, niscaya kalian diberi rezeki sebagaimana burung: pergi pagi lapar, pulang petang kenyang.",
            author = "HR. Tirmidzi no. 2344",
            category = "GENERAL",
            tip = "Ikhtiar bekerja setiap hari dengan hati yang tenang dan berserah diri kepada Sang Pemberi Rezeki."
        ),
        FinancialQuote(
            quote = "Tidak akan bergeser kedua kaki seorang hamba pada hari kiamat hingga ditanya tentang hartanya: dari mana ia peroleh dan ke mana ia belanjakan.",
            author = "HR. Tirmidzi no. 2417",
            category = "OJK_FORMULA",
            tip = "Mencatat setiap pengeluaran dan memastikan sumber halal adalah bentuk amanah menjaga titipan rezeki."
        ),
        FinancialQuote(
            quote = "Pedagang yang jujur dan dapat dipercaya akan bersama para nabi, orang-orang yang benar (shiddiqin), dan para syuhada.",
            author = "HR. Tirmidzi no. 1209 & Bukhari",
            category = "TARGET_ACHIEVED",
            tip = "Kejujuran dalam mencari nafkah dan berbisnis membawa berkah dan ketenangan batin yang abadi."
        ),
        FinancialQuote(
            quote = "Manfaatkan lima perkara sebelum lima perkara: waktu mudamu sebelum tuamu, sehatmu sebelum sakitmu, kayamu sebelum miskinmu, luangmu sebelum sempitmu, dan hidupmu sebelum matimu.",
            author = "HR. Al-Hakim no. 7846 (Shahih)",
            category = "SAVING",
            tip = "Persiapkan dana darurat dan tabungan masa depan selagi masih sehat, produktif, dan memiliki penghasilan."
        ),
        FinancialQuote(
            quote = "Lihatlah orang yang berada di bawahmu (dalam urusan dunia/harta) dan jangan melihat orang yang berada di atasmu, agar kamu tidak meremehkan nikmat Allah.",
            author = "HR. Muslim no. 2564",
            category = "GENERAL",
            tip = "Fokus pada rasa syukur atas rezeki diri sendiri, bukan membandingkan gaya hidup dengan orang lain di media sosial."
        ),
        FinancialQuote(
            quote = "Sesungguhnya Allah tidak akan mengubah keadaan suatu kaum sebelum mereka mengubah keadaan diri mereka sendiri.",
            author = "QS. Ar-Ra'd: 11",
            category = "TARGET_NEAR",
            tip = "Perbaikan kondisi keuangan dimulai dari kemauan diri sendiri untuk disiplin mencatat dan mengendalikan pengeluaran harian."
        ),
        FinancialQuote(
            quote = "Menunda-nunda pembayaran utang bagi orang yang mampu adalah suatu kezaliman.",
            author = "HR. Bukhari no. 2408 & Muslim no. 1628",
            category = "OJK_FORMULA",
            tip = "Segera lunasi utang dan tagihan begitu memiliki uang; jangan tunda hak orang lain agar rezeki tetap lapang."
        ),
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
            author = "Pepatah Finansial",
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
        ),
        FinancialQuote(
            quote = "Sebagian dari apa yang kamu hasilkan adalah milikmu untuk disimpan utuh.",
            author = "George S. Clason (The Richest Man in Babylon)",
            category = "SAVING",
            tip = "Minimal 10% dari penghasilan kotor harus disisihkan untuk tabungan masa depan dan tidak boleh disentuh."
        ),
        FinancialQuote(
            quote = "Kekayaan adalah apa yang tidak kamu lihat: mobil yang tidak dibeli, jam tangan mewah yang tidak dipakai.",
            author = "Morgan Housel (The Psychology of Money)",
            category = "GENERAL",
            tip = "Kaya (rich) adalah memiliki uang untuk dibelanjakan saat ini; makmur (wealthy) adalah memiliki aset yang tak terlihat."
        ),
        FinancialQuote(
            quote = "Jangan menaruh semua telurmu dalam satu keranjang yang sama.",
            author = "Pepatah Investasi Kuno",
            category = "SAVING",
            tip = "Diversifikasikan simpananmu antara kas likuid, tabungan bank, dan instrumen investasi yang aman."
        ),
        FinancialQuote(
            quote = "Jika kamu membeli barang yang tidak kamu butuhkan, segera kamu harus menjual barang yang kamu butuhkan.",
            author = "Warren Buffett",
            category = "GENERAL",
            tip = "Evaluasi kebutuhan vs keinginan sebelum membeli barang elektronik atau pakaian baru."
        ),
        FinancialQuote(
            quote = "Dana darurat bukanlah investasi untuk mencari keuntungan, melainkan asuransi untuk ketenangan jiwa.",
            author = "Dave Ramsey",
            category = "TARGET_NEAR",
            tip = "Siapkan minimal 3 hingga 6 kali pengeluaran bulanan di instrumen likuid seperti rekening terpisah."
        ),
        FinancialQuote(
            quote = "Bunga majemuk adalah keajaiban dunia kedelapan. Siapa yang memahaminya, menghasilkannya; siapa yang tidak, membayarnya.",
            author = "Albert Einstein",
            category = "SAVING",
            tip = "Mulai menabung dan berinvestasi sedini mungkin agar efek bunga majemuk melipatgandakan asetmu."
        ),
        FinancialQuote(
            quote = "Rencana keuangan yang baik hari ini lebih baik daripada rencana keuangan sempurna minggu depan.",
            author = "Carl Richards",
            category = "GENERAL",
            tip = "Mencatat transaksi sederhana jauh lebih berdampak daripada menunda pencatatan karena menunggu waktu luang."
        ),
        FinancialQuote(
            quote = "Kemampuan untuk menunda kepuasan instan adalah prediktor terbaik kesuksesan finansial jangka panjang.",
            author = "Thomas J. Stanley (The Millionaire Next Door)",
            category = "GENERAL",
            tip = "Tahan godaan diskon kilat dan flash sale jika barang tersebut tidak ada dalam daftar kebutuhan pokokmu."
        ),
        FinancialQuote(
            quote = "Jangan biarkan uang mengatur hidupmu, jadilah tuan atas setiap sen yang mengalir di dompetmu.",
            author = "Suze Orman",
            category = "OJK_FORMULA",
            tip = "Tetapkan batas maksimal belanja kategori hiburan setiap awal bulan."
        ),
        FinancialQuote(
            quote = "Kekayaan tidak diukur dari seberapa mewah gaya hidupmu, melainkan seberapa lama kamu bisa bertahan tanpa harus bekerja jika krisis melanda.",
            author = "Robert Kiyosaki",
            category = "SAVING",
            tip = "Hitung runway keuanganmu: Total Tabungan Likuid dibagi Rata-rata Pengeluaran Bulanan."
        ),
        FinancialQuote(
            quote = "Hiduplah di bawah kemampuanmu hari ini agar kelak kamu bisa hidup melampaui kemampuan kebanyakan orang.",
            author = "Pepatah Bijak Keuangan",
            category = "TARGET_ACHIEVED",
            tip = "Hidup hemat bukan berarti pelit, melainkan bijak memprioritaskan hal-hal yang benar-benar bernilai abadi."
        ),
        FinancialQuote(
            quote = "Paylater dan kartu kredit adalah alat bantu transaksi, bukan uang tambahan gratis.",
            author = "Edukasi Finansial OJK",
            category = "OJK_FORMULA",
            tip = "Selalu bayar tagihan tepat waktu dan penuh (full payment) sebelum jatuh tempo agar tidak terjerat denda."
        ),
        FinancialQuote(
            quote = "Banyak orang menghabiskan uang yang belum mereka miliki untuk membeli barang yang tidak mereka sukai demi mengesankan orang yang tidak mereka pedulikan.",
            author = "Will Rogers",
            category = "GENERAL",
            tip = "Bebaskan dirimu dari tekanan sosial dan gengsi semu yang menguras saldo rekening."
        ),
        FinancialQuote(
            quote = "Kunci mengumpulkan kekayaan adalah mengubah pendapatan aktif menjadi aset produktif yang terus bertumbuh.",
            author = "Peter Lynch",
            category = "SAVING",
            tip = "Gunakan surplus anggaran bulanan untuk menambah aset investasi atau modal usaha kecil."
        ),
        FinancialQuote(
            quote = "Ketenangan tidur di malam hari tanpa beban cicilan adalah salah satu bentuk kekayaan paling mewah di dunia modern.",
            author = "Raditya Dika",
            category = "GENERAL",
            tip = "Prioritaskan melunasi utang berbunga tinggi terlebih dahulu (metode Avalanche/Snowball)."
        ),
        FinancialQuote(
            quote = "Satu-satunya cara pasti untuk meningkatkan tabunganmu adalah dengan memperlebar jarak antara pendapatan dan pengeluaran.",
            author = "Nick Maggiulli (Just Keep Buying)",
            category = "SAVING",
            tip = "Fokus pada dua hal: kendalikan biaya hidup dan cari peluang peningkatan penghasilan tambahan."
        ),
        FinancialQuote(
            quote = "Uang adalah pelayan yang sangat baik, tetapi merupakan majikan yang sangat kejam.",
            author = "Francis Bacon",
            category = "DEFICIT",
            tip = "Atur uangmu dengan sistem anggaran yang jelas sebelum uangmu mengendalikan pilihan hidupmu."
        ),
        FinancialQuote(
            quote = "Jangan biarkan gaya hidup naik lebih cepat daripada kenaikan gaji pertamamu.",
            author = "Felicia Putri Tjiasaka",
            category = "SAVING",
            tip = "Ketika mendapat kenaikan gaji atau bonus, alokasikan minimal 50% porsi kenaikan tersebut ke tabungan."
        ),
        FinancialQuote(
            quote = "Investasi pada pengetahuan selalu menghasilkan bunga terbaik.",
            author = "Benjamin Franklin",
            category = "GENERAL",
            tip = "Luangkan waktu membaca buku literasi finansial atau mengikuti pelatihan pengembangan diri."
        ),
        FinancialQuote(
            quote = "Kamu tidak perlu menjadi jenius finansial untuk menjadi kaya, kamu hanya perlu tidak melakukan kesalahan bodoh berulang kali.",
            author = "Charlie Munger",
            category = "GENERAL",
            tip = "Hindari investasi bodong dan skema cepat kaya (get-rich-quick) yang menjanjikan keuntungan tidak masuk akal."
        ),
        FinancialQuote(
            quote = "Setiap kali kamu menolak membeli barang yang tidak perlu, kamu sebenarnya sedang membeli sedikit kebebasan di masa depan.",
            author = "Naval Ravikant",
            category = "SAVING",
            tip = "Uang yang disimpan adalah modal kebebasan waktu untuk memilih karir dan kehidupan yang kamu cintai."
        ),
        FinancialQuote(
            quote = "Ukur kemajuan keuanganmu terhadap tujuan pribadimu, bukan terhadap unggahan media sosial orang lain.",
            author = "Saham & Finansial Indonesia",
            category = "TARGET_NEAR",
            tip = "Fokus pada angka di laporan keuanganmu sendiri, bukan rumor atau pamer gaya hidup di linimasa."
        ),
        FinancialQuote(
            quote = "Sedikit demi sedikit, lama-lama menjadi bukit. Konsistensi harian selalu mengalahkan niat sesaat.",
            author = "Pepatah Tradisional Nusantara",
            category = "SAVING",
            tip = "Menyisihkan Rp 15.000 per hari konsisten selama setahun setara dengan lebih dari Rp 5.400.000 tabungan!"
        )
    )

    private val _currentQuote = MutableStateFlow(CURATED_QUOTES.first())
    val currentQuote: StateFlow<FinancialQuote> = _currentQuote.asStateFlow()

    private var quoteIndex = 0

    init {
        // Inisialisasi kutipan acak saat pertama kali
        val initialIndex = Random.nextInt(CURATED_QUOTES.size)
        quoteIndex = initialIndex
        _currentQuote.value = CURATED_QUOTES[initialIndex]
    }

    /**
     * Memutar ke kutipan berikutnya.
     * Jika [preferOnline] = true, mencoba mengambil dari server online terlebih dahulu.
     * Jika gagal atau offline, mengambil dari katalog lokal 50+ kutipan.
     */
    suspend fun fetchNextQuote(preferOnline: Boolean = true): FinancialQuote = withContext(Dispatchers.IO) {
        if (preferOnline) {
            try {
                val onlineQuote = fetchOnlineQuoteFromServer()
                if (onlineQuote != null) {
                    _currentQuote.value = onlineQuote
                    return@withContext onlineQuote
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mengambil kutipan dari server online: ${e.message}, menggunakan koleksi lokal.")
            }
        }

        // Fallback: putar dari koleksi lokal
        quoteIndex = (quoteIndex + 1) % CURATED_QUOTES.size
        val nextLocal = CURATED_QUOTES[quoteIndex]
        _currentQuote.value = nextLocal
        return@withContext nextLocal
    }

    /**
     * Mengambil kutipan inspirasi dari API server online publik.
     */
    private fun fetchOnlineQuoteFromServer(): FinancialQuote? {
        // Mencoba endpoint publik (DummyJSON quotes / Quotable)
        val endpoints = listOf(
            "https://dummyjson.com/quotes/random",
            "https://api.quotable.io/random?tags=wisdom,business,success"
        )

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "CatatanUangApp/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use null
                        val json = JSONObject(body)

                        val rawQuote = json.optString("quote").ifBlank { json.optString("content") }
                        val rawAuthor = json.optString("author", "Inspirasi Global").ifBlank { "Inspirasi Global" }

                        if (rawQuote.isNotBlank()) {
                            val tip = when {
                                rawQuote.contains("money", ignoreCase = true) || rawQuote.contains("wealth", ignoreCase = true) ->
                                    "Kutipan finansial internasional: kelola aset dengan bijak demi stabilitas masa depan."
                                rawQuote.contains("time", ignoreCase = true) ->
                                    "Waktu adalah aset paling berharga; investasikan waktumu untuk meningkatkan nilai diri."
                                else ->
                                    "Kutipan motivasi harian: disiplin dan konsistensi adalah kunci utama kebebasan finansial."
                            }

                            return FinancialQuote(
                                quote = rawQuote,
                                author = rawAuthor,
                                category = "GENERAL",
                                tip = tip,
                                isOnline = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Gagal request endpoint $endpoint: ${e.message}")
            }
        }
        return null
    }
}
