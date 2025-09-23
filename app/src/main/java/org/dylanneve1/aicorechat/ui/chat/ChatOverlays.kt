package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState

@Composable
fun ChatOverlays(
    uiState: ChatUiState,
    renameDialogState: Pair<Long, String>?,
    onRenameSave: (Long, String) -> Unit,
    onRenameDismiss: () -> Unit,
    deleteDialogId: Long?,
    onRequestDelete: (Long) -> Unit,
    onDeleteConfirm: (Long) -> Unit,
    onDeleteDismiss: () -> Unit,
    renameTitleDialog: Boolean,
    onRenameTitleConfirm: (String) -> Unit,
    onRenameTitleDismiss: () -> Unit,
    clearDialogVisible: Boolean,
    onClearConfirm: () -> Unit,
    onClearDismiss: () -> Unit,
) {
    if (uiState.isTitleGenerating) {
        GeneratingDialog(
            title = "Generating title",
            loadingLabel = "Using chat context…",
        )
    }

    if (uiState.isBulkTitleGenerating) {
        GeneratingDialog(
            title = "Generating titles",
            loadingLabel = "Processing all chats…",
        )
    }

    renameDialogState?.let { (id, currentName) ->
        var name by remember(currentName) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = onRenameDismiss,
            title = { Text("Chat options") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Rename chat") },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            val trimmed = name.trim().ifEmpty { "New Chat" }
                            onRenameSave(id, trimmed)
                            onRenameDismiss()
                        }) { Text("Save") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            onRenameDismiss()
                            onRequestDelete(id)
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onRenameDismiss) { Text("Close") } },
        )
    }

    deleteDialogId?.let { deleteId ->
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Delete chat?") },
            text = { Text("This will permanently remove this chat.") },
            confirmButton = {
                TextButton(onClick = { onDeleteConfirm(deleteId) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDeleteDismiss) { Text("Cancel") } },
        )
    }

    if (renameTitleDialog) {
        var name by remember(uiState.currentSessionName) { mutableStateOf(uiState.currentSessionName) }
        AlertDialog(
            onDismissRequest = onRenameTitleDismiss,
            title = { Text("Rename chat") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = name.trim().ifEmpty { "New Chat" }
                    onRenameTitleConfirm(trimmed)
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onRenameTitleDismiss) { Text("Cancel") } },
        )
    }

    if (clearDialogVisible) {
        AlertDialog(
            onDismissRequest = onClearDismiss,
            title = { Text("Clear conversation?") },
            text = { Text("This will remove this chat completely and start a new one.") },
            confirmButton = {
                TextButton(onClick = onClearConfirm) { Text("Delete chat") }
            },
            dismissButton = { TextButton(onClick = onClearDismiss) { Text("Cancel") } },
        )
    }
}

@Composable
fun ChatSettingsSheet(
    uiState: ChatUiState,
    settingsVisibilityState: MutableTransitionState<Boolean>,
    scrimInteractionSource: MutableInteractionSource,
    currentDestination: SettingsDestination?,
    hasFineLocation: Boolean,
    hasCoarseLocation: Boolean,
    onRequestLocationPermissions: () -> Unit,
    onDismiss: () -> Unit,
    onUpdateTemperature: (Float) -> Unit,
    onUpdateTopK: (Int) -> Unit,
    onResetModelSettings: () -> Unit,
    onUpdateUserName: (String) -> Unit,
    onUpdatePersonalContext: (Boolean) -> Unit,
    onUpdateWebSearch: (Boolean) -> Unit,
    onUpdateMultimodal: (Boolean) -> Unit,
    onWipeAllChats: () -> Unit,
    onUpdateMemoryContext: (Boolean) -> Unit,
    onUpdateCustomInstructionsEnabled: (Boolean) -> Unit,
    onUpdateBioContext: (Boolean) -> Unit,
    onUpdateBioInformation: (String, String, String, String) -> Unit,
    onUpdateCustomInstructions: (String, Boolean) -> Unit,
    onAddMemory: (String) -> Unit,
    onUpdateMemory: (MemoryEntry) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onToggleMemory: (String) -> Unit,
    onSaveBio: (BioInformation) -> Unit,
    onDeleteBio: () -> Unit,
) {
    AnimatedVisibility(
        visibleState = settingsVisibilityState,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = fadeOut(animationSpec = tween(durationMillis = 150)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = scrimInteractionSource,
                    ) { onDismiss() },
            )

            AnimatedVisibility(
                visibleState = settingsVisibilityState,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 260),
                    initialOffsetY = { fullHeight -> fullHeight },
                ) + fadeIn(animationSpec = tween(durationMillis = 240)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 220),
                    targetOffsetY = { fullHeight -> fullHeight },
                ) + fadeOut(animationSpec = tween(durationMillis = 200)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (currentDestination) {
                        null -> {
                            SettingsScreen(
                                temperature = uiState.temperature,
                                topK = uiState.topK,
                                onTemperatureChange = onUpdateTemperature,
                                onTopKChange = onUpdateTopK,
                                onResetModelSettings = onResetModelSettings,
                                userName = uiState.userName,
                                personalContextEnabled = uiState.personalContextEnabled,
                                onUserNameChange = onUpdateUserName,
                                onPersonalContextToggle = { enabled ->
                                    if (enabled && !hasFineLocation && !hasCoarseLocation) {
                                        onRequestLocationPermissions()
                                    }
                                    onUpdatePersonalContext(enabled)
                                },
                                webSearchEnabled = uiState.webSearchEnabled,
                                onWebSearchToggle = onUpdateWebSearch,
                                multimodalEnabled = uiState.multimodalEnabled,
                                onMultimodalToggle = onUpdateMultimodal,
                                onWipeAllChats = onWipeAllChats,
                                onDismiss = onDismiss,
                                memoryContextEnabled = uiState.memoryContextEnabled,
                                onMemoryContextToggle = onUpdateMemoryContext,
                                customInstructionsEnabled = uiState.customInstructionsEnabled,
                                onCustomInstructionsToggle = onUpdateCustomInstructionsEnabled,
                                bioContextEnabled = uiState.bioContextEnabled,
                                onBioContextToggle = onUpdateBioContext,
                                bioName = uiState.bioInformation?.name ?: "",
                                bioAge = uiState.bioInformation?.age?.toString() ?: "",
                                bioOccupation = uiState.bioInformation?.occupation ?: "",
                                bioLocation = uiState.bioInformation?.location ?: "",
                                onBioNameChange = { name ->
                                    val currentBio = uiState.bioInformation
                                    onUpdateBioInformation(
                                        name,
                                        currentBio?.age?.toString() ?: "",
                                        currentBio?.occupation ?: "",
                                        currentBio?.location ?: "",
                                    )
                                },
                                onBioAgeChange = { age ->
                                    val currentBio = uiState.bioInformation
                                    onUpdateBioInformation(
                                        currentBio?.name ?: "",
                                        age,
                                        currentBio?.occupation ?: "",
                                        currentBio?.location ?: "",
                                    )
                                },
                                onBioOccupationChange = { occupation ->
                                    val currentBio = uiState.bioInformation
                                    onUpdateBioInformation(
                                        currentBio?.name ?: "",
                                        currentBio?.age?.toString() ?: "",
                                        occupation,
                                        currentBio?.location ?: "",
                                    )
                                },
                                onBioLocationChange = { location ->
                                    val currentBio = uiState.bioInformation
                                    onUpdateBioInformation(
                                        currentBio?.name ?: "",
                                        currentBio?.age?.toString() ?: "",
                                        currentBio?.occupation ?: "",
                                        location,
                                    )
                                },
                                customInstructions = uiState.customInstructions,
                                onCustomInstructionsChange = { instructions ->
                                    onUpdateCustomInstructions(
                                        instructions,
                                        uiState.customInstructionsEnabled,
                                    )
                                },
                                memoryEntries = uiState.memoryEntries,
                                bioInformation = uiState.bioInformation,
                                onAddMemory = onAddMemory,
                                onUpdateMemory = onUpdateMemory,
                                onDeleteMemory = onDeleteMemory,
                                onToggleMemory = onToggleMemory,
                                onSaveBio = onSaveBio,
                                onDeleteBio = onDeleteBio,
                            )
                        }

                        else -> onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratingDialog(title: String, loadingLabel: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LoadingRow(label = loadingLabel)
            }
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp,
        )
        Text(
            label,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
