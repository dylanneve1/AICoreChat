package org.dylanneve1.aicorechat.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun UnsupportedDeviceScreen(message: String?) {
    val context = LocalContext.current
    val url = "https://play.google.com/apps/testing/com.google.android.aicore"

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(24.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Unsupported device",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = buildString {
                    append("This device doesn't appear to support Gemini Nano on AICore.\n\n")
                    append("If you have a Pixel 9 or Pixel 10 series device, ensure you've joined the testing program.")
                    if (!message.isNullOrBlank()) {
                        append("\n\nDetails: ")
                        append(message)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Join AICore testing program")
            }
        }
    }
} 