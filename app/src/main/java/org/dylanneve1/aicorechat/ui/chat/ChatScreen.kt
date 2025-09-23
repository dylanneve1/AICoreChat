package org.dylanneve1.aicorechat.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.chat.ChatViewModel
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val drawerListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var currentSettingsDestination by remember { mutableStateOf<SettingsDestination?>(null) }
    val settingsVisibilityState = remember { MutableTransitionState(false) }
    settingsVisibilityState.targetState = showSettingsSheet
    val scrimInteractionSource = remember { MutableInteractionSource() }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var renameDialogState by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var deleteDialogId by remember { mutableStateOf<Long?>(null) }
    var renameTitleDialog by remember { mutableStateOf(false) }
    var messageToEdit by remember { mutableStateOf<ChatMessage?>(null) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val hasFine = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    var pendingPhotoUri by remember { mutableStateOf(Uri.EMPTY) }
    val takePictureLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success && pendingPhotoUri != Uri.EMPTY) {
            viewModel.onImageSelected(pendingPhotoUri)
        }
        pendingPhotoUri = Uri.EMPTY
    }

    val pickImageLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { viewModel.onImageSelected(uri) }
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Failed to attach image") }
            }
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            focusManager.clearFocus(force = true)
            viewModel.purgeEmptyChats()
        } else if (drawerState.currentValue == DrawerValue.Closed) {
            scope.launch { drawerListState.scrollToItem(0) }
            viewModel.purgeEmptyChats()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(max(0, uiState.messages.size - 1))
        }
    }

    LaunchedEffect(uiState.modelError) {
        uiState.modelError?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val lastIndex = uiState.messages.lastIndex
            if (lastIndex < 0) return@derivedStateOf false
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem < lastIndex
        }
    }

    val showSnackbar: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
        Unit
    }
    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
        Unit
    }
    val closeDrawer: () -> Unit = {
        scope.launch { drawerState.close() }
        Unit
    }
    val scrollToBottom: () -> Unit = {
        scope.launch { listState.animateScrollToItem(max(0, uiState.messages.size - 1)) }
        Unit
    }

    val onPickImage = {
        pickImageLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    val onTakePhoto = {
        try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val newFile = File(dir, "IMG_$time.jpg")
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", newFile)
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            showSnackbar(e.message ?: "Failed to open camera")
        }
    }

    ChatLayout(
        uiState = uiState,
        listState = listState,
        drawerState = drawerState,
        drawerListState = drawerListState,
        snackbarHostState = snackbarHostState,
        scrollBehavior = scrollBehavior,
        showScrollToBottom = showScrollToBottom,
        onNewChat = viewModel::newChat,
        onSelectChat = viewModel::selectChat,
        onShowRenameOptions = { id, name -> renameDialogState = id to name },
        onGenerateTitlesForAllChats = viewModel::generateTitlesForAllChats,
        onClearChatRequested = { showClearDialog = true },
        onOpenSettings = {
            focusManager.clearFocus(force = true)
            showSettingsSheet = true
        },
        onTitleClick = viewModel::generateChatTitle,
        onTitleLongPress = { renameTitleDialog = true },
        onDrawerOpen = openDrawer,
        onDrawerClose = closeDrawer,
        onSendMessage = viewModel::sendMessage,
        onRegenerateMessage = viewModel::regenerateAssistantResponse,
        onEditMessage = { message -> if (message.isFromUser) messageToEdit = message },
        onStopGeneration = viewModel::stopGeneration,
        onPickImage = onPickImage,
        onTakePhoto = onTakePhoto,
        onRemoveImage = viewModel::clearPendingImage,
        onScrollToBottom = scrollToBottom,
        showSnackbar = showSnackbar,
    )

    ChatOverlays(
        uiState = uiState,
        renameDialogState = renameDialogState,
        onRenameSave = viewModel::renameChat,
        onRenameDismiss = { renameDialogState = null },
        deleteDialogId = deleteDialogId,
        onRequestDelete = { id -> deleteDialogId = id },
        onDeleteConfirm = { id ->
            viewModel.deleteChat(id)
            deleteDialogId = null
        },
        onDeleteDismiss = { deleteDialogId = null },
        renameTitleDialog = renameTitleDialog,
        onRenameTitleConfirm = { newName ->
            viewModel.renameCurrentChat(newName)
            renameTitleDialog = false
        },
        onRenameTitleDismiss = { renameTitleDialog = false },
        clearDialogVisible = showClearDialog,
        onClearConfirm = {
            viewModel.clearChat()
            showClearDialog = false
        },
        onClearDismiss = { showClearDialog = false },
    )

    ChatSettingsSheet(
        uiState = uiState,
        settingsVisibilityState = settingsVisibilityState,
        scrimInteractionSource = scrimInteractionSource,
        currentDestination = currentSettingsDestination,
        hasFineLocation = hasFine,
        hasCoarseLocation = hasCoarse,
        onRequestLocationPermissions = {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onDismiss = {
            showSettingsSheet = false
            currentSettingsDestination = null
        },
        onUpdateTemperature = viewModel::updateTemperature,
        onUpdateTopK = viewModel::updateTopK,
        onResetModelSettings = viewModel::resetModelSettings,
        onUpdateUserName = viewModel::updateUserName,
        onUpdatePersonalContext = viewModel::updatePersonalContextEnabled,
        onUpdateWebSearch = viewModel::updateWebSearchEnabled,
        onUpdateMultimodal = viewModel::updateMultimodalEnabled,
        onWipeAllChats = viewModel::wipeAllChats,
        onUpdateMemoryContext = viewModel::updateMemoryContextEnabled,
        onUpdateCustomInstructionsEnabled = viewModel::updateCustomInstructionsEnabled,
        onUpdateBioContext = viewModel::updateBioContextEnabled,
        onUpdateBioInformation = viewModel::updateBioInformation,
        onUpdateCustomInstructions = viewModel::updateCustomInstructions,
        onAddMemory = viewModel::addMemoryEntry,
        onUpdateMemory = viewModel::updateMemoryEntry,
        onDeleteMemory = viewModel::deleteMemoryEntry,
        onToggleMemory = viewModel::toggleMemoryEntry,
        onSaveBio = viewModel::saveBioInformation,
        onDeleteBio = viewModel::deleteBioInformation,
    )

    messageToEdit?.let { editingMessage ->
        var editedText by remember(editingMessage) { mutableStateOf(editingMessage.text) }
        val isConfirmEnabled = editedText.trim().isNotEmpty() && editedText != editingMessage.text
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sanitized = editedText.trim()
                        viewModel.editUserMessage(editingMessage.id, sanitized)
                        messageToEdit = null
                        showSnackbar("Message updated")
                    },
                    enabled = isConfirmEnabled,
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
