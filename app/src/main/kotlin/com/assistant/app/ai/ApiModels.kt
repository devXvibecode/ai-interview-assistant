package com.assistant.app.ai

import com.google.gson.annotations.SerializedName

// ── Request ──────────────────────────────────────────────

data class NvidiaRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ApiMsg>,
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("top_p") val topP: Float = 0.9f,
    @SerializedName("stream") val stream: Boolean = false
)

data class ApiMsg(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

fun ChatMessage.toApiMsg() = ApiMsg(role = role.value, content = content)

// ── Response ─────────────────────────────────────────────

data class NvidiaResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("usage") val usage: Usage?
)

data class Choice(
    @SerializedName("index") val index: Int?,
    @SerializedName("message") val message: RespMsg?,
    @SerializedName("delta") val delta: DeltaMsg?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class RespMsg(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?
)

data class DeltaMsg(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)

data class NvidiaError(
    @SerializedName("error") val error: ErrorDetail?
)

data class ErrorDetail(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?
)