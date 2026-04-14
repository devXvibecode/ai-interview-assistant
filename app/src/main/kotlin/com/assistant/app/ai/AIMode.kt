package com.assistant.app.ai

enum class AIMode(val displayName: String, val description: String, val emoji: String) {
    INTERVIEW_FULL("IT Interview Expert", "Microsoft 365, Intune, Security", "🎯"),
    CUSTOM("Custom Mode", "Your own system prompt", "✏️"),
    GENERAL("General Assistant", "Open-ended AI conversation", "💬"),
    MEETING("Meeting Assistant", "Tracks points and action items", "📋"),
    LEARNING("Learning Mode", "Explains concepts clearly", "📚");

    companion object {
        fun from(value: String): AIMode =
            entries.find { it.name == value } ?: INTERVIEW_FULL
    }
}