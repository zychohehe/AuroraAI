# ✦ Aurora AI

Aplikasi chat AI native Android (Kotlin + Jetpack Compose) bertema **Midnight & Champagne** — gelap pekat dengan aksen emas lembut, tipografi serif untuk judul, dan latar glow yang bergerak pelan.

API:

```
GET https://api.ikyyxd.my.id/ai/unliai?teks=<pesan>
→ { "status": true, "result": { "query": "...", "response": "..." } }
```

## Fitur

- **Menu geser (drawer)**: riwayat obrolan, obrolan baru, hapus per obrolan, *Bersihkan obrolan ini*, *Hapus semua obrolan*
- **Riwayat tersimpan** di perangkat (maks. 50 obrolan)
- **Tombol kirim berubah jadi tombol jeda** saat AI bekerja; menekannya menghentikan balasan dan menyimpan teks yang sudah tampil
- Efek mengetik (streaming), indikator mengetik
- **Render Markdown**: tebal, `kode`, blok kode (dengan tombol salin), daftar, judul
- Aksi per balasan: **Salin**, **Bagikan**, **Ulangi / Coba lagi**
- Teks balasan bisa diseleksi
- Opsi: *Ingat konteks obrolan* dan *Balas Bahasa Indonesia* (di drawer)

## Soal "gagal terhubung"

Klien HTTP sudah diperkuat:

- Memakai `User-Agent` ala browser (banyak server/WAF memblokir UA bawaan Android)
- Retry otomatis (3x) + timeout lebih longgar
- Jika permintaan dengan konteks gagal, otomatis dicoba lagi hanya dengan pesan terbaru
- Pesan error menampilkan **detail teknis** (mis. `HTTP 403`, `UnknownHostException`) supaya penyebabnya jelas

Kalau masih gagal, lihat baris `Detail:` di bawah pesan error dan sesuaikan `data/AiApi.kt`.

## Build lewat GitHub (tanpa Android Studio)

```bash
git init && git add . && git commit -m "Aurora AI v1.1"
git branch -M main
git remote add origin https://github.com/<username>/<repo>.git
git push -u origin main
```

Buka tab **Actions** → **Build APK** → unduh dari *Artifacts* (`AuroraAI-apk`).
Release otomatis: `git tag v1.1 && git push origin v1.1`.

## Struktur

```
app/src/main/java/com/aurora/ai/
├── MainActivity.kt
├── ChatViewModel.kt      state, kirim/hentikan/ulangi, kelola obrolan
├── data/
│   ├── AiApi.kt          klien HTTP + retry + fallback
│   ├── ChatStore.kt      penyimpanan riwayat & pengaturan
│   └── Models.kt
└── ui/
    ├── Theme.kt          palet & tipografi
    ├── ChatScreen.kt     UI utama, drawer, input bar
    └── Markdown.kt       parser & renderer Markdown ringan
```

## Kustomisasi

- Warna: `Gold`, `GoldSoft`, `Indigo`, `Ink` di `ui/Theme.kt`
- Petunjuk bahasa untuk AI: konstanta `HINT` di `data/AiApi.kt`
- APK release ditandatangani debug key agar bisa langsung diinstal; untuk Play Store ganti dengan keystore sendiri.
