package org.dylanneve1.aicorechat.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Support
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.CustomInstruction
import org.dylanneve1.aicorechat.data.MemoryEntry

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
            SettingsDestination.CustomInstructions -> destination = SettingsDestination.Main
            SettingsDestination.Main -> onDismiss()
        }
    })

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(when (destination) {
                        SettingsDestination.Main -> "Settings"
                        SettingsDestination.ModelSettings -> "Model Settings"
                        SettingsDestination.Personalization -> "Personalization"
                        SettingsDestination.Support -> "Support"
                        SettingsDestination.MemoryManagement -> "Memory Management"
                        SettingsDestination.CustomInstructions -> "Custom Instructions"
                    }) },
                    navigationIcon = {
                        if (destination != SettingsDestination.Main) {
                            IconButton(onClick = { destination = SettingsDestination.Main }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
                    }
                )
            }
        ) { innerPadding ->
            when (destination) {
                SettingsDestination.Main -> {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Settings Cards
                        SettingCard(
                            icon = Icons.Outlined.Tune,
                            title = "Model Settings",
                            description = "Fine-tune AI responses with temperature and Top-K controls",
                            onClick = { destination = SettingsDestination.ModelSettings }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        SettingCard(
                            icon = Icons.Outlined.Person,
                            title = "Personalization",
                            description = "Customize your experience with personal context and features",
                            onClick = { destination = SettingsDestination.Personalization }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        SettingCard(
                            icon = Icons.Outlined.Memory,
                            title = "Memory Management",
                            description = "Manage useful information the AI should remember",
                            onClick = { destination = SettingsDestination.MemoryManagement }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        SettingCard(
                            icon = Icons.Outlined.Support,
                            title = "Device Support",
                            description = "Check device compatibility and AICore status",
                            onClick = { destination = SettingsDestination.Support }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                        // Danger Zone
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Danger Zone",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "This action will permanently delete all chat conversations from your device. This cannot be undone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.FilledTonalButton(
                                    onClick = { confirmWipe = true },
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Delete All Chats")
                                }
                            }
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
                                dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } }
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
                        onResetModelSettings = onResetModelSettings
                    )
                }
                SettingsDestination.Personalization -> {
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
                        onCustomInstructionsChange = onCustomInstructionsChange
                    )
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
                        onDismiss = { destination = SettingsDestination.Main }
                    )
                }
                SettingsDestination.CustomInstructions -> {
                    CustomInstructionsScreen(
                        customInstructions = customInstructions,
                        onCustomInstructionsChange = onCustomInstructionsChange,
                        onDismiss = { destination = SettingsDestination.Main }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
