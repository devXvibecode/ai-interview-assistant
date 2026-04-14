package com.assistant.app.ai

import com.assistant.app.utils.Constants
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class NvidiaApiClient(
    private var apiKey: String,
    private var baseUrl: String = Constants.URL_NVIDIA
) {
    private val gson = Gson()
    private val mediaType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(90L, TimeUnit.SECONDS)
        .writeTimeout(Constants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun updateKey(key: String) { apiKey = key }
    fun updateBaseUrl(url: String) { baseUrl = url.trimEnd('/') }

    suspend fun sendStreaming(
        messages: List<ChatMessage>,
        model: String,
        onChunk: (String) -> Unit,
        onDone: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                withContext(Dispatchers.Main) {
                    onError("API key not configured. Go to Settings.")
                }
                return@withContext
            }

            val body = NvidiaRequest(
                model = model,
                messages = messages.map { it.toApiMsg() },
                temperature = Constants.DEFAULT_TEMPERATURE,
                maxTokens = Constants.MAX_TOKENS,
                stream = true
            )

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(gson.toJson(body).toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                val errMsg = runCatching {
                    gson.fromJson(errBody, NvidiaError::class.java)
                        .error?.message ?: "HTTP ${response.code}"
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
                if (data == "[DONE]") break
                runCatching {
                    val chunk = gson.fromJson(data, NvidiaResponse::class.java)
                    val text = chunk.choices?.firstOrNull()?.delta?.content
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

    fun testConnection(model: String, callback: (Boolean, String) -> Unit) {
        if (apiKey.isBlank()) {
            callback(false, "No API key entered")
            return
        }

        val body = NvidiaRequest(
            model = model,
            messages = listOf(ApiMsg("user", "Say OK")),
            maxTokens = 5,
            stream = false
        )

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody(mediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(false, "Failed: ${e.message}")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
                if (response.isSuccessful)
                    callback(true, "✅ Connected to ${model.substringAfterLast("/")}")
                else
                    callback(false, "❌ Error ${response.code}")
            }
        })
    }
}