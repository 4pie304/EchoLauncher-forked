package org.chokopieum.software.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun MateriaButtonPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        MateriaButton(text = "Log In", onClick = {})
    }
}

@Composable
fun MateriaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color(0xFF007AFF), // Apple Blue
    contentColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Плавная анимация цвета не обязательна для классического "плоского" Apple-стиля,
    // но они обычно слегка затемняют кнопку при нажатии.
    val currentBackgroundColor = when {
        !enabled -> Color(0xFFE5E5EA) // Светло-серый для disabled
        isPressed -> backgroundColor.copy(alpha = 0.8f) // Зажата
        isHovered -> backgroundColor.copy(alpha = 0.95f) // Наведение (актуально для macOS)
        else -> backgroundColor
    }

    val currentContentColor = when {
        !enabled -> Color(0xFF8E8E93) // Серый текст для disabled
        else -> contentColor
    }

    Box(
        modifier = modifier
            // Скругления как в iOS/macOS кнопках
            .clip(RoundedCornerShape(10.dp))
            .background(currentBackgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Убираем ripple, чтобы было как на iOS
                enabled = enabled,
                onClick = onClick
            )
            // Внутренние отступы как в стандартных Apple кнопках
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = currentContentColor,
                fontSize = 17.sp, // Стандартный размер текста для iOS
                fontFamily = FontFamily.SansSerif, // San Francisco (системный шрифт)
                fontWeight = FontWeight.SemiBold, // Полужирное начертание
                letterSpacing = (-0.4).sp // Небольшой трекинг для имитации SF Pro
            )
        )
    }
}
