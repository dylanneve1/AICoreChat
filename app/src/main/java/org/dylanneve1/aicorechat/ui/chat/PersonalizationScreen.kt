package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.ui.components.FeatureToggleCard
import org.dylanneve1.aicorechat.ui.components.SectionHeaderCard

@Composable
fun PersonalizationScreen(
    userName: String,
    personalContextEnabled: Boolean,
    onUserNameChange: (String) -> Unit,
    onPersonalContextToggle: (Boolean) -> Unit,
    webSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
    multimodalEnabled: Boolean,
    onMultimodalToggle: (Boolean) -> Unit,
    memoryContextEnabled: Boolean,
    onMemoryContextToggle: (Boolean) -> Unit,
    customInstructionsEnabled: Boolean,
    onCustomInstructionsToggle: (Boolean) -> Unit,
    bioContextEnabled: Boolean,
    onBioContextToggle: (Boolean) -> Unit,
    // Bio information
    bioName: String,
    bioAge: String,
    bioOccupation: String,
    bioLocation: String,
    onBioNameChange: (String) -> Unit,
    onBioAgeChange: (String) -> Unit,
    onBioOccupationChange: (String) -> Unit,
    onBioLocationChange: (String) -> Unit,
    // Custom instructions
    customInstructions: String,
    onCustomInstructionsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding() // Add IME padding to handle keyboard
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Main Header
        SectionHeaderCard(
            icon = Icons.Outlined.Person,
            title = "Personalization",
            description = "Customize how the AI understands and responds to you",
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // AI Capabilities Section
        Text(
            text = "AI Capabilities",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureToggleCard(
                icon = Icons.Outlined.LocationOn,
                title = "Personal Context",
                description = "Share time, device info, and location for contextual responses",
                checked = personalContextEnabled,
                onCheckedChange = onPersonalContextToggle,
            )

            FeatureToggleCard(
                icon = Icons.Outlined.Search,
                title = "Web Search",
                description = "Allow searching the web for current information",
                checked = webSearchEnabled,
                onCheckedChange = onWebSearchToggle,
            )

            FeatureToggleCard(
                icon = Icons.Outlined.CameraAlt,
                title = "Image Analysis",
                description = "Enable AI to analyze and describe images",
                checked = multimodalEnabled,
                onCheckedChange = onMultimodalToggle,
            )

            FeatureToggleCard(
                icon = Icons.Outlined.Memory,
                title = "Memory Context",
                description = "Use saved memories to personalize responses",
                checked = memoryContextEnabled,
                onCheckedChange = onMemoryContextToggle,
            )
        }

        // Personal Information Section
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )

        // Bio Information Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (bioContextEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = if (bioContextEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biographical Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Help the AI understand who you are",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = bioContextEnabled,
                        onCheckedChange = onBioContextToggle,
                    )
                }

                AnimatedVisibility(
                    visible = bioContextEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = userName,
                            onValueChange = onUserNameChange,
                            label = { Text("Preferred Name") },
                            placeholder = { Text("How should I address you?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = bioAge,
                                onValueChange = { if (it.all { char -> char.isDigit() }) onBioAgeChange(it) },
                                label = { Text("Age") },
                                placeholder = { Text("Optional") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )

                            OutlinedTextField(
                                value = bioLocation,
                                onValueChange = onBioLocationChange,
                                label = { Text("Location") },
                                placeholder = { Text("City, Country") },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                            )
                        }

                        OutlinedTextField(
                            value = bioOccupation,
                            onValueChange = onBioOccupationChange,
                            label = { Text("Occupation") },
                            placeholder = { Text("What do you do? (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Custom Instructions Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (customInstructionsEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = if (customInstructionsEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Response Style",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Define how the AI should communicate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = customInstructionsEnabled,
                        onCheckedChange = onCustomInstructionsToggle,
                    )
                }

                AnimatedVisibility(
                    visible = customInstructionsEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    OutlinedTextField(
                        value = customInstructions,
                        onValueChange = onCustomInstructionsChange,
                        label = { Text("Custom Instructions") },
                        placeholder = { Text("e.g., Be concise and direct, use simple language, always be encouraging...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    // Scroll to make the field visible when focused
                                    coroutineScope.launch {
                                        delay(300) // Wait for keyboard to appear
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }
                            },
                        supportingText = {
                            Text(
                                text = "${customInstructions.length} characters",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
