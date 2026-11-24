# ✦ Aurora AI

Aplikasi chat AI native Android (Kotlin + Jetpack Compose) dengan tampilan **aurora glassmorphism**: latar gradien bergerak, bubble kaca, indikator mengetik, dan tombol salin.

API yang dipakai:

```
GET https://api.ikyyxd.my.id/ai/unliai?teks=<pesan>
→ { "status": true, "result": { "query": "...", "response": "..." } }
```

## Build lewat GitHub (tanpa Android Studio)

1. Buat repo baru di GitHub, lalu push seluruh isi folder ini:
   ```bash
   git init
   git add .
   git commit -m "Aurora AI"
   git branch -M main
   git remote add origin https://github.com/<username>/<repo>.git
   git push -u origin main
   ```
2. Buka tab **Actions** → workflow **Build APK** berjalan otomatis.
3. Setelah selesai, unduh APK di bagian **Artifacts** (`AuroraAI-apk`).
4. Untuk membuat Release otomatis: `git tag v1.0 && git push origin v1.0`.

## Build lokal

Buka di Android Studio (Koala atau lebih baru) lalu Run, atau:

```bash
gradle assembleDebug   # butuh Gradle 8.9+ dan JDK 17
```

## Struktur

```
app/src/main/java/com/aurora/ai/
├── MainActivity.kt        entry point (edge-to-edge)
├── ChatViewModel.kt       state chat
├── data/AiApi.kt          klien HTTP ke API
└── ui/
    ├── Theme.kt           palet & tipografi
    └── ChatScreen.kt      seluruh UI
```

## Kustomisasi cepat

- Warna: ubah `Violet`, `Pink`, `Cyan` di `ui/Theme.kt`.
- Nama app: `res/values/strings.xml`. Package: `com.aurora.ai`.
- Bahasa balasan: ubah/kosongkan `LANGUAGE_HINT` di `data/AiApi.kt`.
  API ini cenderung membalas dalam bahasa Mandarin kalau tidak diberi petunjuk bahasa.

## Catatan

- APK release ditandatangani dengan debug key agar bisa langsung di-install. Untuk Play Store, ganti dengan keystore sendiri.
- API pihak ketiga bisa berubah atau mati sewaktu-waktu.
