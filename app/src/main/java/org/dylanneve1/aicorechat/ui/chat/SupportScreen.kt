package org.dylanneve1.aicorechat.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Support
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.util.DeviceSupportStatus
import org.dylanneve1.aicorechat.util.checkDeviceSupport
import org.dylanneve1.aicorechat.util.isAICoreInstalled

@Composable
fun SupportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val githubUrl = "https://github.com/dylanneve1/AICoreChat"
    val githubIntent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)) }

    var supportStatus by remember { mutableStateOf<DeviceSupportStatus?>(null) }
    var aicoreInstalled by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Checking…") }
    var isSupported by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        aicoreInstalled = isAICoreInstalled(context)
        val status = checkDeviceSupport(context)
        supportStatus = status
        when (status) {
            is DeviceSupportStatus.Supported -> {
                statusText = "Fully Supported"
                isSupported = true
            }
            is DeviceSupportStatus.AICoreMissing -> {
                statusText = "AICore App Missing"
                isSupported = false
            }
            is DeviceSupportStatus.NotReady -> {
                statusText = status.reason ?: "Not Ready"
                isSupported = false
            }
            null -> {
                statusText = "Checking Compatibility..."
                isSupported = false
            }
        }
    }

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
                Icon(
                    imageVector = Icons.Outlined.Support,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Compatibility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Check if your device supports on-device AI processing with Gemini Nano.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Device Status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSupported)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
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
                    Icon(
                        imageVector = if (isSupported) Icons.Outlined.Info else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (isSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Device Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                when (supportStatus) {
                    is DeviceSupportStatus.Supported -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusItem(Icons.Outlined.Info, "Your device fully supports on-device AI processing")
                            StatusItem(Icons.Outlined.AutoAwesome, "Gemini Nano model available")
                            StatusItem(Icons.Outlined.Support, "Optimal performance expected")
                        }
                    }
                    is DeviceSupportStatus.AICoreMissing -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusItem(Icons.Outlined.Warning, "Google AICore app is required for on-device AI")
                            StatusItem(Icons.Outlined.Info, "Install AICore from Google Play Store")
                            StatusItem(Icons.Outlined.Shield, "Restart AICore Chat after installation")
                        }
                    }
                    is DeviceSupportStatus.NotReady -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusItem(Icons.Outlined.Warning, "Device compatibility check in progress")
                            StatusItem(Icons.Outlined.Info, "This may take a moment")
                            StatusItem(Icons.Outlined.Support, "Ensure AICore is properly installed")
                        }
                    }
                    null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusItem(Icons.Outlined.Info, "Checking device compatibility...")
                            StatusItem(Icons.Outlined.Info, "This may take a few seconds")
                        }
                    }
                }
            }
        }

        // AICore Status
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
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AICore Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "AICore App Installed",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (aicoreInstalled) Icons.Outlined.Info else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (aicoreInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (aicoreInstalled) "Installed" else "Not Installed",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (aicoreInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (!aicoreInstalled) {
                    Text(
                        text = "The Google AICore app is essential for on-device AI processing and must be installed from the Google Play Store.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // About Section
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
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About AICore Chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Experience cutting-edge AI technology with on-device Gemini Nano powered by Google's AICore SDK. Enjoy intelligent conversations that respect your privacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Outlined.AutoAwesome,
                        text = "Smart chat organization and automatic titling"
                    )
                    FeatureItem(
                        icon = Icons.Outlined.Shield,
                        text = "Privacy-first with local processing"
                    )
                    FeatureItem(
                        icon = Icons.Outlined.Info,
                        text = "Personal context for better responses"
                    )
                }
                FilledTonalButton(
                    onClick = { context.startActivity(githubIntent) },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("View Source Code")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Open GitHub",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp).padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
