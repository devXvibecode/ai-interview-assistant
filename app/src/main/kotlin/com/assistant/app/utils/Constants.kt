package com.assistant.app.utils

object Constants {

    // ── Branding ──
    const val APP_NAME = "AI Assistant"
    const val DEV_NAME = "vibecode"
    const val DEV_EMAIL = "dev.vibecode@proton.me"
    const val DEV_GITHUB = "github.com/devXvibecode"
    const val APP_VERSION = "1.0.0"

    // ── Notification ──
    const val NOTIFICATION_CHANNEL_OVERLAY = "overlay_channel"
    const val OVERLAY_NOTIFICATION_ID = 1001

    // ── Overlay ──
    const val OVERLAY_WIDTH_RATIO = 0.90f
    const val OVERLAY_HEIGHT_RATIO = 0.50f
    const val BUBBLE_SIZE_DP = 58
    const val DEFAULT_TRANSPARENCY = 0.93f

    // ── NVIDIA Models — Fast ──
    const val MODEL_LLAMA_8B = "meta/llama-3.1-8b-instruct"
    const val MODEL_LLAMA_33_70B = "meta/llama-3.3-70b-instruct"
    const val MODEL_MISTRAL_7B = "mistralai/mistral-7b-instruct-v0.3"
    const val MODEL_MISTRAL_NEMO = "mistralai/mistral-nemo-12b-instruct"

    // ── NVIDIA Models — Powerful ──
    const val MODEL_LLAMA_70B = "meta/llama-3.1-70b-instruct"
    const val MODEL_LLAMA_405B = "meta/llama-3.1-405b-instruct"
    const val MODEL_MIXTRAL = "mistralai/mixtral-8x22b-instruct-v0.1"
    const val MODEL_DEEPSEEK = "deepseek-ai/deepseek-r1"

    const val DEFAULT_MODEL = MODEL_LLAMA_8B

    // ── Gemini Models (Google AI — Free) ──
    const val GEMINI_FLASH_2 = "gemini-2.0-flash"
    const val GEMINI_FLASH_15 = "gemini-1.5-flash"
    const val GEMINI_FLASH_8B = "gemini-1.5-flash-8b"
    const val GEMINI_PRO_15 = "gemini-1.5-pro"
    const val DEFAULT_GEMINI_MODEL = GEMINI_FLASH_2

    // ── Providers ──
    const val PROVIDER_NVIDIA = "nvidia"
    const val PROVIDER_GEMINI = "gemini"
    const val PROVIDER_CUSTOM = "custom"
    const val DEFAULT_PROVIDER = PROVIDER_NVIDIA

    // ── URLs ──
    const val URL_NVIDIA = "https://integrate.api.nvidia.com/v1"
    const val URL_GEMINI_BASE = "https://generativelanguage.googleapis.com"

    // ── AI Settings ──
    const val MAX_CONTEXT_MESSAGES = 16
    const val MAX_TOKENS = 900
    const val DEFAULT_TEMPERATURE = 0.35f
    const val REQUEST_TIMEOUT_SECONDS = 25L

    // ── Speech ──
    const val SPEECH_LANGUAGE = "en-US"
    const val SILENCE_TIMEOUT_MS = 1800L
    const val BUFFER_FLUSH_DELAY_MS = 1600L
    const val MIN_WORDS_TO_PROCESS = 2
    const val RESTART_DELAY_MS = 80L
    const val MAX_RESTARTS_BEFORE_CLEANUP = 20
    const val PARTIAL_DEBOUNCE_MS = 100L

    // ── Preference Keys ──
    const val PREF_NVIDIA_API_KEY = "nvidia_api_key_v2"
    const val PREF_GEMINI_API_KEY = "gemini_api_key"
    const val PREF_CUSTOM_API_KEY = "custom_api_key"
    const val PREF_PROVIDER = "api_provider"
    const val PREF_NVIDIA_MODEL = "nvidia_model"
    const val PREF_GEMINI_MODEL = "gemini_model"
    const val PREF_CUSTOM_URL = "custom_api_url"
    const val PREF_CUSTOM_MODEL = "custom_model_name"
    const val PREF_TRANSPARENCY = "overlay_transparency"
    const val PREF_TEXT_SIZE = "text_size"
    const val PREF_AI_MODE = "ai_mode"
    const val PREF_CUSTOM_PROMPT = "custom_system_prompt"
    const val PREF_USE_CUSTOM_PROMPT = "use_custom_prompt"
    const val PREF_AUTO_LISTEN = "auto_listen"
    const val PREF_DARK_TEXT = "dark_text_mode"

    // ── Actions ──
    const val ACTION_STOP_OVERLAY = "com.assistant.app.STOP_OVERLAY"
    const val ACTION_TOGGLE_MIC = "com.assistant.app.TOGGLE_MIC"
}