package com.aurora.ai.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Klien untuk endpoint: https://api.ikyyxd.my.id/ai/unliai?teks=...
 * Respons: { "status": true, "result": { "query": "...", "response": "..." } }
 *
 * Tahan banting: User-Agent ala browser (beberapa server/WAF memblokir UA bawaan Android),
 * retry otomatis, parsing longgar, dan fallback tanpa konteks kalau permintaan panjang gagal.
 */
object AiApi {
    private const val BASE_URL = "https://api.ikyyxd.my.id/ai/unliai"

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private const val HINT =
        "Jawablah dalam Bahasa Indonesia yang natural dan rapi (boleh pakai Markdown), " +
            "kecuali pengguna memakai bahasa lain.\n\n"

    private const val MAX_CHARS = 1800

    suspend fun ask(
        prompt: String,
        history: List<Msg>,
        useContext: Boolean,
        indoReply: Boolean
    ): Result<String> {
        if (useContext && history.any { !it.isError }) {
            // Percobaan 1: dengan konteks obrolan (sekali saja, tanpa retry).
            val withCtx = fetchWithRetry(buildWithContext(prompt, history, indoReply), attempts = 1)
            if (withCtx.isSuccess) return withCtx
        }
        // Percobaan 2: hanya pesan terbaru, dengan retry.
        val plain = (if (indoReply) HINT else "") + prompt
        return fetchWithRetry(plain, attempts = 2)
    }

    private fun buildWithContext(prompt: String, history: List<Msg>, indo: Boolean): String {
        val turns = history
            .filter { !it.isError }
            .takeLast(8)
            .map { (if (it.fromUser) "Pengguna: " else "AI: ") + it.text.take(400).replace('\n', ' ') }
            .toMutableList()

        fun compose(): String = buildString {
            if (indo) append(HINT)
            append("Berikut riwayat percakapan sebelumnya:\n")
            for (line in turns) append(line).append('\n')
            append("\nBalas pesan terbaru pengguna ini: ").append(prompt)
        }

        var out = compose()
        while (out.length > MAX_CHARS && turns.isNotEmpty()) {
            turns.removeAt(0)
            out = compose()
        }
        return out
    }

    private suspend fun fetchWithRetry(text: String, attempts: Int): Result<String> {
        var last: Throwable? = null
        for (attempt in 0 until attempts) {
            try {
                return Result.success(fetch(text))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
                if (attempt < attempts - 1) delay(900L * (attempt + 1))
            }
        }
        return Result.failure(last ?: IOException("Tidak diketahui"))
    }

    private suspend fun fetch(text: String): String = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(text, "UTF-8")
        val conn = URL("$BASE_URL?teks=$q").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 90_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en;q=0.8")

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Server membalas HTTP $code")
            parse(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): String {
        val t = body.trim()
        if (t.isEmpty()) throw IOException("Respons kosong dari server")
        if (t.startsWith("<")) throw IOException("Server mengirim halaman HTML (kemungkinan permintaan diblokir)")
        if (!t.startsWith("{")) return t

        val json = JSONObject(t)
        if (json.has("status") && !json.optBoolean("status", true)) {
            throw IOException(json.optString("message").ifBlank { "API mengembalikan status gagal" })
        }
        val result = json.opt("result")
        val text = when (result) {
            is JSONObject -> result.optString("response")
                .ifBlank { result.optString("text") }
                .ifBlank { result.optString("message") }
            is String -> result
            else -> json.optString("response")
        }
        if (text.isBlank()) throw IOException("Balasan kosong dari server")
        return text
    }

    /** Pesan error yang ramah + detail teknis untuk membantu diagnosa. */
    fun describe(e: Throwable): String {
        val friendly = when (e) {
            is UnknownHostException -> "Tidak ada koneksi internet, atau server tidak ditemukan."
            is SocketTimeoutException -> "Server terlalu lama membalas. Coba lagi sebentar lagi."
            is SSLException -> "Koneksi aman ke server gagal."
            is ConnectException -> "Tidak bisa menyambung ke server."
            else -> "Gagal mendapatkan balasan dari server."
        }
        val detail = "${e.javaClass.simpleName}: ${e.message ?: "-"}"
        return "$friendly\n\nDetail: $detail"
    }
}
