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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator

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

        // Stack the (optional) image above the bubble for user messages
        Column(horizontalAlignment = Alignment.End) {
            if (message.isFromUser && message.imageUri != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(154.dp)
                        .padding(bottom = 8.dp)
                ) {
                    AsyncImage(
                        model = message.imageUri.toUri(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                        if (isSearching) SearchingIndicator(contentColor) else TypingIndicator(contentColor)
                    } else {
                        if (message.isFromUser) {
                            Text(
                                text = message.text,
                                color = contentColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Markdown(
                                content = message.text,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .widthIn(max = 320.dp)
                            )
                        }
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

@Composable
private fun AssistantAvatar() {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = "AI Assistant",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UserAvatar() {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchingIndicator(tint: androidx.compose.ui.graphics.Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = tint,
        strokeWidth = 2.dp
    )
}

@Composable
private fun TypingIndicator(tint: androidx.compose.ui.graphics.Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier.size(4.dp),
                shape = CircleShape,
                color = tint.copy(alpha = 0.6f)
            ) {}
        }
    }
} 