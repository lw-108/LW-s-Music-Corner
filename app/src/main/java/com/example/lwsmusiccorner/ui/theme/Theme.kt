package com.example.lwsmusiccorner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ReddishOrange = Color(0xFFFF4500)
private val DarkReddishOrange = Color(0xFFCC3700)

private val DarkColorScheme = darkColorScheme(
    primary = ReddishOrange,
    secondary = DarkReddishOrange,
    tertiary = Color(0xFFFF7F50)
)

private val LightColorScheme = lightColorScheme(
    primary = ReddishOrange,
    secondary = DarkReddishOrange,
    tertiary = Color(0xFFFF7F50)
)

@Composable
fun LwsMusicCornerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to force our Reddish Orange theme
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
        typography = Typography,
        content = content
    )
}
