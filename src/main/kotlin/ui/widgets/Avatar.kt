/*
 * Copyright 2025 Chokopieum Software
 *
 * НЕ ЯВЛЯЕТСЯ ОФИЦИАЛЬНЫМ ПРОДУКТОМ MINECRAFT. НЕ ОДОБРЕНО И НЕ СВЯЗАНО С КОМПАНИЕЙ MOJANG ИЛИ MICROSOFT.
 * Распространяется по лицензии MIT.
 * GITHUB: https://github.com/Chokopieum-Software/MateriaKraft-Launcher
 */

package ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import funlauncher.auth.Account
import funlauncher.auth.MicrosoftAccount
import funlauncher.auth.OfflineAccount
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

// Форма Squircle (суперэллипс)
val SquircleShape = GenericShape { size, _ ->
    val n = 4.8 // Увеличили степень для более выраженной квадратной формы
    val w = size.width / 2f
    val h = size.height / 2f
    
    val path = Path()
    val points = 200 // Увеличили количество точек для плавности
    
    for (i in 0..points) {
        val t = (i.toFloat() / points) * 2 * Math.PI
        val cosT = kotlin.math.cos(t)
        val sinT = kotlin.math.sin(t)
        
        val x = w + w * abs(cosT).pow(2.0 / n) * sign(cosT)
        val y = h + h * abs(sinT).pow(2.0 / n) * sign(sinT)
        
        if (i == 0) {
            path.moveTo(x.toFloat(), y.toFloat())
        } else {
            path.lineTo(x.toFloat(), y.toFloat())
        }
    }
    path.close()
    
    addPath(path)
}

@Composable
fun AvatarImage(account: Account?, modifier: Modifier = Modifier) {
    // Применяем форму сквиркла к модификатору
    val clippedModifier = modifier.clip(SquircleShape)

    // Определяем URL аватара
    val avatarUrl = when (account) {
        is MicrosoftAccount -> "https://mc-heads.net/avatar/${account.username}"
        is OfflineAccount -> "https://mc-heads.net/avatar/${account.username}" // Для оффлайн тоже пробуем получить по нику (скин по нику)
        else -> "https://mc-heads.net/avatar/notch" // Дефолтный (или если аккаунт null)
    }

    val imageBitmap = ImageLoader.rememberImageBitmapFromUrl(avatarUrl)

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Avatar of ${account?.username ?: "Unknown"}",
            modifier = clippedModifier
        )
    } else {
        // Если не загрузилось, показываем плейсхолдер (Notch)
        // Можно использовать локальный ресурс или просто иконку, но по запросу - Notch
        val placeholderUrl = "https://mc-heads.net/avatar/notch"
        val placeholderBitmap = ImageLoader.rememberImageBitmapFromUrl(placeholderUrl)
        
        if (placeholderBitmap != null) {
             Image(
                bitmap = placeholderBitmap,
                contentDescription = "Default Avatar",
                modifier = clippedModifier
            )
        } else {
             Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Loading Avatar",
                modifier = clippedModifier
            )
        }
    }
}
