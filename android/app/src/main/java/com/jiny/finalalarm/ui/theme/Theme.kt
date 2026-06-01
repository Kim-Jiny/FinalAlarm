package com.jiny.finalalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = FA.Primary,
    onPrimary = FA.Bg,
    background = FA.Bg,
    onBackground = FA.Label,
    surface = FA.Surface,
    onSurface = FA.Label,
    onSurfaceVariant = FA.LabelSecondary,
    surfaceVariant = FA.Fill,
    outline = FA.Separator,
    outlineVariant = FA.Separator,
    error = FA.Destructive,
    onError = FA.Bg,
)

private val DarkScheme = darkColorScheme(
    primary = FA.Primary,
    onPrimary = FA.LabelDark,
    background = FA.BgDark,
    onBackground = FA.LabelDark,
    surface = FA.SurfaceDark,
    onSurface = FA.LabelDark,
    onSurfaceVariant = FA.LabelSecondaryDark,
    surfaceVariant = FA.FillDark,
    outline = FA.SeparatorDark,
    outlineVariant = FA.SeparatorDark,
    error = FA.Destructive,
    onError = FA.LabelDark,
)

@Composable
fun FinalAlarmTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = FaTypography,
        content = content,
    )
}
