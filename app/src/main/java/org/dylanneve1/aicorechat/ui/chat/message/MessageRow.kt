package org.dylanneve1.aicorechat.ui.chat.message

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.data.ChatMessage
import com.mikepenz.markdown.m3.Markdown
import coil.compose.AsyncImage
import androidx.core.net.toUri

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    isSearching: Boolean = false
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isFromUser) {
            AssistantAvatar()
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
                if (message.imageUri != null) {
                    AsyncImage(
                        model = message.imageUri.toUri(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                if (message.isStreaming && message.text.isBlank()) {
                    if (isSearching) SearchingIndicator(contentColor) else TypingIndicator(contentColor)
                } else {
                    if (message.isFromUser) {
                        Text(
                            text = message.text,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Markdown(content = message.text)
                    }
                }
            }
        }

        if (message.isFromUser) {
            Spacer(Modifier.width(8.dp))
            UserAvatar()
        }
    }
} 