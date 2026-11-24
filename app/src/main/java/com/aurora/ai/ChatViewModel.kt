package com.aurora.ai

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.ai.data.AiApi
import com.aurora.ai.data.ChatStore
import com.aurora.ai.data.Conv
import com.aurora.ai.data.Msg
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ChatStore(app)

    /** Seluruh riwayat obrolan, terbaru di atas. */
    val conversations = mutableStateListOf<Conv>().apply {
        addAll(store.load().sortedByDescending { it.updatedAt })
    }

    /** null = layar "obrolan baru" (belum ada pesan). */
    var currentId by mutableStateOf<String?>(null)
        private set

    /** Menunggu balasan dari server. */
    var loading by mutableStateOf(false)
        private set

    /** Balasan sedang "diketik" (efek streaming). */
    var streaming by mutableStateOf(false)
        private set

    var streamText by mutableStateOf("")
        private set

    var useContext by mutableStateOf(store.useContext)
        private set

    var indoReply by mutableStateOf(store.indoReply)
        private set

    val busy: Boolean get() = loading || streaming

    val messages: List<Msg>
        get() = conversations.firstOrNull { it.id == currentId }?.messages ?: emptyList()

    private var job: Job? = null
    private var runId = 0
    private var idGen = System.currentTimeMillis()

    private fun nextId(): Long = idGen++

    // ───────────── Kirim / hentikan / ulangi ─────────────

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return

        val existing = currentId?.takeIf { cid -> conversations.any { it.id == cid } }
        val convId = existing ?: newConversation(t)
        val history = conversations.firstOrNull { it.id == convId }?.messages.orEmpty()

        addMsg(convId, Msg(nextId(), t, fromUser = true))
        generate(convId, t, history)
    }

    fun stop() {
        if (!busy) return
        val id = currentId
        val partial = streamText
        val wasStreaming = streaming
        cancelRun()
        // Simpan teks yang sudah sempat tampil, jadi tombol jeda terasa "menahan" balasan.
        if (wasStreaming && partial.isNotBlank() && id != null) {
            addMsg(id, Msg(nextId(), partial, fromUser = false))
        }
    }

    fun regenerate() {
        if (busy) return
        val id = currentId ?: return
        val conv = conversations.firstOrNull { it.id == id } ?: return
        val lastUser = conv.messages.indexOfLast { it.fromUser }
        if (lastUser < 0) return

        val prompt = conv.messages[lastUser].text
        val history = conv.messages.take(lastUser)
        updateConv(id) { it.copy(messages = it.messages.take(lastUser + 1)) }
        generate(id, prompt, history)
    }

    private fun generate(convId: String, prompt: String, history: List<Msg>) {
        val run = ++runId
        loading = true
        streaming = false
        streamText = ""

        job = viewModelScope.launch {
            var finalText: String? = null
            var isError = false
            try {
                val r = AiApi.ask(prompt, history, useContext, indoReply)
                val full = r.getOrNull()?.trim().orEmpty()
                if (r.isSuccess && full.isNotEmpty()) {
                    loading = false
                    streaming = true
                    reveal(full)
                    finalText = full
                } else {
                    isError = true
                    finalText = r.exceptionOrNull()?.let { AiApi.describe(it) }
                        ?: "Balasan kosong dari server."
                }
            } finally {
                // Jika run sudah diganti (stop/hapus/obrolan baru), pembersihan dilakukan di sana.
                if (run == runId) {
                    if (finalText == null && streamText.isNotEmpty()) finalText = streamText
                    finalText?.let { addMsg(convId, Msg(nextId(), it, fromUser = false, isError = isError)) }
                    loading = false
                    streaming = false
                    streamText = ""
                }
            }
        }
    }

    /** Efek mengetik: tampilkan teks sedikit demi sedikit. */
    private suspend fun reveal(text: String) {
        val step = (text.length / 150).coerceIn(2, 12)
        var i = 0
        while (i < text.length) {
            i = (i + step).coerceAtMost(text.length)
            if (i < text.length && Character.isHighSurrogate(text[i - 1])) i++
            streamText = text.substring(0, i)
            delay(14)
        }
    }

    private fun cancelRun() {
        runId++
        job?.cancel()
        job = null
        loading = false
        streaming = false
        streamText = ""
    }

    // ───────────── Kelola obrolan ─────────────

    fun newChat() {
        cancelRun()
        currentId = null
    }

    fun open(id: String) {
        if (id == currentId) return
        cancelRun()
        currentId = id
    }

    fun delete(id: String) {
        if (id == currentId) {
            cancelRun()
            currentId = null
        }
        conversations.removeAll { it.id == id }
        persist()
    }

    /** "Bersihkan obrolan ini": hapus seluruh pesan di obrolan aktif. */
    fun clearCurrent() {
        currentId?.let { delete(it) }
    }

    fun deleteAll() {
        cancelRun()
        currentId = null
        conversations.clear()
        persist()
    }

    fun changeUseContext(v: Boolean) {
        useContext = v
        store.useContext = v
    }

    fun changeIndoReply(v: Boolean) {
        indoReply = v
        store.indoReply = v
    }

    // ───────────── Helper data ─────────────

    private fun newConversation(firstMessage: String): String {
        val id = UUID.randomUUID().toString()
        conversations.add(0, Conv(id, titleFrom(firstMessage), emptyList(), System.currentTimeMillis()))
        currentId = id
        return id
    }

    private fun titleFrom(text: String): String {
        val s = text.replace('\n', ' ').trim()
        return if (s.length > 32) s.take(32).trimEnd() + "…" else s
    }

    private fun addMsg(convId: String, msg: Msg) {
        updateConv(convId) { it.copy(messages = it.messages + msg) }
    }

    private fun updateConv(id: String, transform: (Conv) -> Conv) {
        val idx = conversations.indexOfFirst { it.id == id }
        if (idx < 0) return
        val updated = transform(conversations[idx]).copy(updatedAt = System.currentTimeMillis())
        conversations.removeAt(idx)
        conversations.add(0, updated)
        persist()
    }

    private fun persist() {
        store.save(conversations.toList())
    }
}
