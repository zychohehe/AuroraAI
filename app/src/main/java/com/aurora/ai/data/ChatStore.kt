package com.aurora.ai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Penyimpanan lokal sederhana (SharedPreferences + JSON): riwayat obrolan & pengaturan. */
class ChatStore(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("aurora_store", Context.MODE_PRIVATE)

    var useContext: Boolean
        get() = sp.getBoolean("use_context", true)
        set(v) {
            sp.edit().putBoolean("use_context", v).apply()
        }

    var indoReply: Boolean
        get() = sp.getBoolean("indo_reply", true)
        set(v) {
            sp.edit().putBoolean("indo_reply", v).apply()
        }

    fun load(): List<Conv> = runCatching {
        val arr = JSONArray(sp.getString("convs", "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val ms = o.getJSONArray("m")
            Conv(
                id = o.getString("id"),
                title = o.getString("t"),
                updatedAt = o.getLong("u"),
                messages = List(ms.length()) { j ->
                    val m = ms.getJSONObject(j)
                    Msg(
                        id = m.getLong("i"),
                        text = m.getString("x"),
                        fromUser = m.getBoolean("f"),
                        isError = m.optBoolean("e", false)
                    )
                }
            )
        }
    }.getOrDefault(emptyList())

    fun save(list: List<Conv>) {
        val arr = JSONArray()
        list.take(MAX_CONVERSATIONS).forEach { c ->
            val ms = JSONArray()
            c.messages.forEach { m ->
                ms.put(
                    JSONObject()
                        .put("i", m.id)
                        .put("x", m.text)
                        .put("f", m.fromUser)
                        .put("e", m.isError)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("t", c.title)
                    .put("u", c.updatedAt)
                    .put("m", ms)
            )
        }
        sp.edit().putString("convs", arr.toString()).apply()
    }

    private companion object {
        const val MAX_CONVERSATIONS = 50
    }
}
