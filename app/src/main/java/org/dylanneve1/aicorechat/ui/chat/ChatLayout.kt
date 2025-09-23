package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.R
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.ui.chat.drawer.DrawerHeader
import org.dylanneve1.aicorechat.ui.chat.drawer.SessionItem
import org.dylanneve1.aicorechat.ui.chat.message.MessageInput
import org.dylanneve1.aicorechat.ui.chat.message.MessageRow
import org.dylanneve1.aicorechat.ui.chat.topbar.AICoreChatTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatLayout(
    uiState: ChatUiState,
    listState: LazyListState,
    drawerState: DrawerState,
    drawerListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    scrollBehavior: TopAppBarScrollBehavior,
    showScrollToBottom: Boolean,
    onNewChat: () -> Unit,
    onSelectChat: (Long) -> Unit,
    onShowRenameOptions: (Long, String) -> Unit,
    onGenerateTitlesForAllChats: () -> Unit,
    onClearChatRequested: () -> Unit,
    onOpenSettings: () -> Unit,
    onTitleClick: () -> Unit,
    onTitleLongPress: () -> Unit,
    onDrawerOpen: () -> Unit,
    onDrawerClose: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRegenerateMessage: (Long) -> Unit,
    onEditMessage: (ChatMessage) -> Unit,
    onStopGeneration: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    onScrollToBottom: () -> Unit,
    showSnackbar: (String) -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Box(modifier = Modifier.fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        DrawerHeader(
                            onNewChat = {
                                onNewChat()
                                onDrawerClose()
                            },
                        )
                        LazyColumn(
                            state = drawerListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        ) {
                            items(uiState.sessions, key = { it.id }) { meta ->
                                SessionItem(
                                    meta = meta,
                                    isSelected = meta.id == uiState.currentSessionId,
                                    onClick = {
                                        onSelectChat(meta.id)
                                        onDrawerClose()
                                    },
                                    onLongPress = { onShowRenameOptions(meta.id, meta.name) },
                                )
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = onGenerateTitlesForAllChats,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) { Text("Generate titles for all chats") }
                }
            }
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding(),
            topBar = {
                AICoreChatTopAppBar(
                    scrollBehavior = scrollBehavior,
                    isChatNotEmpty = uiState.messages.isNotEmpty(),
                    onClearClick = onClearChatRequested,
                    onSettingsClick = onOpenSettings,
                    onMenuClick = onDrawerOpen,
                    title = uiState.currentSessionName,
                    onTitleLongPress = onTitleLongPress,
                    onTitleClick = {
                        if (!uiState.isGenerating && uiState.messages.any { it.isFromUser }) {
                            onTitleClick()
                        } else {
                            val msg = if (uiState.isGenerating) {
                                "Please wait for the response to finish."
                            } else {
                                "Chat is empty."
                            }
                            showSnackbar(msg)
                        }
                    },
                )
            },
            bottomBar = {
                MessageInput(
                    onSendMessage = onSendMessage,
                    onStop = onStopGeneration,
                    isGenerating = uiState.isGenerating,
                    onPickImage = {
                        if (!uiState.multimodalEnabled) {
                            showSnackbar("Multimodal is disabled in Settings")
                        } else {
                            onPickImage()
                        }
                    },
                    onTakePhoto = {
                        if (!uiState.multimodalEnabled) {
                            showSnackbar("Multimodal is disabled in Settings")
                        } else {
                            onTakePhoto()
                        }
                    },
                    attachmentUri = uiState.pendingImageUri,
                    isDescribingImage = uiState.isDescribingImage,
                    onRemoveImage = onRemoveImage,
                    showPlus = uiState.multimodalEnabled,
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    FloatingActionButton(
                        onClick = onScrollToBottom,
                    ) { Icon(Icons.Outlined.ArrowDownward, contentDescription = "Scroll to bottom") }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            if (uiState.messages.isEmpty()) {
                EmptyChatState(modifier = Modifier.padding(innerPadding))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                ) {
                    items(items = uiState.messages, key = { it.id }) { message ->
                        val isLast = message.id == uiState.messages.lastOrNull()?.id
                        MessageRow(
                            message = message,
                            onCopy = { copiedText: String ->
                                val shortText = copiedText.take(40).replace("\n", " ")
                                val display = "Copied: \"$shortText${if (copiedText.length > 40) "…" else ""}\""
                                showSnackbar(display)
                            },
                            onRegenerate = onRegenerateMessage,
                            onEdit = onEditMessage,
                            isSearching = isLast && message.isStreaming && uiState.isSearchInProgress,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Ask anything, powered by Gemini Nano",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
