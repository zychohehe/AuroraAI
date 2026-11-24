package com.aurora.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.ai.data.AiApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class Msg(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val isError: Boolean = false
)

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<Msg>()

    var loading by mutableStateOf(false)
        private set

    private var nextId = 0L
    private var job: Job? = null

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || loading) return

        messages.add(Msg(nextId++, t, fromUser = true))
        loading = true

        job = viewModelScope.launch {
            val result = AiApi.ask(t)
            messages.add(
                result.fold(
                    onSuccess = { Msg(nextId++, it.trim(), fromUser = false) },
                    onFailure = {
                        Msg(
                            nextId++,
                            "Gagal terhubung: ${it.message ?: "kesalahan tidak diketahui"}",
                            fromUser = false,
                            isError = true
                        )
                    }
                )
            )
            loading = false
        }
    }

    fun clear() {
        job?.cancel()
        loading = false
        messages.clear()
    }
}
