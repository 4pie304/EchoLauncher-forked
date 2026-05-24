package ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import funlauncher.Theme

/**
 * Компонент, который применяет выбранную цветовую схему с анимацией и градиентным фоном.
 * @param theme Выбранная тема оформления.
 * @param content Содержимое, к которому применяется тема.
 */
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

    val gradientBrush = if (useDarkTheme) {
        Brush.verticalGradient(listOf(DarkGradientStart, DarkGradientEnd))
    } else {
        Brush.verticalGradient(listOf(LightGradientStart, LightGradientEnd))
    }

    MaterialTheme(
        colorScheme = colors
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            content()
        }
    }
}