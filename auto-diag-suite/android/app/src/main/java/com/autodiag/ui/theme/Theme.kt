package com.autodiag.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AutoDiagColorScheme = darkColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFE5EAF2),
    secondary = Color(0xFF7C8AA5),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE5EAF2),
    surface = Color(0xFF131C2E),
    onSurface = Color(0xFFE5EAF2),
    surfaceVariant = Color(0xFF1E2A3D),
    onSurfaceVariant = Color(0xFF7C8AA5),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF1E2A3D)
)

@Composable
fun AutoDiagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoDiagColorScheme,
        content = content
    )
}
