package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.runtime.Stable
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage

@Stable
data class ChatLayoutCallbacks(
    val onNewChat: () -> Unit,
    val onSelectChat: (Long) -> Unit,
    val onShowRenameOptions: (Long, String) -> Unit,
    val onGenerateTitlesForAllChats: () -> Unit,
    val onClearChatRequested: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onTitleClick: () -> Unit,
    val onTitleLongPress: () -> Unit,
    val onDrawerOpen: () -> Unit,
    val onDrawerClose: () -> Unit,
    val onSendMessage: (String) -> Unit,
    val onRegenerateMessage: (Long) -> Unit,
    val onEditMessage: (ChatMessage) -> Unit,
    val onStopGeneration: () -> Unit,
    val onPickImage: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onRemoveImage: () -> Unit,
    val onScrollToBottom: () -> Unit,
    val showSnackbar: (String) -> Unit,
)
