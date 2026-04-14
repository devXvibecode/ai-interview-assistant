package com.assistant.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assistant.app.ui.theme.PrimaryPurple

private val QUICK_ACTIONS = listOf(
    "Summarize" to "Please summarize our conversation so far.",
    "Explain" to "Can you explain that more simply?",
    "Action Items" to "List the action items from our conversation.",
    "Rephrase" to "Can you rephrase your last response?",
    "More Detail" to "Please give more detail on that."
)

@Composable
fun QuickActions(onAction: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QUICK_ACTIONS.forEach { (label, prompt) ->
            AssistChip(
                onClick = { onAction(prompt) },
                label = { Text(label, fontSize = 11.sp) },
                shape = RoundedCornerShape(18.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.12f)
                )
            )
        }
    }
}