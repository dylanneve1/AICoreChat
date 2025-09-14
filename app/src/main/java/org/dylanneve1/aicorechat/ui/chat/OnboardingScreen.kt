package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.dylanneve1.aicorechat.R
import org.dylanneve1.aicorechat.ui.components.FeatureToggleCard
import org.dylanneve1.aicorechat.ui.components.SectionHeaderCard

@Composable
fun OnboardingScreen(
    initialName: String,
    initialPersonalContextEnabled: Boolean,
    onComplete: (
        name: String,
        personalContextEnabled: Boolean,
        webSearchEnabled: Boolean,
        multimodalEnabled: Boolean,
        memoryContextEnabled: Boolean,
        bioContextEnabled: Boolean,
        bioName: String,
        bioAge: String,
        bioOccupation: String,
        bioLocation: String,
        customInstructions: String,
        customInstructionsEnabled: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var personalEnabled by remember { mutableStateOf(initialPersonalContextEnabled) }
    var webSearchEnabled by remember { mutableStateOf(false) }
    var multimodalEnabled by remember { mutableStateOf(false) }
    var memoryContextEnabled by remember { mutableStateOf(true) }
    var bioContextEnabled by remember { mutableStateOf(false) }
    var bioName by remember { mutableStateOf("") }
    var bioAge by remember { mutableStateOf("") }
    var bioOccupation by remember { mutableStateOf("") }
    var bioLocation by remember { mutableStateOf("") }
    var customInstructions by remember { mutableStateOf("") }
    var customInstructionsEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    fun requestLocationIfNeeded() {
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RocketLaunch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Welcome to AICore Chat",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Experience private, on-device AI powered by Gemini Nano",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Your Name Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Let's get to know you",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Your name") },
                        placeholder = { Text("How should the AI address you?") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }

            // AI Features Section
            Text(
                text = "AI Capabilities",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureToggleCard(
                    icon = Icons.Outlined.LocationOn,
                    title = "Personal Context",
                    description = "Share time, device, and location for contextual responses",
                    checked = personalEnabled,
                    onCheckedChange = { checked ->
                        if (checked) requestLocationIfNeeded()
                        personalEnabled = checked
                    }
                )

                FeatureToggleCard(
                    icon = Icons.Outlined.Search,
                    title = "Web Search",
                    description = "Search the web for current information",
                    checked = webSearchEnabled,
                    onCheckedChange = { webSearchEnabled = it }
                )

                FeatureToggleCard(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Image Analysis",
                    description = "Analyze and describe images you share",
                    checked = multimodalEnabled,
                    onCheckedChange = { multimodalEnabled = it }
                )

                FeatureToggleCard(
                    icon = Icons.Outlined.Memory,
                    title = "Memory Context",
                    description = "Use saved memories to personalize responses",
                    checked = memoryContextEnabled,
                    onCheckedChange = { memoryContextEnabled = it }
                )
            }

            // Personal Information Section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personal Information (Optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            // Bio Information Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (bioContextEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            tint = if (bioContextEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biographical Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Help the AI understand who you are",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bioContextEnabled,
                            onCheckedChange = { bioContextEnabled = it }
                        )
                    }

                    AnimatedVisibility(
                        visible = bioContextEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = bioName,
                                onValueChange = { bioName = it },
                                singleLine = true,
                                label = { Text("Full Name") },
                                placeholder = { Text("Your full name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = bioAge,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() }) {
                                            bioAge = newValue
                                        }
                                    },
                                    singleLine = true,
                                    label = { Text("Age") },
                                    placeholder = { Text("Age") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = bioOccupation,
                                    onValueChange = { bioOccupation = it },
                                    singleLine = true,
                                    label = { Text("Occupation") },
                                    placeholder = { Text("Your job/role") },
                                    modifier = Modifier.weight(2f)
                                )
                            }

                            OutlinedTextField(
                                value = bioLocation,
                                onValueChange = { bioLocation = it },
                                singleLine = true,
                                label = { Text("Location") },
                                placeholder = { Text("City, Country") },
                                modifier = Modifier.fillMaxWidth()
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
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = if (customInstructionsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Response Style",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Customize how the AI communicates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = customInstructionsEnabled,
                            onCheckedChange = { customInstructionsEnabled = it }
                        )
                    }

                    AnimatedVisibility(
                        visible = customInstructionsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = customInstructions,
                            onValueChange = { customInstructions = it },
                            minLines = 3,
                            maxLines = 5,
                            label = { Text("Custom Instructions") },
                            placeholder = { Text("e.g., Be concise and direct, use simple language, always be encouraging...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            supportingText = {
                                Text(
                                    text = "${customInstructions.length} characters",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (personalEnabled) requestLocationIfNeeded()
                    onComplete(
                        name.trim(),
                        personalEnabled,
                        webSearchEnabled,
                        multimodalEnabled,
                        memoryContextEnabled,
                        bioContextEnabled,
                        bioName.trim(),
                        bioAge.trim(),
                        bioOccupation.trim(),
                        bioLocation.trim(),
                        customInstructions.trim(),
                        customInstructionsEnabled
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Outlined.RocketLaunch,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Chatting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Privacy Note
            Text(
                text = "All data stays on your device. You can change these settings anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
