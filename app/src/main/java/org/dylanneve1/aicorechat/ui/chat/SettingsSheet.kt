package org.dylanneve1.aicorechat.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    temperature: Float,
    topK: Int,
    onTemperatureChange: (Float) -> Unit,
    onTopKChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val githubUrl = "https://github.com/dylanneve1/AICoreChat"
    val githubIntent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Model Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Temperature Slider
            SettingSlider(
                label = "Temperature",
                value = temperature,
                valueRange = 0f..1f,
                onValueChange = onTemperatureChange,
                valueLabel = "%.2f".format(temperature)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Top-K Slider
            SettingSlider(
                label = "Top-K",
                value = topK.toFloat(),
                valueRange = 1f..100f,
                onValueChange = { onTopKChange(it.roundToInt()) },
                valueLabel = topK.toString()
            )

            Divider(modifier = Modifier.padding(vertical = 24.dp))

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "This app was created by Dylan Neve.",
                style = MaterialTheme.typography.bodyMedium
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
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
