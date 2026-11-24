package com.aurora.ai.data

data class Msg(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val isError: Boolean = false
)

data class Conv(
    val id: String,
    val title: String,
    val messages: List<Msg>,
    val updatedAt: Long
)
