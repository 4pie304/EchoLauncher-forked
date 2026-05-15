package org.chokopieum.software.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val state = rememberWindowState(size = DpSize(400.dp, 400.dp))
    var isSuccess by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MateriaCircularProgressIndicator Preview (Click anywhere)",
        state = state
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null // Убираем эффект волны при клике
                ) {
                    isSuccess = true
                },
            contentAlignment = Alignment.Center
        ) {
            MateriaCircularProgressIndicator(isSuccess = isSuccess)
        }
    }
}
