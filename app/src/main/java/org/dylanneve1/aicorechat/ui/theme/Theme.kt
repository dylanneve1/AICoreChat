package org.dylanneve1.aicorechat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = AICorePrimary,
    onPrimary = Color.White,
    primaryContainer = AICoreAccentLight,
    onPrimaryContainer = AICorePrimaryDark,
    inversePrimary = AICorePrimaryDark,
    secondary = AICoreAccent,
    onSecondary = Color.White,
    secondaryContainer = Neutral200,
    onSecondaryContainer = Neutral800,
    tertiary = AICorePrimaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Neutral100,
    onTertiaryContainer = AICorePrimary,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Error,
    background = Neutral50,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral700,
    inverseSurface = Neutral900,
    inverseOnSurface = Neutral50,
    outline = Neutral400,
    surfaceContainerHighest = Neutral200,
    surfaceContainerHigh = Neutral100,
    surfaceContainer = Neutral50,
    surfaceContainerLow = Neutral50,
    surfaceContainerLowest = Neutral50,
    outlineVariant = Neutral300,
    scrim = Color.Black.copy(alpha = 0.32f)
)

private val DarkColorScheme = darkColorScheme(
    primary = AICoreAccentLight,
    onPrimary = Neutral900,
    primaryContainer = AICorePrimaryDark,
    onPrimaryContainer = AICoreAccentLight,
    inversePrimary = AICorePrimary,
    secondary = AICoreAccent,
    onSecondary = Neutral900,
    secondaryContainer = Neutral700,
    onSecondaryContainer = Neutral200,
    tertiary = AICorePrimaryLight,
    onTertiary = Neutral900,
    tertiaryContainer = Neutral800,
    onTertiaryContainer = AICoreAccentLight,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Neutral900,
    onBackground = Neutral100,
    surface = Neutral800,
    onSurface = Neutral100,
    surfaceVariant = Neutral700,
    onSurfaceVariant = Neutral400,
    inverseSurface = Neutral100,
    inverseOnSurface = Neutral900,
    outline = Neutral500,
    surfaceContainerHighest = Neutral600,
    surfaceContainerHigh = Neutral700,
    surfaceContainer = Neutral800,
    surfaceContainerLow = Neutral900,
    surfaceContainerLowest = Neutral900,
    outlineVariant = Neutral600,
    scrim = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun AICoreChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enhanced to use dynamic colors when available
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EnhancedTypography,
        content = content
    )
}
