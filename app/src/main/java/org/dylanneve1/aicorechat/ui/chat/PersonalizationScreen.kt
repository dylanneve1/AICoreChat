package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size

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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // Header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Personalization Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        text = "Customize how the AI understands and responds to you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }


        // Feature Toggles
        FeatureToggleCard(
            icon = Icons.Outlined.LocationOn,
            title = "Personal Context",
            description = "Include time, device info, and location for more relevant responses",
            checked = personalContextEnabled,
            onCheckedChange = onPersonalContextToggle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FeatureToggleCard(
            icon = Icons.Outlined.Search,
            title = "Web Search",
            description = "Enable AI to search the web for current information",
            checked = webSearchEnabled,
            onCheckedChange = onWebSearchToggle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FeatureToggleCard(
            icon = Icons.Outlined.Image,
            title = "Image Analysis",
            description = "Allow AI to analyze and describe images you share",
            checked = multimodalEnabled,
            onCheckedChange = onMultimodalToggle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FeatureToggleCard(
            icon = Icons.Outlined.Memory,
            title = "Memory Context",
            description = "Include relevant memories in conversations",
            checked = memoryContextEnabled,
            onCheckedChange = onMemoryContextToggle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Bio Information Section
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bio Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            text = "Basic information about you for personalized responses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = bioContextEnabled,
                        onCheckedChange = onBioContextToggle
                    )
                }

                if (bioContextEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Name field (moved from separate section)
                    OutlinedTextField(
                        value = userName,
                        onValueChange = onUserNameChange,
                        label = { Text("Your Name") },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bioName,
                        onValueChange = onBioNameChange,
                        label = { Text("Full Name (optional)") },
                        placeholder = { Text("Your full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bioAge,
                            onValueChange = onBioAgeChange,
                            label = { Text("Age") },
                            placeholder = { Text("Your age") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = bioOccupation,
                            onValueChange = onBioOccupationChange,
                            label = { Text("Occupation") },
                            placeholder = { Text("Your job/role") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bioLocation,
                        onValueChange = onBioLocationChange,
                        label = { Text("Location") },
                        placeholder = { Text("City, Country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Custom Instructions Section
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Instructions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            text = "Instructions to tailor how the AI responds to you",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customInstructionsEnabled,
                        onCheckedChange = onCustomInstructionsToggle
                    )
                }

                if (customInstructionsEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customInstructions,
                        onValueChange = onCustomInstructionsChange,
                        label = { Text("Instructions") },
                        placeholder = { Text("e.g., Be concise, use simple language, be encouraging...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
