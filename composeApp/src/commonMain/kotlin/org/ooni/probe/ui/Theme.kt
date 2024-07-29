package org.ooni.probe.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


private val LightColors =
    lightColorScheme(
        primary = primaryColor,
        onPrimary = primaryTextColor,
        secondary = secondaryColor,
        onSecondary = secondaryTextColor,
        tertiary = primaryLightColor,
        onTertiary = primaryTextColor,
        background = backgroundLightColor,
        onBackground = Color.Black,
        surface = surfaceLight,
        onSurface = Color.Black,
        surfaceVariant = surfaceLight,
        onSurfaceVariant = Color.Black,
        secondaryContainer = primaryColor,
        onSecondaryContainer = Color.White,
        error = errorColor,
        onError = onErrorColor,
    )

private val DarkColors =
    darkColorScheme(
        primary = primaryColor,
        onPrimary = primaryTextColor,
        secondary = secondaryLightColor,
        onSecondary = secondaryTextColor,
        tertiary = primaryLightColor,
        onTertiary = primaryTextColor,
        background = backgroundDarkColor,
        onBackground = Color.White,
        surface = surfaceDark,
        onSurface = Color.White,
        surfaceVariant = surfaceDark,
        onSurfaceVariant = Color.White,
        secondaryContainer = primaryColor,
        onSecondaryContainer = Color.White,
        error = errorColor,
        onError = onErrorColor,
    )

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp),
        )
    val typography = MaterialTheme.typography

    MaterialTheme(
        colorScheme,
        shapes,
        typography,
        content,
    )
}
