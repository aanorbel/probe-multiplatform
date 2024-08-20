package org.ooni.probe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.ooni.probe.ui.shared.LightStatusBars

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    LightStatusBars(!useDarkTheme)

    MaterialTheme(
        colorScheme = if (useDarkTheme) darkScheme else lightScheme,
        typography = AppTypography(),
        content = content,
    )
}
