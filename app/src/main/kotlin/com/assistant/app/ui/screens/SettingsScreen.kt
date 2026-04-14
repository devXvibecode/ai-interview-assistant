package com.assistant.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assistant.app.ai.ConversationManager
import com.assistant.app.ai.GeminiApiClient
import com.assistant.app.ai.NvidiaApiClient
import com.assistant.app.data.AppPreferences
import com.assistant.app.ui.theme.*
import com.assistant.app.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    convManager: ConversationManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var nvidiaKey by remember { mutableStateOf(prefs.nvidiaApiKey) }
    var geminiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var customKey by remember { mutableStateOf(prefs.customApiKey) }

    var showNvidiaKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showCustomKey by remember { mutableStateOf(false) }

    var nvidiaTestResult by remember { mutableStateOf<String?>(null) }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }
    var customTestResult by remember { mutableStateOf<String?>(null) }
    var nviaTesting by remember { mutableStateOf(false) }
    var geminiTesting by remember { mutableStateOf(false) }
    var customTesting by remember { mutableStateOf(false) }

    var selectedProvider by remember { mutableStateOf(prefs.provider) }
    var customUrl by remember { mutableStateOf(prefs.customApiUrl) }
    var customModelName by remember { mutableStateOf(prefs.customModelName) }
    var nvidiaModel by remember { mutableStateOf(prefs.nvidiaModel) }
    var geminiModel by remember { mutableStateOf(prefs.geminiModel) }

    var transparency by remember { mutableStateOf(prefs.overlayTransparency) }
    var textSize by remember { mutableStateOf(prefs.textSize) }
    var darkText by remember { mutableStateOf(prefs.darkText) }
    var useCustomPrompt by remember { mutableStateOf(prefs.useCustomPrompt) }
    var customPrompt by remember { mutableStateOf(prefs.customPrompt) }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(PrimaryPurple.copy(0.1f), BgDeep)))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                }
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ══ PROVIDER SELECTOR ══
                SettingsCard("🌐 Active Provider", AccentBlue) {
                    Text("Select which AI service to use", fontSize = 12.sp, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderTab(
                            label = "NVIDIA",
                            emoji = "🟣",
                            selected = selectedProvider == Constants.PROVIDER_NVIDIA,
                            hasKey = prefs.nvidiaApiKey.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedProvider = Constants.PROVIDER_NVIDIA
                            prefs.provider = Constants.PROVIDER_NVIDIA
                            convManager.updateProvider()
                        }
                        ProviderTab(
                            label = "Gemini",
                            emoji = "🔵",
                            selected = selectedProvider == Constants.PROVIDER_GEMINI,
                            hasKey = prefs.geminiApiKey.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedProvider = Constants.PROVIDER_GEMINI
                            prefs.provider = Constants.PROVIDER_GEMINI
                            convManager.updateProvider()
                        }
                        ProviderTab(
                            label = "Custom",
                            emoji = "🔧",
                            selected = selectedProvider == Constants.PROVIDER_CUSTOM,
                            hasKey = prefs.customApiKey.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedProvider = Constants.PROVIDER_CUSTOM
                            prefs.provider = Constants.PROVIDER_CUSTOM
                            convManager.updateProvider()
                        }
                    }
                }

                // ══ NVIDIA ══
                SettingsCard(
                    "🟣 NVIDIA NIM", PrimaryPurple,
                    highlighted = selectedProvider == Constants.PROVIDER_NVIDIA
                ) {
                    ApiKeyField(
                        "NVIDIA API Key", "nvapi-xxxxxxxxxxxx",
                        nvidiaKey, showNvidiaKey,
                        { nvidiaKey = it }, { showNvidiaKey = !showNvidiaKey }
                    )
                    Spacer(Modifier.height(8.dp))
                    ProviderActionRow(
                        testing = nviaTesting,
                        buttonColor = PrimaryPurple,
                        onSave = {
                            prefs.nvidiaApiKey = nvidiaKey
                            convManager.refreshApiClient()
                        },
                        onTest = {
                            nviaTesting = true; nvidiaTestResult = null
                            prefs.nvidiaApiKey = nvidiaKey
                            NvidiaApiClient(nvidiaKey, Constants.URL_NVIDIA)
                                .testConnection(nvidiaModel) { ok, msg ->
                                    nviaTesting = false
                                    nvidiaTestResult = if (ok) "✅ $msg" else "❌ $msg"
                                }
                        }
                    )
                    nvidiaTestResult?.let { TestResultBanner(it) }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = OverlayStroke)
                    Spacer(Modifier.height(10.dp))
                    Text("Model", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    ModelGroupLabel("⚡ Fast", AccentTeal)
                    listOf(
                        Constants.MODEL_LLAMA_8B to "Llama 3.1 8B",
                        Constants.MODEL_LLAMA_33_70B to "Llama 3.3 70B",
                        Constants.MODEL_MISTRAL_7B to "Mistral 7B",
                        Constants.MODEL_MISTRAL_NEMO to "Mistral Nemo 12B"
                    ).forEach { (id, label) ->
                        CompactModelOption(label, id, nvidiaModel) { nvidiaModel = it; prefs.nvidiaModel = it }
                    }
                    Spacer(Modifier.height(6.dp))
                    ModelGroupLabel("🧠 Powerful", AccentGold)
                    listOf(
                        Constants.MODEL_LLAMA_70B to "Llama 3.1 70B",
                        Constants.MODEL_LLAMA_405B to "Llama 3.1 405B",
                        Constants.MODEL_MIXTRAL to "Mixtral 8x22B",
                        Constants.MODEL_DEEPSEEK to "DeepSeek R1"
                    ).forEach { (id, label) ->
                        CompactModelOption(label, id, nvidiaModel) { nvidiaModel = it; prefs.nvidiaModel = it }
                    }
                }

                // ══ GEMINI ══
                SettingsCard(
                    "🔵 Google Gemini", AccentBlue,
                    highlighted = selectedProvider == Constants.PROVIDER_GEMINI
                ) {
                    // Info banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlue.copy(0.08f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Free tier available. Get key at aistudio.google.com",
                            fontSize = 11.sp, color = AccentBlue
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ApiKeyField(
                        "Gemini API Key", "AIzaSyxxxxxxxxxxxxxxx",
                        geminiKey, showGeminiKey,
                        { geminiKey = it }, { showGeminiKey = !showGeminiKey }
                    )
                    Spacer(Modifier.height(8.dp))
                    ProviderActionRow(
                        testing = geminiTesting,
                        buttonColor = AccentBlue,
                        onSave = {
                            prefs.geminiApiKey = geminiKey
                            convManager.refreshApiClient()
                        },
                        onTest = {
                            geminiTesting = true; geminiTestResult = null
                            prefs.geminiApiKey = geminiKey
                            GeminiApiClient(geminiKey)
                                .testConnection(geminiModel) { ok, msg ->
                                    geminiTesting = false
                                    geminiTestResult = if (ok) "✅ $msg" else "❌ $msg"
                                }
                        }
                    )
                    geminiTestResult?.let { TestResultBanner(it) }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = OverlayStroke)
                    Spacer(Modifier.height(10.dp))
                    Text("Model", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        Constants.GEMINI_FLASH_2 to "Gemini 2.0 Flash ⚡ (Recommended)",
                        Constants.GEMINI_FLASH_15 to "Gemini 1.5 Flash",
                        Constants.GEMINI_FLASH_8B to "Gemini 1.5 Flash 8B (Fastest)",
                        Constants.GEMINI_PRO_15 to "Gemini 1.5 Pro (Most capable)"
                    ).forEach { (id, label) ->
                        CompactModelOption(label, id, geminiModel) { geminiModel = it; prefs.geminiModel = it }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGold.copy(0.08f))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = AccentGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Free limits: 15 req/min • 1M tokens/day\nPro: 2 req/min • 50 req/day",
                            fontSize = 10.sp, color = AccentGold, lineHeight = 14.sp
                        )
                    }
                }

                // ══ CUSTOM ══
                SettingsCard(
                    "🔧 Custom Endpoint", AccentPink,
                    highlighted = selectedProvider == Constants.PROVIDER_CUSTOM
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentPink.copy(0.08f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = AccentPink, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Any OpenAI-compatible API", fontSize = 11.sp, color = AccentPink)
                    }
                    Spacer(Modifier.height(10.dp))
                    SettingsTextField(customUrl, "Base URL", "https://api.example.com/v1") {
                        customUrl = it; prefs.customApiUrl = it
                    }
                    Spacer(Modifier.height(6.dp))
                    SettingsTextField(customModelName, "Model Name", "model-name") {
                        customModelName = it; prefs.customModelName = it
                    }
                    Spacer(Modifier.height(6.dp))
                    ApiKeyField(
                        "API Key", "sk-xxxxxxxxxxxx",
                        customKey, showCustomKey,
                        { customKey = it }, { showCustomKey = !showCustomKey }
                    )
                    Spacer(Modifier.height(8.dp))
                    ProviderActionRow(
                        testing = customTesting,
                        buttonColor = AccentPink,
                        onSave = {
                            prefs.customApiKey = customKey
                            prefs.customApiUrl = customUrl
                            prefs.customModelName = customModelName
                            convManager.refreshApiClient()
                        },
                        onTest = {
                            customTesting = true; customTestResult = null
                            prefs.customApiKey = customKey
                            prefs.customApiUrl = customUrl
                            prefs.customModelName = customModelName
                            NvidiaApiClient(customKey, customUrl.ifBlank { Constants.URL_NVIDIA })
                                .testConnection(customModelName.ifBlank { Constants.DEFAULT_MODEL }) { ok, msg ->
                                    customTesting = false
                                    customTestResult = if (ok) "✅ $msg" else "❌ $msg"
                                }
                        }
                    )
                    customTestResult?.let { TestResultBanner(it) }
                }

                // ══ CUSTOM PROMPT ══
                SettingsCard("📝 Custom Prompt", AccentPink) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Custom system prompt", fontSize = 13.sp, color = TextPrimary)
                            Text("Override IT interview expertise", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = useCustomPrompt,
                            onCheckedChange = {
                                useCustomPrompt = it
                                prefs.useCustomPrompt = it
                                convManager.reloadPrompt()
                            }
                        )
                    }
                    if (useCustomPrompt) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            label = { Text("System Prompt") },
                            placeholder = { Text("You are an expert in...") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            maxLines = 10,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentPink, unfocusedBorderColor = OverlayStroke
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { prefs.customPrompt = customPrompt; convManager.reloadPrompt() },
                            Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPink.copy(0.85f))
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save & Apply Prompt")
                        }
                    }
                }

                // ══ OVERLAY ══
                SettingsCard("🪟 Overlay Display", AccentTeal) {
                    SliderSetting("Transparency", "${(transparency * 100).toInt()}%", transparency, 0.3f..1f) {
                        transparency = it; prefs.overlayTransparency = it
                    }
                    Spacer(Modifier.height(8.dp))
                    SliderSetting("Text Size", "${textSize.toInt()}sp", textSize, 10f..24f) {
                        textSize = it; prefs.textSize = it
                    }
                    Spacer(Modifier.height(8.dp))
                    ToggleSetting("Dark text mode", "For light backgrounds", darkText) {
                        darkText = it; prefs.darkText = it
                    }
                }

                // ══ EXPORT ══
                SettingsCard("📤 Export", AccentGold) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clip.setPrimaryClip(ClipData.newPlainText("chat", convManager.exportConversation()))
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OverlayStroke)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp)); Text("Copy", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                context.startActivity(Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, convManager.exportConversation())
                                    }, "Share"
                                ))
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp)); Text("Share", fontSize = 12.sp)
                        }
                    }
                }

                // ══ DATA ══
                SettingsCard("🗑 Data", ErrorRed) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp)); Text("Clear All Conversations")
                    }
                }

                // ══ ABOUT ══
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(PrimaryPurple.copy(0.15f), AccentTeal.copy(0.1f))))
                        .border(1.dp, PrimaryPurple.copy(0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentTeal))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Code, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(Constants.DEV_NAME, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("AI Assistant v${Constants.APP_VERSION}", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = OverlayStroke)
                        Spacer(Modifier.height(10.dp))
                        DevInfoRow(Icons.Default.Email, Constants.DEV_EMAIL)
                        Spacer(Modifier.height(4.dp))
                        DevInfoRow(Icons.Default.Code, Constants.DEV_GITHUB)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(PrimaryPurple.copy(0.1f)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active: ${prefs.getProviderDisplayName()}", fontSize = 11.sp, color = AccentTeal)
                            Text(prefs.getModelDisplayName(), fontSize = 11.sp, color = AccentTeal)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Open Source • MIT License", fontSize = 10.sp, color = TextDim)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear conversation?") },
            text = { Text("All messages will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { convManager.clearChat(); showClearDialog = false }) {
                    Text("Clear", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
            containerColor = SurfaceDark
        )
    }
}

// ════════════════════════════════════════
// REUSABLE COMPOSABLES
// ════════════════════════════════════════

@Composable
fun SettingsCard(
    title: String,
    accent: Color,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) accent.copy(0.07f) else SurfaceDark)
            .border(
                if (highlighted) 2.dp else 1.dp,
                if (highlighted) accent.copy(0.6f) else accent.copy(0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            if (highlighted) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ACTIVE", fontSize = 9.sp, color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderTab(
    label: String,
    emoji: String,
    selected: Boolean,
    hasKey: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) PrimaryPurple.copy(0.12f) else Color.Transparent,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) PrimaryPurple else OverlayStroke
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                label, fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) TextPrimary else TextMuted
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (hasKey) SuccessGreen else TextDim))
                Spacer(Modifier.width(3.dp))
                Text(if (hasKey) "Ready" else "No key", fontSize = 8.sp, color = if (hasKey) SuccessGreen else TextDim)
            }
        }
    }
}

@Composable
private fun ProviderActionRow(
    testing: Boolean,
    buttonColor: Color,
    onSave: () -> Unit,
    onTest: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
        ) {
            Icon(Icons.Default.Save, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save", fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = onTest,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, buttonColor.copy(0.5f)),
            enabled = !testing
        ) {
            if (testing) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = buttonColor)
            } else {
                Icon(Icons.Default.Wifi, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Test", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String, hint: String, value: String,
    showValue: Boolean, onValueChange: (String) -> Unit, onToggleShow: () -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(hint, color = TextDim) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (showValue) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
            }
        },
        singleLine = true, shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedBorderColor = PrimaryPurple, unfocusedBorderColor = OverlayStroke,
            focusedLabelColor = PrimaryGlow, unfocusedLabelColor = TextMuted
        )
    )
}

@Composable
private fun TestResultBanner(result: String) {
    val ok = result.startsWith("✅")
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (ok) SuccessGreen.copy(0.1f) else ErrorRed.copy(0.1f))
            .border(1.dp, if (ok) SuccessGreen.copy(0.3f) else ErrorRed.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Error, null,
            tint = if (ok) SuccessGreen else ErrorRed, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(result, fontSize = 12.sp, color = if (ok) SuccessGreen else ErrorRed)
    }
}

@Composable
private fun ModelGroupLabel(label: String, color: Color) {
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun CompactModelOption(label: String, modelId: String, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected == modelId, onClick = { onSelect(modelId) },
            colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple),
            modifier = Modifier.size(32.dp)
        )
        Text(label, fontSize = 13.sp, color = if (selected == modelId) TextPrimary else TextSecondary)
        if (selected == modelId) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Check, null, tint = AccentTeal, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SliderSetting(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = PrimaryGlow, fontWeight = FontWeight.Medium)
    }
    Slider(value = current, onValueChange = onChange, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurple, inactiveTrackColor = OverlayStroke))
}

@Composable
private fun ToggleSetting(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, fontSize = 13.sp, color = TextSecondary)
            Text(desc, fontSize = 11.sp, color = TextMuted)
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryPurple, uncheckedTrackColor = OverlayStroke))
    }
}

@Composable
private fun SettingsTextField(value: String, label: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, placeholder = { Text(placeholder, color = TextDim) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedBorderColor = PrimaryPurple, unfocusedBorderColor = OverlayStroke
        )
    )
}

@Composable
private fun DevInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary)
    }
}
  