package org.dylanneve1.aicorechat.ui.chat

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

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean
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
                        if (trimmed.isNotEmpty() && !isGenerating) {
                            onSendMessage(trimmed)
                            text = ""
                        }
                    }
                ),
                trailingIcon = {
                    if (isGenerating) {
                        IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, contentDescription = "Stop generation") }
                    } else {
                        IconButton(
                            onClick = {
                                val trimmed = text.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed)
                                    text = ""
                                }
                            },
                            enabled = text.isNotBlank()
                        ) { Icon(Icons.Outlined.Send, contentDescription = "Send") }
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
