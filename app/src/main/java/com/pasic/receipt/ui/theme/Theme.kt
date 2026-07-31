package com.pasic.receipt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FabNavy,
    secondary = ModernCardBlue,
    background = ScreenBackground,
    surface = CardBackground,
    onPrimary = CardBackground,
    onSecondary = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ReceiptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
