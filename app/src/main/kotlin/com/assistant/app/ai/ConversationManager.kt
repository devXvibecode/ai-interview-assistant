package com.assistant.app.ai

import com.assistant.app.data.AppPreferences
import com.assistant.app.utils.Constants
import com.assistant.app.utils.SpeechCorrector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList

class ConversationManager(private val preferences: AppPreferences) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentMode = MutableStateFlow(preferences.aiMode)
    val currentMode: StateFlow<AIMode> = _currentMode.asStateFlow()

    private val _speakerStatus = MutableStateFlow("")
    val speakerStatus: StateFlow<String> = _speakerStatus.asStateFlow()

    private val _correctionNotice = MutableStateFlow<String?>(null)
    val correctionNotice: StateFlow<String?> = _correctionNotice.asStateFlow()

    private val allMessages = mutableListOf<ChatMessage>()

    // Both clients — initialized based on provider
    private var nvidiaClient: NvidiaApiClient =
        NvidiaApiClient(preferences.nvidiaApiKey, Constants.URL_NVIDIA)

    private var geminiClient: GeminiApiClient =
        GeminiApiClient(preferences.geminiApiKey)

    private val scope = CoroutineScope(Dispatchers.Main)
    private val pendingQueue = LinkedList<String>()

    // Speaker ID state
    private var isCalibrated = false
    private var userVolumeBaseline = 0f
    private val calibrationReadings = mutableListOf<Float>()
    private var lastProcessedWasQuestion = false
    private var consecutiveSkips = 0
    private val volumeThresholdFactor = 0.65f

    private val questionWords = listOf(
        "what", "how", "why", "when", "where", "who", "which",
        "can you", "could you", "would you", "will you",
        "is it", "is there", "are there", "are you",
        "do you", "does it", "did you", "have you",
        "tell me", "explain", "describe", "define", "elaborate",
        "should i", "shall we", "may i", "difference between",
        "compare", "walk me through", "give me", "show me",
        "what's", "how's", "talk about", "know about",
        "familiar with", "experience with", "worked with"
    )

    private val domainKeywords = listOf(
        "microsoft", "m365", "office 365", "o365",
        "exchange", "sharepoint", "onedrive", "teams", "outlook",
        "entra", "azure", "conditional access", "mfa", "authentication",
        "sso", "identity", "pim", "intune", "endpoint", "mdm", "mam",
        "autopilot", "company portal", "enrollment", "compliance",
        "active directory", "group policy", "gpo", "domain",
        "dns", "spf", "dkim", "dmarc", "mx", "mail flow",
        "anti-spam", "anti-phishing", "safe links", "safe attachments",
        "defender", "zero trust", "security", "dlp", "sentinel",
        "bitlocker", "filevault", "encryption", "certificate",
        "windows", "macos", "ios", "android", "jamf", "dep", "ade",
        "zscaler", "zia", "zpa", "ztna", "vpn", "firewall",
        "troubleshoot", "error", "issue", "configure", "deploy",
        "license", "e3", "e5", "tenant", "admin", "rbac", "role",
        "cloud", "hybrid", "on-premises", "federation"
    )

    init {
        addSystemMessage()
    }

    // ── Client Management ──

    fun refreshApiClient() {
        nvidiaClient.updateKey(preferences.nvidiaApiKey)
        geminiClient.updateKey(preferences.geminiApiKey)
    }

    fun updateProvider() {
        refreshApiClient()
    }

    // Legacy compatibility
    fun updateApiKey(key: String) {
        refreshApiClient()
    }

    private fun isGeminiActive(): Boolean =
        preferences.provider == Constants.PROVIDER_GEMINI

    private fun isCustomActive(): Boolean =
        preferences.provider == Constants.PROVIDER_CUSTOM

    // ── System Message ──

    private fun addSystemMessage() {
        allMessages.clear()
        val prompt = if (preferences.useCustomPrompt && preferences.customPrompt.isNotBlank()) {
            SystemPrompts.get(AIMode.CUSTOM, preferences.customPrompt)
        } else {
            SystemPrompts.get(_currentMode.value)
        }
        allMessages.add(ChatMessage(role = MessageRole.SYSTEM, content = prompt))
        pushVisible()
    }

    private fun pushVisible() {
        _messages.value = allMessages
            .filter { it.role != MessageRole.SYSTEM }
            .toList()
    }

    // ── Calibration ──

    fun isCalibrationDone(): Boolean = isCalibrated

    fun addCalibrationReading(rmsDb: Float) {
        if (!isCalibrated && rmsDb > 1f) calibrationReadings.add(rmsDb)
    }

    fun finishCalibration() {
        userVolumeBaseline = if (calibrationReadings.size >= 3)
            calibrationReadings.average().toFloat() else 6f
        isCalibrated = true
        _speakerStatus.value = "✅ Calibrated"
    }

    fun skipCalibration() {
        userVolumeBaseline = 6f
        isCalibrated = true
        _speakerStatus.value = "⏭ Skipped"
    }

    // ── Speaker ID ──

    private fun identifySpeaker(text: String, avgVolume: Float): Speaker {
        if (!isCalibrated || userVolumeBaseline <= 0f) return Speaker.UNKNOWN
        val lower = text.lowercase().trim()
        val wordCount = lower.split("\\s+".toRegex()).size
        val threshold = userVolumeBaseline * volumeThresholdFactor
        val isQuestion = isQuestionLike(lower)
        val hasDomain = domainKeywords.any { lower.contains(it) }
        val isLong = wordCount > 15

        var iScore = 0; var uScore = 0
        if (avgVolume in 0.1f..threshold) iScore += 3
        if (avgVolume > threshold) uScore += 3
        if (isQuestion) iScore += 2
        if (hasDomain && isQuestion) iScore += 2
        if (isLong && !isQuestion) uScore += 2
        if (lastProcessedWasQuestion && !isQuestion) uScore += 1
        if (!lastProcessedWasQuestion && isQuestion) iScore += 1

        if (avgVolume <= 0.5f)
            return if (isQuestion || hasDomain) Speaker.INTERVIEWER else Speaker.UNKNOWN

        return when {
            iScore > uScore + 1 -> Speaker.INTERVIEWER
            uScore > iScore + 1 -> Speaker.USER
            isQuestion || hasDomain -> Speaker.INTERVIEWER
            else -> Speaker.UNKNOWN
        }
    }

    // ── Speech Detection ──

    fun onSpeechDetected(rawText: String, avgVolume: Float) {
        if (rawText.isBlank()) return

        val (corrected, wasCorrected) = SpeechCorrector.correctAndLog(rawText)
        val cleaned = corrected.trim().replace(
            Regex(
                "^(um+|uh+|hmm+|ah+|oh+|okay so|so+|well|like|you know)\\s+",
                RegexOption.IGNORE_CASE
            ), ""
        ).trim()

        if (cleaned.isBlank()) return

        if (wasCorrected) {
            _correctionNotice.value =
                "🔧 \"${rawText.take(25)}\" → \"${corrected.take(25)}\""
            scope.launch {
                kotlinx.coroutines.delay(3000)
                _correctionNotice.value = null
            }
        }

        val speaker = identifySpeaker(cleaned, avgVolume)

        when (speaker) {
            Speaker.USER -> {
                lastProcessedWasQuestion = false
                consecutiveSkips++
                _speakerStatus.value = "🔵 You"
                allMessages.add(
                    ChatMessage(role = MessageRole.USER, content = "🔵 [You]: $cleaned")
                )
                pushVisible()
            }
            Speaker.INTERVIEWER -> {
                consecutiveSkips = 0
                _speakerStatus.value = "🟡 Interviewer"
                allMessages.add(ChatMessage(role = MessageRole.USER, content = "🎤 $cleaned"))
                pushVisible()
                if (shouldProcess(cleaned)) {
                    lastProcessedWasQuestion = true
                    if (_isProcessing.value) pendingQueue.add(cleaned)
                    else processWithAI(cleaned)
                }
            }
            Speaker.UNKNOWN -> {
                _speakerStatus.value = "⚪"
                allMessages.add(ChatMessage(role = MessageRole.USER, content = "🎤 $cleaned"))
                pushVisible()
                if (shouldProcess(cleaned)) {
                    lastProcessedWasQuestion = true
                    if (_isProcessing.value) pendingQueue.add(cleaned)
                    else processWithAI(cleaned)
                } else {
                    consecutiveSkips++
                    if (consecutiveSkips > 3 && !_isProcessing.value) {
                        consecutiveSkips = 0
                        processWithAI(cleaned)
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isProcessing.value) return
        val corrected = SpeechCorrector.correct(text)
        allMessages.add(ChatMessage(role = MessageRole.USER, content = corrected))
        pushVisible()
        processWithAI(corrected)
    }

    // ── Question Detection ──

    private fun isQuestionLike(lower: String): Boolean {
        if (lower.endsWith("?")) return true
        if (questionWords.any { lower.startsWith(it) || lower.contains(" $it ") }) return true
        return listOf(
            "tell me about", "walk me through", "can you explain",
            "what do you know", "how would you", "have you ever",
            "give me an example", "what experience", "describe a time",
            "what is your", "how do you", "what are the",
            "how familiar", "have you worked", "describe how"
        ).any { lower.contains(it) }
    }

    private fun shouldProcess(text: String): Boolean {
        val lower = text.lowercase().trim()
        val wordCount = lower.split("\\s+".toRegex()).size
        if (wordCount < 2) return domainKeywords.any { lower.contains(it) }
        if (domainKeywords.any { lower.contains(it) }) return true
        if (lower.endsWith("?")) return true
        if (isQuestionLike(lower)) return true
        if (wordCount >= 4) return true
        return false
    }

    // ── AI Processing — Route to correct client ──

    private fun processWithAI(text: String) {
        _isProcessing.value = true
        _speakerStatus.value = "🧠 Thinking..."

        val context = allMessages.takeLast(Constants.MAX_CONTEXT_MESSAGES).toList()

        allMessages.add(
            ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
        )
        pushVisible()

        when {
            isGeminiActive() -> processWithGemini(context)
            isCustomActive() -> processWithNvidia(context, preferences.customModelName)
            else -> processWithNvidia(context, preferences.nvidiaModel)
        }
    }

    private fun processWithNvidia(context: List<ChatMessage>, model: String) {
        scope.launch {
            val effectiveUrl = if (isCustomActive())
                preferences.customApiUrl.ifBlank { Constants.URL_NVIDIA }
            else Constants.URL_NVIDIA

            val effectiveKey = if (isCustomActive())
                preferences.customApiKey
            else preferences.nvidiaApiKey

            val client = NvidiaApiClient(effectiveKey, effectiveUrl)

            client.sendStreaming(
                messages = context,
                model = model.ifBlank { Constants.DEFAULT_MODEL },
                onChunk = { chunk -> updateStreamingMessage(chunk) },
                onDone = { full -> finalizeMessage(full) },
                onError = { err -> handleError(err) }
            )
        }
    }

    private fun processWithGemini(context: List<ChatMessage>) {
        scope.launch {
            geminiClient.sendStreaming(
                messages = context,
                model = preferences.geminiModel,
                onChunk = { chunk -> updateStreamingMessage(chunk) },
                onDone = { full -> finalizeMessage(full) },
                onError = { err -> handleError(err) }
            )
        }
    }

    private fun updateStreamingMessage(chunk: String) {
        val idx = allMessages.indexOfLast { it.isStreaming }
        if (idx >= 0) {
            allMessages[idx] = allMessages[idx].copy(
                content = allMessages[idx].content + chunk
            )
            pushVisible()
        }
    }

    private fun finalizeMessage(full: String) {
        val idx = allMessages.indexOfLast { it.isStreaming }
        if (idx >= 0) {
            allMessages[idx] = allMessages[idx].copy(
                content = full,
                isStreaming = false
            )
            pushVisible()
        }
        _isProcessing.value = false
        _speakerStatus.value = "✅ Ready"
        processNextInQueue()
    }

    private fun handleError(err: String) {
        val idx = allMessages.indexOfLast { it.isStreaming }
        if (idx >= 0) {
            allMessages[idx] = allMessages[idx].copy(
                content = "⚠️ $err",
                isStreaming = false,
                isError = true
            )
            pushVisible()
        }
        _isProcessing.value = false
        _speakerStatus.value = "⚠️ Error"
        processNextInQueue()
    }

    private fun processNextInQueue() {
        val next = pendingQueue.poll() ?: return
        if (next.isNotBlank()) {
            allMessages.add(ChatMessage(role = MessageRole.USER, content = "🎤 $next"))
            pushVisible()
            processWithAI(next)
        }
    }

    // ── Export ──

    fun exportConversation(): String {
        val sb = StringBuilder()
        sb.appendLine("╔══════════════════════════════════════════╗")
        sb.appendLine("║     AI Assistant — Session Export        ║")
        sb.appendLine("╚══════════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("Mode    : ${_currentMode.value.displayName}")
        sb.appendLine("Provider: ${preferences.getProviderDisplayName()}")
        sb.appendLine("Model   : ${preferences.getModelDisplayName()}")
        sb.appendLine("Dev     : ${Constants.DEV_NAME} | ${Constants.DEV_GITHUB}")
        sb.appendLine("─".repeat(44))
        sb.appendLine()
        allMessages.filter { it.role != MessageRole.SYSTEM }.forEach { msg ->
            val label = if (msg.role == MessageRole.USER) "HEARD" else "AI   "
            sb.appendLine("[$label] ${msg.content}")
            sb.appendLine()
        }
        sb.appendLine("─".repeat(44))
        sb.appendLine("Built by ${Constants.DEV_NAME} • ${Constants.DEV_EMAIL}")
        return sb.toString()
    }

    fun reloadPrompt() { addSystemMessage() }

    fun clearChat() {
        pendingQueue.clear()
        allMessages.clear()
        isCalibrated = false
        calibrationReadings.clear()
        userVolumeBaseline = 0f
        lastProcessedWasQuestion = false
        consecutiveSkips = 0
        _speakerStatus.value = ""
        _correctionNotice.value = null
        addSystemMessage()
    }

    fun changeMode(mode: AIMode) {
        preferences.aiMode = mode
        _currentMode.value = mode
        clearChat()
    }
}

enum class Speaker { INTERVIEWER, USER, UNKNOWN }