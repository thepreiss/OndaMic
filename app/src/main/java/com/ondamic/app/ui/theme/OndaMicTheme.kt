package com.ondamic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianStage = Color(0xFF090C10)
val ObsidianDimmer = Color(0xFF000000)
val SurfaceCard = Color(0xFF121822)
val SurfaceCardElevated = Color(0xFF192231)
val OndaCyan = Color(0xFF00E5FF)
val OndaEmerald = Color(0xFF00E676)
val AmberGuard = Color(0xFFFFB300)
val OnAirCoral = Color(0xFFFF2A54)
val TextPrimary = Color(0xFFF3F6FA)
val TextSecondary = Color(0xFF9BA9BC)
val BorderSubtle = Color(0xFF253247)

private val OndaDarkColorScheme = darkColorScheme(
    primary = OndaCyan,
    onPrimary = Color(0xFF00262C),
    primaryContainer = Color(0xFF004D59),
    onPrimaryContainer = Color(0xFFB8F8FF),
    secondary = OndaEmerald,
    onSecondary = Color(0xFF002912),
    tertiary = AmberGuard,
    error = OnAirCoral,
    background = ObsidianStage,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun OndaMicTheme(
    stageDimmer: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (stageDimmer) {
        OndaDarkColorScheme.copy(
            background = ObsidianDimmer,
            surface = Color(0xFF080B10),
            surfaceVariant = Color(0xFF0E131B)
        )
    } else {
        OndaDarkColorScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
