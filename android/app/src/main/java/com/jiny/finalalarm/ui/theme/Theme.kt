package com.jiny.finalalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = FA.Primary,
    onPrimary = FA.OnPrimary,
    background = FA.BgTop,
    onBackground = FA.Label,
    surface = FA.Surface,
    onSurface = FA.Label,
    onSurfaceVariant = FA.LabelSecondary,
    surfaceVariant = FA.Fill,
    outline = FA.Separator,
    outlineVariant = FA.Separator,
    error = FA.Destructive,
    onError = FA.OnPrimary,
    tertiary = FA.Accent,
    secondary = FA.Success,
)

private val DarkScheme = darkColorScheme(
    primary = FA.Primary,
    onPrimary = FA.OnPrimary,
    background = FA.BgTopDark,
    onBackground = FA.LabelDark,
    surface = FA.SurfaceDark,
    onSurface = FA.LabelDark,
    onSurfaceVariant = FA.LabelSecondaryDark,
    surfaceVariant = FA.FillDark,
    outline = FA.SeparatorDark,
    outlineVariant = FA.SeparatorDark,
    error = FA.Destructive,
    onError = FA.OnPrimary,
    tertiary = FA.Accent,
    secondary = FA.Success,
)

// 부드러운 둥글기
private val FaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FinalAlarmTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = FaTypography,
        shapes = FaShapes,
        content = content,
    )
}
