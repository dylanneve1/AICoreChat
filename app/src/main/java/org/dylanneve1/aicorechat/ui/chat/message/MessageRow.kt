package org.dylanneve1.aicorechat.ui.chat.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    onRegenerate: (Long) -> Unit = {},
    onEdit: (ChatMessage) -> Unit = {},
    isSearching: Boolean = false,
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    var showActions by remember(message.id) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!message.isFromUser) {
            AssistantAvatar()
            Spacer(Modifier.width(8.dp))
        }

        val bubbleColor = if (message.isFromUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val contentColor = if (message.isFromUser) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        // Stack the (optional) image above the bubble for user messages
        Column(horizontalAlignment = Alignment.End) {
            if (message.isFromUser && message.imageUri != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(154.dp)
                        .padding(bottom = 8.dp),
                ) {
                    AsyncImage(
                        model = message.imageUri.toUri(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Box {
                MessageBubble(
                    isFromUser = message.isFromUser,
                    backgroundColor = bubbleColor,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .animateContentSize()
                        .combinedClickable(
                            enabled = !message.isStreaming,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                showActions = true
                            },
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        val contentModifier = Modifier.wrapContentWidth()
                        if (message.isStreaming && message.text.isBlank()) {
                            if (isSearching) SearchingIndicator(contentColor) else TypingIndicator(contentColor)
                        } else {
                            Text(
                                text = message.text,
                                color = contentColor,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = contentModifier,
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showActions,
                    onDismissRequest = { showActions = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                            )
                        },
                        enabled = message.text.isNotBlank(),
                        onClick = {
                            showActions = false
                            if (message.text.isNotBlank()) {
                                clipboard.setText(AnnotatedString(message.text))
                                onCopy(message.text)
                            }
                        },
                    )

                    if (message.isFromUser) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showActions = false
                                onEdit(message)
                            },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Regenerate") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                )
                            },
                            enabled = !message.isStreaming,
                            onClick = {
                                showActions = false
                                onRegenerate(message.id)
                            },
                        )
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
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = "AI Assistant",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun UserAvatar() {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SearchingIndicator(tint: androidx.compose.ui.graphics.Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_animation")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "search_scale",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "search_alpha",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = "Searching",
            modifier = Modifier
                .size(18.dp)
                .scale(scale)
                .alpha(alpha),
            tint = tint,
        )

        Column {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
            ) {
                Text(
                    "Searching the web...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                    modifier = Modifier.alpha(alpha),
                )
            }

            Spacer(Modifier.size(2.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = tint,
                trackColor = tint.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun TypingIndicator(tint: androidx.compose.ui.graphics.Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_animation")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        repeat(3) { index ->
            val delay = index * 100
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = delay,
                        easing = LinearEasing,
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "dot_alpha_$index",
            )

            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = delay,
                        easing = LinearEasing,
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "dot_scale_$index",
            )

            Surface(
                modifier = Modifier
                    .size(6.dp)
                    .scale(scale)
                    .alpha(alpha),
                shape = CircleShape,
                color = tint,
            ) {}
        }
    }
}
