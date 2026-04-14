package com.assistant.app.data

import android.content.Context
import android.content.SharedPreferences
import com.assistant.app.ai.AIMode
import com.assistant.app.utils.Constants

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)

    // ── API Keys (separate per provider) ──

    var nvidiaApiKey: String
        get() = prefs.getString(Constants.PREF_NVIDIA_API_KEY, "") ?: ""
        set(v) = prefs.edit().putString(Constants.PREF_NVIDIA_API_KEY, v).apply()

    var geminiApiKey: String
        get() = prefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
        set(v) = prefs.edit().putString(Constants.PREF_GEMINI_API_KEY, v).apply()

    var customApiKey: String
        get() = prefs.getString(Constants.PREF_CUSTOM_API_KEY, "") ?: ""
        set(v) = prefs.edit().putString(Constants.PREF_CUSTOM_API_KEY, v).apply()

    // ── Active Provider ──

    var provider: String
        get() = prefs.getString(Constants.PREF_PROVIDER, Constants.DEFAULT_PROVIDER)
            ?: Constants.DEFAULT_PROVIDER
        set(v) = prefs.edit().putString(Constants.PREF_PROVIDER, v).apply()

    // ── Per-Provider Models ──

    var nvidiaModel: String
        get() = prefs.getString(Constants.PREF_NVIDIA_MODEL, Constants.MODEL_LLAMA_8B)
            ?: Constants.MODEL_LLAMA_8B
        set(v) = prefs.edit().putString(Constants.PREF_NVIDIA_MODEL, v).apply()

    var geminiModel: String
        get() = prefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL)
            ?: Constants.DEFAULT_GEMINI_MODEL
        set(v) = prefs.edit().putString(Constants.PREF_GEMINI_MODEL, v).apply()

    // ── Custom Endpoint ──

    var customApiUrl: String
        get() = prefs.getString(Constants.PREF_CUSTOM_URL, "") ?: ""
        set(v) = prefs.edit().putString(Constants.PREF_CUSTOM_URL, v).apply()

    var customModelName: String
        get() = prefs.getString(Constants.PREF_CUSTOM_MODEL, "") ?: ""
        set(v) = prefs.edit().putString(Constants.PREF_CUSTOM_MODEL, v).apply()

    // ── Active API Key based on provider ──

    fun getActiveApiKey(): String = when (provider) {
        Constants.PROVIDER_NVIDIA -> nvidiaApiKey
        Constants.PROVIDER_GEMINI -> geminiApiKey
        Constants.PROVIDER_CUSTOM -> customApiKey
        else -> nvidiaApiKey
    }

    fun hasActiveApiKey(): Boolean = getActiveApiKey().isNotBlank()
    fun hasApiKey(): Boolean = hasActiveApiKey()

    // ── Active model based on provider ──

    fun getActiveModel(): String = when (provider) {
        Constants.PROVIDER_NVIDIA -> nvidiaModel
        Constants.PROVIDER_GEMINI -> geminiModel
        Constants.PROVIDER_CUSTOM -> customModelName.ifBlank { Constants.MODEL_LLAMA_8B }
        else -> nvidiaModel
    }

    // ── Active base URL ──

    fun getActiveBaseUrl(): String = when (provider) {
        Constants.PROVIDER_NVIDIA -> Constants.URL_NVIDIA
        Constants.PROVIDER_GEMINI -> Constants.URL_GEMINI_BASE
        Constants.PROVIDER_CUSTOM -> customApiUrl.ifBlank { Constants.URL_NVIDIA }
        else -> Constants.URL_NVIDIA
    }

    // ── Display names ──

    fun getProviderDisplayName(): String = when (provider) {
        Constants.PROVIDER_NVIDIA -> "NVIDIA"
        Constants.PROVIDER_GEMINI -> "Gemini"
        Constants.PROVIDER_CUSTOM -> "Custom"
        else -> "NVIDIA"
    }

    fun getModelDisplayName(): String =
        getActiveModel().substringAfterLast("/").take(22)

    // ── Other settings ──

    var overlayTransparency: Float
        get() = prefs.getFloat(Constants.PREF_TRANSPARENCY, Constants.DEFAULT_TRANSPARENCY)
        set(v) = prefs.edit().putFloat(Constants.PREF_TRANSPARENCY, v).apply()

    var textSize: Float
        get() = prefs.getFloat(Constants.PREF_TEXT_SIZE, 14f)
        set(v) = prefs.edit().putFloat(Constants.PREF_TEXT_SIZE, v).apply()

    var aiMode: AIMode
        get() = AIMode.from(
            prefs.getString(Constants.PREF_AI_MODE, AIMode.INTERVIEW_FULL.name) ?: ""
        )
        set(v) = prefs.edit().putString(Constants.PREF_AI_MODE, v.name).apply()

    var customPrompt: String
        get() = prefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
        set(v) = prefs.edit().putString(Constants.PREF_CUSTOM_PROMPT, v).apply()

    var useCustomPrompt: Boolean
        get() = prefs.getBoolean(Constants.PREF_USE_CUSTOM_PROMPT, false)
        set(v) = prefs.edit().putBoolean(Constants.PREF_USE_CUSTOM_PROMPT, v).apply()

    var autoListen: Boolean
        get() = prefs.getBoolean(Constants.PREF_AUTO_LISTEN, false)
        set(v) = prefs.edit().putBoolean(Constants.PREF_AUTO_LISTEN, v).apply()

    var darkText: Boolean
        get() = prefs.getBoolean(Constants.PREF_DARK_TEXT, false)
        set(v) = prefs.edit().putBoolean(Constants.PREF_DARK_TEXT, v).apply()

    fun clear() = prefs.edit().clear().apply()
}