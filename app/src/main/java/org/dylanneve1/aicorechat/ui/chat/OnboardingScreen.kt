package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.dylanneve1.aicorechat.R
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew

@Composable
fun OnboardingScreen(
    initialName: String,
    initialPersonalContextEnabled: Boolean,
    onComplete: (name: String, personalContextEnabled: Boolean, webSearchEnabled: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var personalEnabled by remember { mutableStateOf(initialPersonalContextEnabled) }
    var webSearchEnabled by remember { mutableStateOf(false) }

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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Text("Welcome to AICore Chat", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Private, on-device AI powered by Gemini Nano. Personalize with optional context for better responses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "To use Gemini Nano, you must join the Google Group and then enroll in the AICore testing program.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val groupUrl = "https://groups.google.com/g/aicore-experimental"
            val testingUrl = "https://play.google.com/apps/testing/com.google.android.aicore"
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(groupUrl))) }, modifier = Modifier.fillMaxWidth()) {
                Text("Join Google Group (AICore Experimental)")
                Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
            }
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(testingUrl))) }, modifier = Modifier.fillMaxWidth()) {
                Text("Join AICore Testing Program")
                Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Your name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            RowWithSwitch(
                title = "Enable Personal Context",
                subtitle = "Adds time, device, locale, and (if permitted) location to chats.",
                checked = personalEnabled,
                onCheckedChange = { checked -> if (checked) requestLocationIfNeeded(); personalEnabled = checked }
            )
            RowWithSwitch(
                title = "Enable Web Search",
                subtitle = "Allow tool calls to fetch fresh info.",
                checked = webSearchEnabled,
                onCheckedChange = { webSearchEnabled = it }
            )
            Button(
                onClick = { if (personalEnabled) requestLocationIfNeeded(); onComplete(name.trim(), personalEnabled, webSearchEnabled) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Get started") }
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