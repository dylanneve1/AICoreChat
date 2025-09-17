package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.ui.components.SectionHeaderCard
import kotlin.math.roundToInt

@Composable
fun ModelSettingsScreen(
    temperature: Float,
    topK: Int,
    onTemperatureChange: (Float) -> Unit,
    onTopKChange: (Int) -> Unit,
    onResetModelSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        SectionHeaderCard(
            icon = Icons.Outlined.Tune,
            title = "Model Parameters",
            description = "Fine-tune how Gemini Nano generates responses",
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Model Info Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemini Nano",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "On-device AI model",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Temperature Control
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Thermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Temperature",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            text = "%.2f".format(temperature),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }

                Text(
                    text = "Controls randomness and creativity in responses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // Visual indicator bar
                val tempColor by animateColorAsState(
                    targetValue = when {
                        temperature < 0.3f -> MaterialTheme.colorScheme.tertiary
                        temperature < 0.7f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                    label = "temp_color",
                )

                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = tempColor,
                        activeTrackColor = tempColor,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Focused",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (temperature < 0.3f) FontWeight.Bold else FontWeight.Normal,
                            color = if (temperature < 0.3f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Balanced",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (temperature in 0.3f..0.7f) FontWeight.Bold else FontWeight.Normal,
                            color = if (temperature in 0.3f..0.7f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "0.5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Creative",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (temperature > 0.7f) FontWeight.Bold else FontWeight.Normal,
                            color = if (temperature > 0.7f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        // Top-K Sampling
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Token,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top-K Sampling",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            text = topK.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }

                Text(
                    text = "Limits vocabulary selection for better focus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                val topKColor by animateColorAsState(
                    targetValue = when {
                        topK < 20 -> MaterialTheme.colorScheme.tertiary
                        topK < 60 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    },
                    label = "topk_color",
                )

                Slider(
                    value = topK.toFloat(),
                    onValueChange = { onTopKChange(it.roundToInt()) },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = topKColor,
                        activeTrackColor = topKColor,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Precise",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (topK < 20) FontWeight.Bold else FontWeight.Normal,
                            color = if (topK < 20) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Balanced",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (topK in 20..60) FontWeight.Bold else FontWeight.Normal,
                            color = if (topK in 20..60) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "40",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Diverse",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (topK > 60) FontWeight.Bold else FontWeight.Normal,
                            color = if (topK > 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        // Actions Section
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 12.dp),
                )
                Text(
                    text = "Reset to Recommended",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Temperature: 0.30 • Top-K: 40",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Button(
                    onClick = onResetModelSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Parameters",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
