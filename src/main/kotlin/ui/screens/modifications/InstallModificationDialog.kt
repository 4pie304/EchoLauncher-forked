package ui.screens.modifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import funlauncher.MinecraftBuild
import org.jetbrains.compose.resources.stringResource
import org.chokopieum.software.materia_launcher.generated.resources.Res
import org.chokopieum.software.materia_launcher.generated.resources.cancel
import org.chokopieum.software.materia_launcher.generated.resources.no_compatible_builds
import org.chokopieum.software.materia_launcher.generated.resources.select_a_build

@Composable
fun InstallModificationDialog(
    compatibleBuilds: List<MinecraftBuild>,
    onDismiss: () -> Unit,
    onInstall: (MinecraftBuild) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.select_a_build)) },
        text = {
            if (compatibleBuilds.isEmpty()) {
                Text(stringResource(Res.string.no_compatible_builds))
            } else {
                LazyColumn {
                    items(compatibleBuilds) { build ->
                        Text(build.name, modifier = Modifier.clickable { onInstall(build) }.fillMaxWidth().padding(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
