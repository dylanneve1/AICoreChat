package org.dylanneve1.aicorechat.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dylanneve1.aicorechat.util.DeviceSupportStatus
import org.dylanneve1.aicorechat.util.checkDeviceSupport
import org.dylanneve1.aicorechat.util.isAICoreInstalled

enum class SettingsDestination { Main, Personalization, Support }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    temperature: Float,
    topK: Int,
    onTemperatureChange: (Float) -> Unit,
    onTopKChange: (Int) -> Unit,
    userName: String,
    personalContextEnabled: Boolean,
    onUserNameChange: (String) -> Unit,
    onPersonalContextToggle: (Boolean) -> Unit,
    webSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
    multimodalEnabled: Boolean,
    onMultimodalToggle: (Boolean) -> Unit,
    onWipeAllChats: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val githubUrl = "https://github.com/dylanneve1/AICoreChat"
    val githubIntent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)) }
    var confirmWipe by remember { mutableStateOf(false) }

    // Simple in-component nav state
    var destination by remember { mutableStateOf(SettingsDestination.Main) }

    BackHandler(onBack = {
        if (destination != SettingsDestination.Main) destination = SettingsDestination.Main else onDismiss()
    })

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(when (destination) {
                        SettingsDestination.Main -> "Settings"
                        SettingsDestination.Personalization -> "Personalization"
                        SettingsDestination.Support -> "Support"
                    }) },
                    navigationIcon = {
                        if (destination != SettingsDestination.Main) {
                            IconButton(onClick = { destination = SettingsDestination.Main }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
                            .padding(horizontal = 24.dp)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Model Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
                        )

                        SettingSlider(
                            label = "Temperature",
                            value = temperature,
                            valueRange = 0f..1f,
                            onValueChange = onTemperatureChange,
                            valueLabel = "%.2f".format(temperature)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingSlider(
                            label = "Top-K",
                            value = topK.toFloat(),
                            valueRange = 1f..100f,
                            onValueChange = { onTopKChange(it.roundToInt()) },
                            valueLabel = topK.toString()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Navigation rows
                        Text(
                            text = "Sections",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destination = SettingsDestination.Personalization }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Personalization", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Name, Personal Context, Web Search, Multimodal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destination = SettingsDestination.Support }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Support", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Device support checks, AICore status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Wipe all chats from this device.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { confirmWipe = true }) { Text("Wipe all chats", color = MaterialTheme.colorScheme.error) }
                        }

                        if (confirmWipe) {
                            androidx.compose.material3.AlertDialog(
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "AICore Chat demonstrates on-device Gemini Nano via the AICore SDK. Chats can be renamed, organized, and titled automatically.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Personal Context is optional and only used locally to help the model respond more accurately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = { context.startActivity(githubIntent) }) {
                            Text("Source Code on GitHub")
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = "Open in new window",
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                    )
                }
                SettingsDestination.Support -> {
                    SupportScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizationScreen(
    userName: String,
    personalContextEnabled: Boolean,
    onUserNameChange: (String) -> Unit,
    onPersonalContextToggle: (Boolean) -> Unit,
    webSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
    multimodalEnabled: Boolean,
    onMultimodalToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChange,
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Personal Context", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Adds current time, device, locale, and (if permitted) location to the start of each chat to improve responses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = personalContextEnabled, onCheckedChange = onPersonalContextToggle)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Web Search", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Allow the assistant to trigger a one-shot web search using tool calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = webSearchEnabled, onCheckedChange = onWebSearchToggle)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Multimodal (images)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Generates short descriptions for attached images and adds them to the chat context.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = multimodalEnabled, onCheckedChange = onMultimodalToggle)
        }
    }
}

@Composable
private fun SupportScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var supportStatus by remember { mutableStateOf<DeviceSupportStatus?>(null) }
    var aicoreInstalled by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Checking…") }

    LaunchedEffect(Unit) {
        aicoreInstalled = isAICoreInstalled(context)
        val status = checkDeviceSupport(context)
        supportStatus = status
        statusText = when (status) {
            is DeviceSupportStatus.Supported -> "Supported"
            is DeviceSupportStatus.AICoreMissing -> "AICore app missing"
            is DeviceSupportStatus.NotReady -> status.reason ?: "Not ready"
            null -> "Unknown"
        }
    }

    Column(modifier = modifier) {
        Text("Device Support: $statusText", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "AICore Installed: ${if (aicoreInstalled) "Yes" else "No"}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}
