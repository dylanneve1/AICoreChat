package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.ChatViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(max(0, uiState.messages.size - 1))
        }
    }

    LaunchedEffect(uiState.modelError) {
        uiState.modelError?.let { snackbarHostState.showSnackbar(it) }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val lastIndex = uiState.messages.lastIndex
            if (lastIndex < 0) return@derivedStateOf false
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem < lastIndex
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AICoreChatTopAppBar(
                scrollBehavior = scrollBehavior,
                isChatNotEmpty = uiState.messages.isNotEmpty(),
                onClearClick = { showClearDialog = true },
                onSettingsClick = { showSettingsSheet = true }
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
                isGenerating = uiState.isGenerating
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(uiState.messages.size - 1)
                        }
                    }
                ) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Scroll to bottom")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp // Removed incorrect manual padding calculation
                )
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    MessageRow(
                        message = message,
                        onCopy = { copiedText: String ->
                            scope.launch {
                                val shortText = copiedText.take(40).replace("\n", " ")
                                snackbarHostState.showSnackbar(
                                    "Copied: \"$shortText${if (copiedText.length > 40) "…" else ""}\""
                                )
                            }
                        }
                    )
                }
            }
        }
    )

    // Clear chat dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear conversation?") },
            text = { Text("This will remove all messages from this chat.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChat()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Settings sheet
    if (showSettingsSheet) {
        SettingsSheet(
            temperature = uiState.temperature,
            topK = uiState.topK,
            onTemperatureChange = viewModel::updateTemperature,
            onTopKChange = viewModel::updateTopK,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AICoreChatTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    isChatNotEmpty: Boolean,
    onClearClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "AICore Chat",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            )
        },
        actions = {
            IconButton(onClick = onClearClick, enabled = isChatNotEmpty) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Clear chat"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}
