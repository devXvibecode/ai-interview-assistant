package com.assistant.app.ai

import com.assistant.app.utils.newId

data class ChatMessage(
    val id: String = newId(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

enum class MessageRole(val value: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    companion object {
        fun from(value: String): MessageRole =
            entries.find { it.value == value } ?: USER
    }
}