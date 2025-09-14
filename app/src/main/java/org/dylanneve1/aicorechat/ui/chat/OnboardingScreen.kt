package org.dylanneve1.aicorechat.ui.chat

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.dylanneve1.aicorechat.R

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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Welcome to AICore Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Private, on-device AI powered by Gemini Nano. Let's set up your personalized experience.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Basic Info Card
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Your name") },
                        placeholder = { Text("How should we address you?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Features Card
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "AI Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    FeatureSwitch(
                        title = "Personal Context",
                        subtitle = "Include time, device info, and location for more relevant responses",
                        checked = personalEnabled,
                        onCheckedChange = { checked ->
                            if (checked) requestLocationIfNeeded()
                            personalEnabled = checked
                        }
                    )

                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    FeatureSwitch(
                        title = "Web Search",
                        subtitle = "Allow AI to search the web for current information",
                        checked = webSearchEnabled,
                        onCheckedChange = { webSearchEnabled = it }
                    )

                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    FeatureSwitch(
                        title = "Image Analysis",
                        subtitle = "Enable AI to analyze and describe images you share",
                        checked = multimodalEnabled,
                        onCheckedChange = { multimodalEnabled = it }
                    )

                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    FeatureSwitch(
                        title = "Memory Context",
                        subtitle = "Include relevant memories in conversations (recommended)",
                        checked = memoryContextEnabled,
                        onCheckedChange = { memoryContextEnabled = it }
                    )
                }
            }

            // Bio Information Card
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        androidx.compose.material3.Text(
                            "Bio Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.Switch(
                            checked = bioContextEnabled,
                            onCheckedChange = { bioContextEnabled = it }
                        )
                    }

                    androidx.compose.material3.Text(
                        "Help the AI understand you better with biographical information.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (bioContextEnabled) {
                        OutlinedTextField(
                            value = bioName,
                            onValueChange = { bioName = it },
                            singleLine = true,
                            label = { Text("Full Name") },
                            placeholder = { Text("Your full name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = bioAge,
                                onValueChange = { bioAge = it },
                                singleLine = true,
                                label = { Text("Age") },
                                placeholder = { Text("Your age") },
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

                        Spacer(modifier = Modifier.height(12.dp))

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

            // Custom Instructions Card
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        androidx.compose.material3.Text(
                            "Custom Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.Switch(
                            checked = customInstructionsEnabled,
                            onCheckedChange = { customInstructionsEnabled = it }
                        )
                    }

                    androidx.compose.material3.Text(
                        "Set specific instructions for how the AI should respond to you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (customInstructionsEnabled) {
                        OutlinedTextField(
                            value = customInstructions,
                            onValueChange = { customInstructions = it },
                            minLines = 3,
                            maxLines = 5,
                            label = { Text("Instructions") },
                            placeholder = { Text("e.g., Be concise, use simple language, be encouraging...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Get Started Button
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
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RowWithSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RowHorizontal(title = title, checked = checked, onCheckedChange = onCheckedChange)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RowHorizontal(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FeatureSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
} 