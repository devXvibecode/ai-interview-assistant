package com.assistant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assistant.app.ai.ChatMessage
import com.assistant.app.ai.MessageRole
import com.assistant.app.ui.theme.*
import com.assistant.app.utils.toTimeString

@Composable
fun MessageBubble(msg: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = msg.role == MessageRole.USER
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp, topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 3.dp,
                        bottomEnd = if (isUser) 3.dp else 14.dp
                    )
                )
                .background(if (isUser) UserBubble else AiBubble)
                .padding(10.dp)
        ) {
            Text(
                text = if (msg.isStreaming) "${msg.content}▌" else msg.content,
                color = if (msg.isError) ErrorRed else Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = msg.timestamp.toTimeString(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}