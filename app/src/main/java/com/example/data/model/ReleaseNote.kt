package com.example.data.model

data class ReleaseCategory(
    val title: String,
    val emoji: String,
    val items: List<String>
)

data class ReleaseNote(
    val version: String,
    val releaseDate: String,
    val isLatest: Boolean = false,
    val title: String,
    val highlights: List<String>,
    val categories: List<ReleaseCategory>
)

object ReleaseNotesData {
    val NOTES = listOf(
        ReleaseNote(
            version = "1.1.1",
            releaseDate = "15 Oktober 2026",
            isLatest = true,
            title = "Pembaruan v1.1.1 Beta: Format Bold Chatbot, Percakapan Alami & Kutipan Islami",
            highlights = listOf(
                "Dukungan format teks tebal (bold) otomatis di bubble chat Curhat AI tanpa tanda bintang (*).",
                "Kecerdasan emosional Curhat AI: kini nyaman diajak bercerita, curhat lelah kerja, dilema utang, dan belanja.",
                "Tampilan input pesan mulus tepat di atas keyboard tanpa kotak hitam atau celah kosong.",
                "Koleksi mutiara finansial Islami dari Al-Qur'an dan Hadits seputar rezeki halal, qana'ah, dan berkah sedekah."
            ),
            categories = listOf(
                ReleaseCategory(
                    title = "Curhat AI & Percakapan Alami",
                    emoji = "🤖",
                    items = listOf(
                        "Dukungan styling Markdown: teks **tebal** otomatis tampil menggunakan font bold yang rapi tanpa tanda bintang.",
                        "Empati mendalam: Finansia kini merespons curhat lelah bekerja, rasa sesal setelah belanja, minder dengan pencapaian teman, dan dilema pinjam-meminjam uang.",
                        "Penambahan respon cerdas untuk pembagian gaji (metode 50-30-20), pertimbangan membeli barang impian, dan tips bertahan saat saldo menipis.",
                        "Integrasi kutipan inspiratif dari Al-Qur'an (QS. Al-Furqan 67, Al-Isra 26-27, At-Talaq 2-3, Al-Baqarah 261) dan Hadits shahih tentang keberkahan harta."
                    )
                ),
                ReleaseCategory(
                    title = "Perbaikan Antarmuka & Keyboard",
                    emoji = "📱",
                    items = listOf(
                        "Menghilangkan kotak hitam dan double insets di atas keyboard saat mengetik pesan di Curhat AI.",
                        "Kolom input pesan dan tombol kirim otomatis terpasang presisi tepat di atas keyboard ponsel.",
                        "Layar Curhat AI kini beroperasi mandiri dari Profil tanpa tumpang tindih dengan navigasi bawah."
                    )
                ),
                ReleaseCategory(
                    title = "Peningkatan Sistem & Rilis",
                    emoji = "⚡",
                    items = listOf(
                        "Penanganan coroutine cancellation pada login Google agar tidak menimbulkan notifikasi error palsu.",
                        "Pembaruan versi rilis resmi menjadi v1.1.1 Beta."
                    )
                )
            )
        ),
        ReleaseNote(
            version = "1.1.0",
            releaseDate = "13 Oktober 2026",
            isLatest = false,
            title = "Pembaruan Besar: Ekspor Finansial, Smart OCR Nota & Catatan Rilis",
            highlights = listOf(
                "Ekspor Catatan Finansial lengkap ke PDF & Excel/CSV yang elegan dan rapi.",
                "Scan Nota Cerdas: Catat transaksi otomatis seketika dengan input tanggal nota.",
                "Tombol Scan Kamera & Galeri lebih bersih dengan ikon modern simetris.",
                "Catatan What's New terintegrasi dengan tombol Berbagi instan ke WhatsApp & medsos.",
                "Penyederhanaan menu profil & navigasi lebih intuitif tanpa duplikasi informasi."
            ),
            categories = listOf(
                ReleaseCategory(
                    title = "Fitur Baru",
                    emoji = "✨",
                    items = listOf(
                        "Ekspor Laporan PDF Profesional beresolusi tinggi lengkap dengan ringkasan pemasukan, pengeluaran, saldo bersih, dan tabel transaksi berwarna.",
                        "Ekspor File Excel/CSV berformat UTF-8 BOM, kompatibel penuh dengan Microsoft Excel, Google Sheets, & WPS Office.",
                        "Pencatatan nota instan: saat tombol 'Catat Transaksi' ditekan, transaksi langsung tercatat ke dompet tanpa kembali ke dialog sebelumnya.",
                        "Pemilihan tanggal transaksi nota langsung di layar pemindaian nota.",
                        "Catatan Pembaruan (What's New) dengan fitur Bagikan dan Salin Teks untuk memudahkan broadcast update aplikasi."
                    )
                ),
                ReleaseCategory(
                    title = "Tampilan & Antarmuka (UI/UX)",
                    emoji = "🎨",
                    items = listOf(
                        "Desain tombol Buka Kamera dan Dari Galeri diganti ikon minimalis berukuran seimbang 48dp.",
                        "Pembersihan informasi pada kartu dan dialog 'Tentang Aplikasi' agar padat, jelas, dan tanpa pengulangan teks.",
                        "Integrasi edit profil nama dan bio akun dalam satu jendela modal yang praktis.",
                        "Tata letak ringkasan arus kas bulanan lebih luas dan adaptif terhadap layar ponsel."
                    )
                ),
                ReleaseCategory(
                    title = "Kinerja & Keamanan",
                    emoji = "🛡️",
                    items = listOf(
                        "Sinkronisasi Cloud Firestore otomatis antar perangkat secara real-time.",
                        "Keamanan login akun Google terproteksi Credential Manager.",
                        "Optimalisasi alokasi pengeluaran berdasarkan panduan finansial OJK (50% Kebutuhan, 30% Keinginan, 20% Tabungan/Investasi)."
                    )
                )
            )
        ),
        ReleaseNote(
            version = "1.0.0",
            releaseDate = "01 Oktober 2026",
            isLatest = false,
            title = "Peluncuran Perdana: Pengelola Keuangan Pribadi Modern",
            highlights = listOf(
                "Pencatatan pemasukan & pengeluaran multi-dompet (Kas, Bank, E-Wallet).",
                "Analisis anggaran interaktif dan ringkasan arus kas.",
                "Dukungan Tema Terang, Gelap, dan mengikuti pengaturan sistem."
            ),
            categories = listOf(
                ReleaseCategory(
                    title = "Rilis Awal",
                    emoji = "🎉",
                    items = listOf(
                        "Pencatatan transaksi instan dengan pemilihan dompet dan kategori anggaran.",
                        "Target tabungan impian dengan pelacak progres visual.",
                        "Pemindaian struk berbasis AI/OCR kamera dan galeri foto.",
                        "Kustomisasi tampilan beranda untuk pengalaman minimalis (Clean Mode)."
                    )
                )
            )
        )
    )

    fun getFormattedShareText(note: ReleaseNote): String {
        val highlightsText = note.highlights.joinToString("\n") { "• $it" }
        val categoriesText = note.categories.joinToString("\n\n") { cat ->
            "${cat.emoji} *${cat.title}*\n" + cat.items.joinToString("\n") { "  - $it" }
        }

        return """
📢 *WHAT'S NEW - CATATAN PEMBARUAN APLIKASI*
📱 Versi: v${note.version} ${if (note.isLatest) "(Terbaru)" else ""}
📅 Tanggal: ${note.releaseDate}
🔖 ${note.title}

━━━━━━━━━━━━━━━━━━━━
🌟 *HIGHLIGHT PEMBARUAN:*
$highlightsText

━━━━━━━━━━━━━━━━━━━━
$categoriesText

━━━━━━━━━━━━━━━━━━━━
💡 _Catat pengeluaran lebih disiplin dan wujudkan tujuan finansial Anda bersama kami!_
        """.trimIndent()
    }
}
