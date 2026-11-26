package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FameLifeDarkColorScheme = darkColorScheme(
    primary = FameMint,
    onPrimary = StudioObsidian,
    primaryContainer = FameMintContainer,
    onPrimaryContainer = FameMint,
    secondary = InfoCyan,
    onSecondary = StudioObsidian,
    secondaryContainer = StudioSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = PhotoLiveRose,
    onTertiary = TextPrimary,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = StudioSurfaceElevated,
    outline = StudioOutline,
    error = LiveCrimson,
    onError = TextPrimary
)

val FameLifeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FameLifeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FameLifeDarkColorScheme,
        typography = FameLifeTypography,
        shapes = FameLifeShapes,
        content = content
    )
}
