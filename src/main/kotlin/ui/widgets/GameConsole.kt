package ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

@Composable
fun GameConsole(output: Flow<String>) {
    val lines = remember { mutableStateListOf<String>() }
    val state = rememberLazyListState()

    LaunchedEffect(output) {
        output.collect {
            lines.add(it)
            state.animateScrollToItem(lines.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp),
        state = state
    ) {
        items(lines) { line ->
            Text(
                text = line,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}