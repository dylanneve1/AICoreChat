package org.dylanneve1.aicorechat.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.ui.components.InfoCard
import org.dylanneve1.aicorechat.ui.components.SettingsNavigationCard

enum class SettingsDestination { Main, ModelSettings, Personalization, Support, MemoryManagement, CustomInstructions }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    temperature: Float,
    topK: Int,
    onTemperatureChange: (Float) -> Unit,
    onTopKChange: (Int) -> Unit,
    onResetModelSettings: () -> Unit,
    userName: String,
    personalContextEnabled: Boolean,
    onUserNameChange: (String) -> Unit,
    onPersonalContextToggle: (Boolean) -> Unit,
    webSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
    multimodalEnabled: Boolean,
    onMultimodalToggle: (Boolean) -> Unit,
    onWipeAllChats: () -> Unit,
    onDismiss: () -> Unit,
    // Memory and Bio parameters
    memoryContextEnabled: Boolean = true,
    onMemoryContextToggle: (Boolean) -> Unit = {},
    customInstructionsEnabled: Boolean = true,
    onCustomInstructionsToggle: (Boolean) -> Unit = {},
    bioContextEnabled: Boolean = true,
    onBioContextToggle: (Boolean) -> Unit = {},
    // Bio information
    bioName: String = "",
    bioAge: String = "",
    bioOccupation: String = "",
    bioLocation: String = "",
    onBioNameChange: (String) -> Unit = {},
    onBioAgeChange: (String) -> Unit = {},
    onBioOccupationChange: (String) -> Unit = {},
    onBioLocationChange: (String) -> Unit = {},
    // Custom instructions
    customInstructions: String = "",
    onCustomInstructionsChange: (String) -> Unit = {},
    // Memory and Bio management parameters
    memoryEntries: List<MemoryEntry> = emptyList(),
    bioInformation: BioInformation? = null,
    onAddMemory: (String) -> Unit = {},
    onUpdateMemory: (MemoryEntry) -> Unit = {},
    onDeleteMemory: (String) -> Unit = {},
    onToggleMemory: (String) -> Unit = {},
    onSaveBio: (BioInformation) -> Unit = {},
    onDeleteBio: () -> Unit = {},
) {
    var confirmWipe by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf(SettingsDestination.Main) }

    BackHandler(onBack = {
        when (destination) {
            SettingsDestination.ModelSettings,
            SettingsDestination.Personalization,
            SettingsDestination.Support,
            SettingsDestination.MemoryManagement,
            SettingsDestination.CustomInstructions,
            -> destination = SettingsDestination.Main
            SettingsDestination.Main -> onDismiss()
        }
    })

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (destination) {
                                SettingsDestination.Main -> "Settings"
                                SettingsDestination.ModelSettings -> "Model Settings"
                                SettingsDestination.Personalization -> "Personalization"
                                SettingsDestination.Support -> "Device Support"
                                SettingsDestination.MemoryManagement -> "Memory Management"
                                SettingsDestination.CustomInstructions -> "Custom Instructions"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        if (destination != SettingsDestination.Main) {
                            IconButton(onClick = { destination = SettingsDestination.Main }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        } else {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { innerPadding ->
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val offset = { fullWidth: Int -> if (forward) fullWidth else -fullWidth }
                    (
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 260),
                            initialOffsetX = offset,
                        ) + fadeIn(animationSpec = tween(durationMillis = 200))
                        ) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 240),
                            targetOffsetX = offset,
                        ) + fadeOut(animationSpec = tween(durationMillis = 180))
                        )
                },
                label = "SettingsDestinationTransition",
                modifier = Modifier.fillMaxSize(),
            ) { currentDestination ->
                when (currentDestination) {
                    SettingsDestination.Main -> {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            // Settings Categories
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // AI Configuration Section
                                Text(
                                    text = "AI Configuration",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                )

                                SettingsNavigationCard(
                                    icon = Icons.Outlined.Tune,
                                    title = "Model Settings",
                                    description = "Adjust temperature and Top-K for response creativity",
                                    onClick = { destination = SettingsDestination.ModelSettings },
                                )

                                SettingsNavigationCard(
                                    icon = Icons.Outlined.Person,
                                    title = "Personalization",
                                    description = "Personal context, bio, and custom instructions",
                                    onClick = { destination = SettingsDestination.Personalization },
                                )

                                SettingsNavigationCard(
                                    icon = Icons.Outlined.Memory,
                                    title = "Memory Management",
                                    description = "Manage what the AI remembers about you",
                                    onClick = { destination = SettingsDestination.MemoryManagement },
                                )

                                // System Section
                                Text(
                                    text = "System",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                                )

                                SettingsNavigationCard(
                                    icon = Icons.Outlined.PhoneAndroid,
                                    title = "Device Support",
                                    description = "Check AICore compatibility and status",
                                    onClick = { destination = SettingsDestination.Support },
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Danger Zone Section
                                Text(
                                    text = "Danger Zone",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                                )

                                InfoCard(
                                    icon = Icons.Outlined.Warning,
                                    title = "Delete All Chats",
                                    description = "Permanently remove all chat conversations from your device. This action cannot be undone.",
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error,
                                )

                                FilledTonalButton(
                                    onClick = { confirmWipe = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Wipe all chats")
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            if (confirmWipe) {
                                AlertDialog(
                                    onDismissRequest = { confirmWipe = false },
                                    title = { Text("Wipe all chats?") },
                                    text = { Text("This cannot be undone.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            confirmWipe = false
                                            onWipeAllChats()
                                            onDismiss()
                                        }) { Text("Wipe") }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { confirmWipe = false },
                                        ) { Text("Cancel") }
                                    },
                                )
                            }
                        }
                    }
                    SettingsDestination.ModelSettings -> {
                        ModelSettingsScreen(
                            temperature = temperature,
                            topK = topK,
                            onTemperatureChange = onTemperatureChange,
                            onTopKChange = onTopKChange,
                            onResetModelSettings = onResetModelSettings,
                        )
                    }
                    SettingsDestination.Personalization -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            PersonalizationScreen(
                                userName = userName,
                                personalContextEnabled = personalContextEnabled,
                                onUserNameChange = onUserNameChange,
                                onPersonalContextToggle = onPersonalContextToggle,
                                webSearchEnabled = webSearchEnabled,
                                onWebSearchToggle = onWebSearchToggle,
                                multimodalEnabled = multimodalEnabled,
                                onMultimodalToggle = onMultimodalToggle,
                                memoryContextEnabled = memoryContextEnabled,
                                onMemoryContextToggle = onMemoryContextToggle,
                                customInstructionsEnabled = customInstructionsEnabled,
                                onCustomInstructionsToggle = onCustomInstructionsToggle,
                                bioContextEnabled = bioContextEnabled,
                                onBioContextToggle = onBioContextToggle,
                                // Bio information
                                bioName = bioName,
                                bioAge = bioAge,
                                bioOccupation = bioOccupation,
                                bioLocation = bioLocation,
                                onBioNameChange = onBioNameChange,
                                onBioAgeChange = onBioAgeChange,
                                onBioOccupationChange = onBioOccupationChange,
                                onBioLocationChange = onBioLocationChange,
                                // Custom instructions
                                customInstructions = customInstructions,
                                onCustomInstructionsChange = onCustomInstructionsChange,
                            )
                        }
                    }
                    SettingsDestination.Support -> {
                        SupportScreen()
                    }
                    SettingsDestination.MemoryManagement -> {
                        MemoryManagementScreen(
                            memoryEntries = memoryEntries,
                            onAddMemory = onAddMemory,
                            onUpdateMemory = onUpdateMemory,
                            onDeleteMemory = onDeleteMemory,
                            onToggleMemory = onToggleMemory,
                            onDismiss = { destination = SettingsDestination.Main },
                        )
                    }
                    SettingsDestination.CustomInstructions -> {
                        CustomInstructionsScreen(
                            customInstructions = customInstructions,
                            onCustomInstructionsChange = onCustomInstructionsChange,
                            onDismiss = { destination = SettingsDestination.Main },
                        )
                    }
                }
            }
        }
    }
}
