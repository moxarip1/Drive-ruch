package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val SweetRushColorScheme = darkColorScheme(
    primary = CandyPink,
    onPrimary = FrostingWhite,
    primaryContainer = CandyPinkDark,
    onPrimaryContainer = FrostingWhite,
    secondary = MintCream,
    onSecondary = BerryDeep,
    secondaryContainer = MintDark,
    onSecondaryContainer = FrostingWhite,
    tertiary = CaramelGold,
    onTertiary = BerryDeep,
    background = BerryDeep,
    onBackground = FrostingWhite,
    surface = BerryPurple,
    onSurface = FrostingWhite,
    surfaceVariant = BerryCard,
    onSurfaceVariant = SugarCream,
    error = StrawberryRed,
    onError = FrostingWhite
)

private val CandyShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun SweetRushTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SweetRushColorScheme,
        typography = CandyTypography,
        shapes = CandyShapes,
        content = content
    )
}
