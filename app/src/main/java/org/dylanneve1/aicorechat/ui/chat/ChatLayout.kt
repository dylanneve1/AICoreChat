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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.R
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.ui.chat.drawer.DrawerHeader
import org.dylanneve1.aicorechat.ui.chat.drawer.SessionItem
import org.dylanneve1.aicorechat.ui.chat.message.MessageInput
import org.dylanneve1.aicorechat.ui.chat.message.MessageRow
import org.dylanneve1.aicorechat.ui.chat.topbar.AICoreChatTopAppBar

@Stable
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
    actions: ChatLayoutCallbacks,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ChatDrawerContent(
                uiState = uiState,
                drawerListState = drawerListState,
                actions = actions,
            )
        },
    ) {
        ChatScaffold(
            uiState = uiState,
            listState = listState,
            snackbarHostState = snackbarHostState,
            scrollBehavior = scrollBehavior,
            showScrollToBottom = showScrollToBottom,
            actions = actions,
        )
    }
}

@Composable
private fun ChatDrawerContent(uiState: ChatUiState, drawerListState: LazyListState, actions: ChatLayoutCallbacks) {
    ModalDrawerSheet {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                DrawerHeader(
                    onNewChat = {
                        actions.onNewChat()
                        actions.onDrawerClose()
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
                                actions.onSelectChat(meta.id)
                                actions.onDrawerClose()
                            },
                            onLongPress = { actions.onShowRenameOptions(meta.id, meta.name) },
                        )
                    }
                }
            }
            FilledTonalButton(
                onClick = actions.onGenerateTitlesForAllChats,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) { Text("Generate titles for all chats") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScaffold(
    uiState: ChatUiState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    scrollBehavior: TopAppBarScrollBehavior,
    showScrollToBottom: Boolean,
    actions: ChatLayoutCallbacks,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        topBar = {
            ChatTopBar(uiState = uiState, scrollBehavior = scrollBehavior, actions = actions)
        },
        bottomBar = {
            ChatInputBar(uiState = uiState, actions = actions)
        },
        floatingActionButton = {
            ScrollToBottomFab(showScrollToBottom = showScrollToBottom, onScrollToBottom = actions.onScrollToBottom)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (uiState.messages.isEmpty()) {
            EmptyChatState(modifier = Modifier.padding(innerPadding))
        } else {
            ChatMessagesList(
                uiState = uiState,
                listState = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                actions = actions,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(uiState: ChatUiState, scrollBehavior: TopAppBarScrollBehavior, actions: ChatLayoutCallbacks) {
    AICoreChatTopAppBar(
        scrollBehavior = scrollBehavior,
        isChatNotEmpty = uiState.messages.isNotEmpty(),
        onClearClick = actions.onClearChatRequested,
        onSettingsClick = actions.onOpenSettings,
        onMenuClick = actions.onDrawerOpen,
        title = uiState.currentSessionName,
        onTitleLongPress = actions.onTitleLongPress,
        onTitleClick = {
            if (!uiState.isGenerating && uiState.messages.any { it.isFromUser }) {
                actions.onTitleClick()
            } else {
                val msg = if (uiState.isGenerating) {
                    "Please wait for the response to finish."
                } else {
                    "Chat is empty."
                }
                actions.showSnackbar(msg)
            }
        },
    )
}

@Composable
private fun ChatInputBar(uiState: ChatUiState, actions: ChatLayoutCallbacks) {
    MessageInput(
        onSendMessage = actions.onSendMessage,
        onStop = actions.onStopGeneration,
        isGenerating = uiState.isGenerating,
        onPickImage = {
            if (!uiState.multimodalEnabled) {
                actions.showSnackbar("Multimodal is disabled in Settings")
            } else {
                actions.onPickImage()
            }
        },
        onTakePhoto = {
            if (!uiState.multimodalEnabled) {
                actions.showSnackbar("Multimodal is disabled in Settings")
            } else {
                actions.onTakePhoto()
            }
        },
        attachmentUri = uiState.pendingImageUri,
        isDescribingImage = uiState.isDescribingImage,
        onRemoveImage = actions.onRemoveImage,
        showPlus = uiState.multimodalEnabled,
    )
}

@Composable
private fun ScrollToBottomFab(showScrollToBottom: Boolean, onScrollToBottom: () -> Unit) {
    AnimatedVisibility(
        visible = showScrollToBottom,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
    ) {
        FloatingActionButton(onClick = onScrollToBottom) {
            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Scroll to bottom")
        }
    }
}

@Composable
private fun ChatMessagesList(
    uiState: ChatUiState,
    listState: LazyListState,
    modifier: Modifier,
    actions: ChatLayoutCallbacks,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
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
                    actions.showSnackbar(display)
                },
                onRegenerate = actions.onRegenerateMessage,
                onEdit = actions.onEditMessage,
                isSearching = isLast && message.isStreaming && uiState.isSearchInProgress,
            )
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
