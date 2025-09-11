package org.dylanneve1.aicorechat.ui.chat.message

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
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.Alignment
import androidx.core.net.toUri

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    onOpenTools: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    attachmentUri: String? = null,
    isDescribingImage: Boolean = false,
    onRemoveImage: () -> Unit = {},
    showPlus: Boolean = true
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier,
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.navigationBarsPadding()) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp)
                    .heightIn(min = 52.dp, max = 180.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(28.dp)
                    ),
                placeholder = { Text("Message", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) },
                enabled = !isGenerating,
                shape = RoundedCornerShape(28.dp),
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
                leadingIcon = {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        IconButton(onClick = onOpenTools, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.Build, contentDescription = "Tools", modifier = Modifier.size(24.dp)) }
                        if (showPlus) {
                            IconButton(onClick = onPickImage, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.Add, contentDescription = "Add image", modifier = Modifier.size(24.dp)) }
                            IconButton(onClick = onTakePhoto, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Take photo", modifier = Modifier.size(24.dp)) }
                        }
                    }
                },
                trailingIcon = {
                    if (isGenerating) {
                        IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, contentDescription = "Stop generation") }
                    } else {
                        IconButton(
                            onClick = {
                                val trimmed = text.trim()
                                if (trimmed.isNotEmpty() && !isDescribingImage) {
                                    onSendMessage(trimmed)
                                    text = ""
                                }
                            },
                            enabled = text.isNotBlank() && !isDescribingImage
                        ) { Icon(Icons.Outlined.Send, contentDescription = "Send") }
                    }
                },
                supportingText = {
                    if (attachmentUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = attachmentUri.toUri(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (isDescribingImage) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Generating image description…", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("Image attached", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onRemoveImage) { Icon(Icons.Outlined.Close, contentDescription = "Remove image") }
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
} 