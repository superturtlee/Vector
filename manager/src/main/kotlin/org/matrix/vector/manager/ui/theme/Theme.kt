package org.matrix.vector.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Fallback colors if the user is below Android 12
private val DarkColorScheme =
    darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
        secondary = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
        tertiary = androidx.compose.ui.graphics.Color(0xFFEFB8C8),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF6650a4),
        secondary = androidx.compose.ui.graphics.Color(0xFF625b71),
        tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260),
    )

@Composable
fun VectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (API 31)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
