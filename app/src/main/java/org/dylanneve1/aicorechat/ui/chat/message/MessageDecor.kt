package org.dylanneve1.aicorechat.ui.chat.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape

@Composable
fun UserAvatar() {
    Avatar(icon = Icons.Outlined.Person)
}

@Composable
fun AssistantAvatar() {
    Avatar(icon = Icons.Outlined.SmartToy)
}

@Composable
private fun Avatar(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun TypingIndicator(color: Color) {
    var dots by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dots = if (dots >= 3) 1 else dots + 1
        }
    }
    androidx.compose.material3.Text(
        text = "typing" + ".".repeat(dots),
        color = color.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun SearchingIndicator(color: Color) {
    var dots by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            dots = if (dots >= 3) 1 else dots + 1
        }
    }
    androidx.compose.material3.Text(
        text = "Searching" + ".".repeat(dots),
        color = color.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyLarge
    )
} 