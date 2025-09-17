package org.dylanneve1.aicorechat.ui.chat.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.data.ChatSessionMeta

@Composable
fun DrawerHeader(onNewChat: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Text("Chats", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onNewChat) { Text("New chat") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionItem(meta: ChatSessionMeta, isSelected: Boolean, onClick: () -> Unit, onLongPress: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Text(
            text = meta.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
} 
