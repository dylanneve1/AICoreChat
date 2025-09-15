package org.dylanneve1.aicorechat.ui.chat.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.core.net.toUri

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    onPickImage: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    attachmentUri: String? = null,
    isDescribingImage: Boolean = false,
    onRemoveImage: () -> Unit = {},
    showPlus: Boolean = true
) {
    var text by remember { mutableStateOf("") }
    val hasText = text.isNotBlank()

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .heightIn(min = 56.dp, max = 200.dp),
            placeholder = { 
                Text(
                    "Ask anything...", 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ) 
            },
            enabled = !isGenerating,
            shape = RoundedCornerShape(24.dp),
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty() && !isGenerating && !isDescribingImage) {
                        onSendMessage(trimmed)
                        text = ""
                    }
                }
            ),
            leadingIcon = if (showPlus) {
                {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPickImage,
                            modifier = Modifier.size(40.dp),
                            enabled = !isGenerating && !isDescribingImage
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add image",
                                modifier = Modifier.size(22.dp),
                                tint = if (!isGenerating && !isDescribingImage)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        IconButton(
                            onClick = onTakePhoto,
                            modifier = Modifier.size(40.dp),
                            enabled = !isGenerating && !isDescribingImage
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Take photo",
                                modifier = Modifier.size(22.dp),
                                tint = if (!isGenerating && !isDescribingImage)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            } else null,
            trailingIcon = {
                    AnimatedContent(
                        targetState = isGenerating,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + 
                             slideInVertically(animationSpec = tween(300)) { it / 2 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(300)) + 
                                    slideOutVertically(animationSpec = tween(300)) { -it / 2 }
                                )
                        },
                        label = "send/stop button"
                    ) { generating ->
                        if (generating) {
                            IconButton(
                                onClick = onStop,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Stop,
                                    contentDescription = "Stop generation",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            val sendButtonColor by animateColorAsState(
                                targetValue = if (hasText && !isDescribingImage)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                animationSpec = tween(200),
                                label = "send button color"
                            )
                            
                            IconButton(
                                onClick = {
                                    val trimmed = text.trim()
                                    if (trimmed.isNotEmpty() && !isDescribingImage) {
                                        onSendMessage(trimmed)
                                        text = ""
                                    }
                                },
                                enabled = hasText && !isDescribingImage,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (hasText && !isDescribingImage)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    contentColor = sendButtonColor
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
            },
            supportingText = {
                    if (attachmentUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = attachmentUri.toUri(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                if (isDescribingImage) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Analyzing image...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        "Image ready",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = onRemoveImage,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove image",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
            )
        )
    }
}
