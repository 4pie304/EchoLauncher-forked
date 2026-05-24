package org.chokopieum.software.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

@OptIn(ExperimentalFoundationApi::class)
fun main() = application {
    val state = rememberWindowState(size = DpSize(400.dp, 400.dp))
    var progressState by remember { mutableStateOf(ProgressState.Loading) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Preview: LMB = Success, RMB = Error, MMB = Loading",
        state = state
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onClick(
                    matcher = androidx.compose.foundation.PointerMatcher.mouse(PointerButton.Primary), // ЛКМ
                    onClick = { progressState = ProgressState.Success }
                )
                .onClick(
                    matcher = androidx.compose.foundation.PointerMatcher.mouse(PointerButton.Secondary), // ПКМ
                    onClick = { progressState = ProgressState.Error }
                )
                .onClick(
                    matcher = androidx.compose.foundation.PointerMatcher.mouse(PointerButton.Tertiary), // Колесико (СКМ)
                    onClick = { progressState = ProgressState.Loading }
                ),
            contentAlignment = Alignment.Center
        ) {
            MateriaCircularProgressIndicator(state = progressState)
        }
    }
}