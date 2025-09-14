package org.dylanneve1.aicorechat.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Support
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.ui.components.SectionHeaderCard
import org.dylanneve1.aicorechat.util.DeviceSupportStatus
import org.dylanneve1.aicorechat.util.checkDeviceSupport
import org.dylanneve1.aicorechat.util.isAICoreInstalled

@Composable
fun SupportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val githubUrl = "https://github.com/dylanneve1/AICoreChat"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.google.android.aicore"

    var supportStatus by remember { mutableStateOf<DeviceSupportStatus?>(null) }
    var aicoreInstalled by remember { mutableStateOf(false) }
    var isSupported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        aicoreInstalled = isAICoreInstalled(context)
        val status = checkDeviceSupport(context)
        supportStatus = status
        isSupported = status is DeviceSupportStatus.Supported
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        SectionHeaderCard(
            icon = Icons.Outlined.PhoneAndroid,
            title = "Device Support",
            description = "Check compatibility with on-device AI processing",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Compatibility Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (supportStatus) {
                    is DeviceSupportStatus.Supported -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                    is DeviceSupportStatus.AICoreMissing, is DeviceSupportStatus.NotReady -> 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
                    null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    when (supportStatus) {
                                        is DeviceSupportStatus.Supported -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        is DeviceSupportStatus.AICoreMissing, is DeviceSupportStatus.NotReady -> 
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (supportStatus) {
                                is DeviceSupportStatus.Supported -> Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                is DeviceSupportStatus.AICoreMissing, is DeviceSupportStatus.NotReady -> Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                null -> CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (supportStatus) {
                                    is DeviceSupportStatus.Supported -> "Fully Compatible"
                                    is DeviceSupportStatus.AICoreMissing -> "AICore Missing"
                                    is DeviceSupportStatus.NotReady -> "Not Ready"
                                    null -> "Checking..."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (supportStatus) {
                                    is DeviceSupportStatus.Supported -> MaterialTheme.colorScheme.primary
                                    is DeviceSupportStatus.AICoreMissing, is DeviceSupportStatus.NotReady -> 
                                        MaterialTheme.colorScheme.error
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = when (supportStatus) {
                                    is DeviceSupportStatus.Supported -> "Ready for AI processing"
                                    is DeviceSupportStatus.AICoreMissing -> "Install required component"
                                    is DeviceSupportStatus.NotReady -> 
                                        (supportStatus as? DeviceSupportStatus.NotReady)?.reason ?: "Setup required"
                                    null -> "Verifying device capabilities"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (supportStatus != null && supportStatus !is DeviceSupportStatus.Supported) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (supportStatus is DeviceSupportStatus.AICoreMissing) {
                        Button(
                            onClick = { 
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install AICore")
                        }
                    }
                }
            }
        }

        // System Requirements
        Text(
            text = "System Requirements",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                RequirementItem(
                    icon = Icons.Outlined.Android,
                    title = "AICore App",
                    status = if (aicoreInstalled) "Installed" else "Not Installed",
                    isValid = aicoreInstalled
                )
                Spacer(modifier = Modifier.height(12.dp))
                RequirementItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Android 14+",
                    status = "Required",
                    isValid = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                )
                Spacer(modifier = Modifier.height(12.dp))
                RequirementItem(
                    icon = Icons.Outlined.Memory,
                    title = "Gemini Nano",
                    status = "On-device model",
                    isValid = isSupported
                )
            }
        }

        
        // About Section
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "About",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AICore Chat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Experience private, on-device AI conversations powered by Gemini Nano.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureHighlight(
                        icon = Icons.Outlined.Security,
                        text = "100% private - all processing on-device"
                    )
                    FeatureHighlight(
                        icon = Icons.Outlined.Speed,
                        text = "Fast, offline AI responses"
                    )
                    FeatureHighlight(
                        icon = Icons.Outlined.Memory,
                        text = "Smart memory and context awareness"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View on GitHub")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RequirementItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    isValid: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isValid) Icons.Outlined.CheckCircle else Icons.Outlined.Close,
            contentDescription = null,
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FeatureHighlight(
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
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
