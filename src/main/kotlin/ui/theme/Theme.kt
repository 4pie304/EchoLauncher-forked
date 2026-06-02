package ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import funlauncher.Theme

@Composable
fun AnimatedAppTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (theme) {
        Theme.System -> isSystemDark
        Theme.Light -> false
        Theme.Dark -> true
    }

    val colors = if (useDarkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    val animationSpec = tween<Color>(durationMillis = 500)

    val topColor by animateColorAsState(
        targetValue = if (useDarkTheme) DarkGradientStart else LightGradientStart,
        animationSpec = animationSpec,
        label = "TopGradientColor"
    )

    val middleColor by animateColorAsState(
        targetValue = if (useDarkTheme) Color(0xFF11161C) else Color(0xFFF7F8FA),
        animationSpec = animationSpec,
        label = "MiddleGradientColor"
    )

    val bottomColor by animateColorAsState(
        targetValue = if (useDarkTheme) DarkGradientEnd else LightGradientEnd,
        animationSpec = animationSpec,
        label = "BottomGradientColor"
    )

    val gradientBrush = Brush.verticalGradient(
        listOf(topColor, middleColor, bottomColor)
    )

    MaterialTheme(
        colorScheme = colors
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(gradientBrush)
        ) {
            content()
        }
    }

}