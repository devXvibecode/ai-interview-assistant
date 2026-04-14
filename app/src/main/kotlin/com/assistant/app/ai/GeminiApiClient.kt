package com.assistant.app.ai

import com.assistant.app.utils.Constants
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

// ── Gemini Request Models ──

data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<GeminiContent>,
    @SerializedName("systemInstruction")
    val systemInstruction: GeminiSystemInstruction? = null,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiContent(
    @SerializedName("role")
    val role: String,
    @SerializedName("parts")
    val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text")
    val text: String
)

data class GeminiSystemInstruction(
    @SerializedName("parts")
    val parts: List<GeminiPart>
)

data class GeminiGenerationConfig(
    @SerializedName("temperature")
    val temperature: Float = Constants.DEFAULT_TEMPERATURE,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = Constants.MAX_TOKENS,
    @SerializedName("topP")
    val topP: Float = 0.9f
)

// ── Gemini Response Models ──

data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<GeminiCandidate>?,
    @SerializedName("error")
    val error: GeminiError?
)

data class GeminiCandidate(
    @SerializedName("content")
    val content: GeminiContent?,
    @SerializedName("finishReason")
    val finishReason: String?
)

data class GeminiError(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
)

// ── Gemini Streaming Response ──

data class GeminiStreamChunk(
    @SerializedName("candidates")
    val candidates: List<GeminiCandidate>?
)

// ── Gemini API Client ──

class GeminiApiClient(private var apiKey: String) {

    private val gson = Gson()
    private val mediaType = "application/json".toMediaType()
    private val baseUrl = Constants.URL_GEMINI_BASE

    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(90L, TimeUnit.SECONDS)
        .writeTimeout(Constants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun updateKey(key: String) { apiKey = key }

    // ── Convert ChatMessages to Gemini format ──

    private fun buildGeminiRequest(messages: List<ChatMessage>): GeminiRequest {
        // Separate system message from conversation
        val systemMsg = messages.firstOrNull { it.role == MessageRole.SYSTEM }
        val conversationMsgs = messages.filter { it.role != MessageRole.SYSTEM }

        // Build contents — Gemini uses "user" and "model" roles
        val contents = mutableListOf<GeminiContent>()

        conversationMsgs.forEach { msg ->
            val geminiRole = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "model"
                MessageRole.SYSTEM -> "user" // fallback
            }
            contents.add(
                GeminiContent(
                    role = geminiRole,
                    parts = listOf(GeminiPart(text = msg.content))
                )
            )
        }

        // Gemini requires alternating user/model messages
        // If two consecutive messages have same role, merge them
        val mergedContents = mergeConsecutiveSameRole(contents)

        val systemInstruction = systemMsg?.let {
            GeminiSystemInstruction(parts = listOf(GeminiPart(text = it.content)))
        }

        return GeminiRequest(
            contents = mergedContents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig()
        )
    }

    private fun mergeConsecutiveSameRole(
        contents: List<GeminiContent>
    ): List<GeminiContent> {
        if (contents.isEmpty()) return contents

        val merged = mutableListOf<GeminiContent>()
        var current = contents[0]

        for (i in 1 until contents.size) {
            val next = contents[i]
            if (next.role == current.role) {
                // Merge parts
                current = GeminiContent(
                    role = current.role,
                    parts = current.parts + next.parts
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        // Gemini requires starting with "user" role
        // If first message is "model", prepend a dummy user message
        if (merged.isNotEmpty() && merged[0].role == "model") {
            merged.add(
                0,
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "Hello"))
                )
            )
        }

        return merged
    }

    // ── Streaming request ──

    suspend fun sendStreaming(
        messages: List<ChatMessage>,
        model: String = Constants.DEFAULT_GEMINI_MODEL,
        onChunk: (String) -> Unit,
        onDone: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                withContext(Dispatchers.Main) {
                    onError("Gemini API key not configured. Go to Settings.")
                }
                return@withContext
            }

            val requestBody = buildGeminiRequest(messages)
            val json = gson.toJson(requestBody)

            // Gemini streaming endpoint
            val url = "$baseUrl/v1beta/models/$model:streamGenerateContent?key=$apiKey&alt=sse"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(json.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                val errMsg = runCatching {
                    val errResp = gson.fromJson(errBody, GeminiResponse::class.java)
                    errResp.error?.message ?: "HTTP ${response.code}"
                }.getOrDefault("HTTP ${response.code}")
                withContext(Dispatchers.Main) { onError(errMsg) }
                return@withContext
            }

            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val raw = line ?: continue
                if (!raw.startsWith("data: ")) continue
                val data = raw.removePrefix("data: ").trim()
                if (data.isBlank()) continue

                runCatching {
                    val chunk = gson.fromJson(data, GeminiStreamChunk::class.java)
                    val text = chunk.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                        ?: return@runCatching

                    sb.append(text)
                    withContext(Dispatchers.Main) { onChunk(text) }
                }
            }
            reader.close()
            withContext(Dispatchers.Main) { onDone(sb.toString()) }

        }.onFailure { e ->
            withContext(Dispatchers.Main) { onError(e.message ?: "Connection error") }
        }
    }

    // ── Test connection ──

    fun testConnection(
        model: String = Constants.DEFAULT_GEMINI_MODEL,
        callback: (Boolean, String) -> Unit
    ) {
        if (apiKey.isBlank()) {
            callback(false, "No API key entered")
            return
        }

        val testRequest = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "Say OK"))
                )
            ),
            generationConfig = GeminiGenerationConfig(maxOutputTokens = 5)
        )

        val url = "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(testRequest).toRequestBody(mediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(false, "Connection failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                response.close()

                if (response.isSuccessful) {
                    callback(true, "✅ Connected to $model")
                } else {
                    val errMsg = runCatching {
                        gson.fromJson(body, GeminiResponse::class.java)
                            .error?.message ?: "Error ${response.code}"
                    }.getOrDefault("Error ${response.code}")
                    callback(false, "❌ $errMsg")
                }
            }
        })
    }
}