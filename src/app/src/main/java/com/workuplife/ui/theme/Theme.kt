package com.workuplife.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonGreen = Color(0xFF00FF88)
val DarkBg = Color(0xFF000000)
val Surface = Color(0xFF121212)

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    background = DarkBg,
    surface = Surface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WorkUplifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
