package com.assistant.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assistant.app.ai.AIMode
import com.assistant.app.ai.ChatMessage
import com.assistant.app.ai.ConversationManager
import com.assistant.app.ai.MessageRole
import com.assistant.app.data.AppPreferences
import com.assistant.app.ui.components.QuickActions
import com.assistant.app.ui.theme.*
import com.assistant.app.utils.Constants
import com.assistant.app.utils.toTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    convManager: ConversationManager,
    prefs: AppPreferences,
    onSettings: () -> Unit,
    onStartOverlay: () -> Unit
) {
    val context = LocalContext.current
    val messages by convManager.messages.collectAsState()
    val isProcessing by convManager.isProcessing.collectAsState()
    val currentMode by convManager.currentMode.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModeDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Use Scaffold to handle insets automatically
    Scaffold(
        containerColor = BgDeep,
        topBar = {
            // ── Top Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = 0.15f),
                                BgDeep
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryPurple, AccentTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy, null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            Constants.APP_NAME,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (prefs.hasApiKey()) AccentTeal else ErrorRed
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${currentMode.emoji} ${currentMode.displayName}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Actions
                    IconButton(onClick = { showModeDialog = true }) {
                        Icon(Icons.Default.Tune, "Mode", tint = TextMuted)
                    }

                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, "Export", tint = TextMuted)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📋 Copy") },
                                onClick = {
                                    val clip = context.getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                    ) as ClipboardManager
                                    clip.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "chat", convManager.exportConversation()
                                        )
                                    )
                                    showExportMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📤 Share") },
                                onClick = {
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    convManager.exportConversation()
                                                )
                                            }, "Share"
                                        )
                                    )
                                    showExportMenu = false
                                }
                            )
                        }
                    }

                    // Overlay button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentTeal.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                AccentTeal.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onStartOverlay) {
                            Icon(
                                Icons.Default.ChatBubble,
                                "Overlay",
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextMuted)
                    }
                }
            }
        },
        bottomBar = {
            // ── Input Bar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgDeep)
                    .navigationBarsPadding()
            ) {
                QuickActions(onAction = { convManager.sendMessage(it) })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDark)
                            .border(
                                1.dp,
                                if (inputText.isNotBlank())
                                    PrimaryPurple.copy(alpha = 0.5f)
                                else OverlayStroke,
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask a question to practice…",
                                    color = TextDim,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 4,
                            enabled = !isProcessing
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val sendEnabled = inputText.isNotBlank() && !isProcessing

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (sendEnabled)
                                    Brush.linearGradient(
                                        listOf(PrimaryPurple, AccentTeal)
                                    )
                                else Brush.linearGradient(
                                    listOf(CardDark, CardDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val t = inputText.trim()
                                if (t.isNotEmpty()) {
                                    convManager.sendMessage(t)
                                    inputText = ""
                                }
                            },
                            enabled = sendEnabled
                        ) {
                            Icon(
                                Icons.Default.Send,
                                "Send",
                                tint = if (sendEnabled) Color.White else TextDim
                            )
                        }
                    }
                }

                // Footer branding
                Text(
                    "Built by ${Constants.DEV_NAME} • ${Constants.DEV_GITHUB}",
                    fontSize = 10.sp,
                    color = TextDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── API Key Warning ──
            if (!prefs.hasApiKey()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarnOrange.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            WarnOrange.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning, null,
                        tint = WarnOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No API key — tap Settings",
                        fontSize = 12.sp,
                        color = WarnOrange
                    )
                }
            }

            // ── Info Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryPurple.copy(alpha = 0.07f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info, null,
                        tint = AccentTeal,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Tap 💬 for live overlay • Hold window 3s to move",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Text(
                    "${prefs.getProviderDisplayName()} • ${prefs.getModelDisplayName()}",
                    fontSize = 9.sp,
                    color = TextDim
                )
            }

            // ── Messages or Empty ──
            if (messages.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            PrimaryPurple.copy(alpha = 0.3f),
                                            AccentTeal.copy(alpha = 0.3f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School, null,
                                Modifier.size(36.dp),
                                tint = PrimaryGlow
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "Interview Assistant",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Type a question to practice, or tap 💬 to start live interview assistance",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardDark)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Code, null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "by ${Constants.DEV_NAME}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ModernMsgBubble(msg)
                    }
                    if (isProcessing) {
                        item { TypingIndicator() }
                    }
                }
            }
        }
    }

    // ── Mode Dialog ──
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune, null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Select Mode", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    AIMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (mode == currentMode)
                                        PrimaryPurple.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == currentMode,
                                onClick = {
                                    convManager.changeMode(mode)
                                    showModeDialog = false
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mode.emoji, fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        mode.displayName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    mode.description,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun ModernMsgBubble(msg: ChatMessage) {
    val isUser = msg.role == MessageRole.USER
    val isSkipped = msg.content.startsWith("🔵")
    val isAI = msg.role == MessageRole.ASSISTANT

    if (isSkipped) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(TextDim)
            )
            Spacer(Modifier.width(6.dp))
            Text(msg.content, fontSize = 10.sp, color = TextDim, maxLines = 1)
        }
        return
    }

    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isAI) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(AiBubble)
                    .border(
                        1.dp,
                        AiBubbleStroke.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PrimaryPurple, AccentTeal))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "A",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AI Assistant",
                        fontSize = 10.sp,
                        color = PrimaryGlow,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        msg.timestamp.toTimeString(),
                        fontSize = 9.sp,
                        color = TextDim
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (msg.isStreaming) "${msg.content}▌" else msg.content,
                    color = if (msg.isError) ErrorRed else TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                    .background(UserBubble)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = msg.timestamp.toTimeString(),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AiBubble)
            .border(
                1.dp,
                AiBubbleStroke.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Psychology, null,
            tint = PrimaryGlow,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        repeat(3) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PrimaryGlow.copy(alpha = 0.7f))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("Thinking...", fontSize = 11.sp, color = TextMuted)
    }
}