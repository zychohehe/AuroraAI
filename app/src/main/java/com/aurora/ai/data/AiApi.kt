package com.aurora.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Klien untuk endpoint: https://api.ikyyxd.my.id/ai/unliai?teks=...
 * Respons: { "status": true, "result": { "query": "...", "response": "..." } }
 */
object AiApi {
    private const val BASE_URL = "https://api.ikyyxd.my.id/ai/unliai"

    // Petunjuk bahasa supaya AI membalas sesuai bahasa pengguna.
    // Kosongkan string ini kalau ingin mengirim teks pengguna apa adanya.
    private const val LANGUAGE_HINT =
        "Balas dalam bahasa yang sama dengan pesan pengguna (default Bahasa Indonesia). Pesan: "

    suspend fun ask(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(LANGUAGE_HINT + prompt, "UTF-8")
            val conn = URL("$BASE_URL?teks=$q").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 20_000
                conn.readTimeout = 60_000
                conn.setRequestProperty("Accept", "application/json")

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Server membalas HTTP $code")

                val json = JSONObject(body)
                if (!json.optBoolean("status", false)) error("API mengembalikan status gagal")
                json.getJSONObject("result").getString("response")
            } finally {
                conn.disconnect()
            }
        }
    }
}
