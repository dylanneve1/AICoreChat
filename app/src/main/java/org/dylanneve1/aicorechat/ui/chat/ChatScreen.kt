package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.ChatSessionMeta
import org.dylanneve1.aicorechat.data.ChatViewModel
import kotlin.math.max
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.CircularProgressIndicator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import org.dylanneve1.aicorechat.R
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import org.dylanneve1.aicorechat.ui.chat.tools.ToolsSheet
import org.dylanneve1.aicorechat.ui.chat.drawer.DrawerHeader
import org.dylanneve1.aicorechat.ui.chat.drawer.SessionItem
import org.dylanneve1.aicorechat.ui.chat.topbar.AICoreChatTopAppBar
import org.dylanneve1.aicorechat.ui.chat.message.MessageRow
import org.dylanneve1.aicorechat.ui.chat.message.MessageInput

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showRenameDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var renameTitleDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results ignored; ViewModel will use if granted */ }

    val photoFile = remember {
        val ctx = context.applicationContext
        val dir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        File(dir, "IMG_${'$'}time.jpg")
    }
    var pendingPhotoUri by remember { mutableStateOf(android.net.Uri.EMPTY) }
    val takePictureLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success && pendingPhotoUri != android.net.Uri.EMPTY) {
            viewModel.onImageSelected(pendingPhotoUri)
        } else {
            pendingPhotoUri = android.net.Uri.EMPTY
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                viewModel.onImageSelected(uri)
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar(e.message ?: "Failed to attach image") }
            }
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            focusManager.clearFocus(force = true)
            viewModel.purgeEmptyChats()
        }
    }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(
                    onNewChat = {
                        viewModel.newChat()
                        scope.launch { drawerState.close() }
                    }
                )
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = true),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.sessions, key = { it.id }) { meta ->
                        SessionItem(
                            meta = meta,
                            isSelected = meta.id == uiState.currentSessionId,
                            onClick = {
                                viewModel.selectChat(meta.id)
                                scope.launch { drawerState.close() }
                            },
                            onLongPress = { showRenameDialog = meta.id to meta.name }
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { viewModel.generateTitlesForAllChats() },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) { Text("Generate titles for all chats") }
            }
        }
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
                    onClearClick = { showClearDialog = true },
                    onSettingsClick = { focusManager.clearFocus(force = true); showSettingsSheet = true },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    title = uiState.currentSessionName,
                    onTitleLongPress = { renameTitleDialog = true },
                    onTitleClick = {
                        if (!uiState.isGenerating && uiState.messages.any { it.isFromUser }) {
                            viewModel.generateChatTitle()
                        } else {
                            scope.launch {
                                val msg = if (uiState.isGenerating) "Please wait for the response to finish." else "Chat is empty."
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                MessageInput(
                    onSendMessage = viewModel::sendMessage,
                    onStop = viewModel::stopGeneration,
                    isGenerating = uiState.isGenerating,
                    onOpenTools = { showToolsSheet = true },
                    onPickImage = {
                        if (!uiState.multimodalEnabled) {
                            scope.launch { snackbarHostState.showSnackbar("Multimodal is disabled in Settings") }
                        } else {
                            pickImageLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        }
                    },
                    onTakePhoto = {
                        if (!uiState.multimodalEnabled) {
                            scope.launch { snackbarHostState.showSnackbar("Multimodal is disabled in Settings") }
                        } else {
                            try {
                                val ctx = context
                                val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", photoFile)
                                pendingPhotoUri = uri
                                takePictureLauncher.launch(uri)
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(e.message ?: "Failed to open camera") }
                            }
                        }
                    },
                    attachmentUri = uiState.pendingImageUri,
                    isDescribingImage = uiState.isDescribingImage,
                    onRemoveImage = viewModel::clearPendingImage,
                    showPlus = uiState.multimodalEnabled
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
                            scope.launch { listState.animateScrollToItem(uiState.messages.size - 1) }
                        }
                    ) { Icon(Icons.Outlined.ArrowDownward, contentDescription = "Scroll to bottom") }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            content = { innerPadding ->
                if (uiState.messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ask anything, powered by Gemini Nano",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        items(items = uiState.messages, key = { it.id }) { message ->
                            val isLast = message.id == uiState.messages.lastOrNull()?.id
                            MessageRow(
                                message = message,
                                onCopy = { copiedText: String ->
                                    scope.launch {
                                        val shortText = copiedText.take(40).replace("\n", " ")
                                        snackbarHostState.showSnackbar(
                                            "Copied: \"$shortText${if (copiedText.length > 40) "…" else ""}\""
                                        )
                                    }
                                },
                                isSearching = isLast && message.isStreaming && uiState.isSearchInProgress
                            )
                        }
                    }
                }
            }
        )
    }

    if (uiState.isTitleGenerating) {
        AlertDialog(
            onDismissRequest = { /* not dismissible */ },
            title = { Text("Generating title") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Using chat context…")
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (uiState.isBulkTitleGenerating) {
        AlertDialog(
            onDismissRequest = { /* not dismissible */ },
            title = { Text("Generating titles") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Processing all chats…")
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showRenameDialog != null) {
        val (id, currentName) = showRenameDialog!!
        var name by remember(currentName) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Chat options") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Rename chat") })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            val trimmed = name.trim().ifEmpty { "New Chat" }
                            viewModel.renameChat(id, trimmed)
                            showRenameDialog = null
                        }) { Text("Save") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { showRenameDialog = null; showDeleteDialog = id }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Close") } }
        )
    }

    if (showDeleteDialog != null) {
        val deleteId = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete chat?") },
            text = { Text("This will permanently remove this chat.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteChat(deleteId); showDeleteDialog = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    if (renameTitleDialog) {
        var name by remember(uiState.currentSessionName) { mutableStateOf(uiState.currentSessionName) }
        AlertDialog(
            onDismissRequest = { renameTitleDialog = false },
            title = { Text("Rename chat") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { val t = name.trim().ifEmpty { "New Chat" }; viewModel.renameCurrentChat(t); renameTitleDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renameTitleDialog = false }) { Text("Cancel") } }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear conversation?") },
            text = { Text("This will remove this chat completely and start a new one.") },
            confirmButton = { TextButton(onClick = { viewModel.clearChat(); showClearDialog = false }) { Text("Delete chat") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSettingsSheet) {
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        SettingsScreen(
            temperature = uiState.temperature,
            topK = uiState.topK,
            onTemperatureChange = viewModel::updateTemperature,
            onTopKChange = viewModel::updateTopK,
            userName = uiState.userName,
            personalContextEnabled = uiState.personalContextEnabled,
            onUserNameChange = viewModel::updateUserName,
            onPersonalContextToggle = { enabled ->
                if (enabled && !hasFine && !hasCoarse) {
                    locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                viewModel.updatePersonalContextEnabled(enabled)
            },
            webSearchEnabled = uiState.webSearchEnabled,
            onWebSearchToggle = viewModel::updateWebSearchEnabled,
            multimodalEnabled = uiState.multimodalEnabled,
            onMultimodalToggle = viewModel::updateMultimodalEnabled,
            onWipeAllChats = viewModel::wipeAllChats,
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showToolsSheet) {
        ToolsSheet(
            webSearchEnabled = uiState.webSearchEnabled,
            onWebSearchToggle = viewModel::updateWebSearchEnabled,
            personalContextEnabled = uiState.personalContextEnabled,
            onPersonalContextToggle = { enabled ->
                val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (enabled && !hasFine && !hasCoarse) {
                    locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                viewModel.updatePersonalContextEnabled(enabled)
            },
            onDismiss = { showToolsSheet = false }
        )
    }
}
