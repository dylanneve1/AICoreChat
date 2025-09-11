package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dylanneve1.aicorechat.data.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    message: ChatMessage,
    onCopy: (String) -> Unit
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isFromUser) {
            Avatar(icon = Icons.Outlined.SmartToy)
            Spacer(Modifier.width(8.dp))
        }

        val bubbleColor = if (message.isFromUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh
        val contentColor = if (message.isFromUser) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface

        MessageBubble(
            isFromUser = message.isFromUser,
            backgroundColor = bubbleColor,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .animateContentSize()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (message.text.isNotBlank()) {
                            clipboard.setText(AnnotatedString(message.text))
                            onCopy(message.text)
                        }
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (message.isStreaming && message.text.isBlank()) {
                    TypingIndicator(contentColor)
                } else {
                    Text(
                        text = message.text,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (message.isFromUser) {
            Spacer(Modifier.width(8.dp))
            Avatar(icon = Icons.Outlined.Person)
        }
    }
}

@Composable
private fun Avatar(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TypingIndicator(color: Color) {
    var dots by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dots = if (dots >= 3) 1 else dots + 1
        }
    }
    Text(
        text = "typing" + ".".repeat(dots),
        color = color.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyLarge
    )
}
