package ui.screens.modifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import funlauncher.net.Project
import funlauncher.net.Version
import org.jetbrains.compose.resources.stringResource
import org.chokopieum.software.materia_launcher.generated.resources.Res
import org.chokopieum.software.materia_launcher.generated.resources.cancel
import org.chokopieum.software.materia_launcher.generated.resources.install
import org.chokopieum.software.materia_launcher.generated.resources.versions
import ui.widgets.ImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificationDetails(
    project: Project,
    projectVersions: List<Version>,
    onInstallClick: (Version) -> Unit
) {
    val imageTransformer = remember {
        object : ImageTransformer {
            @Composable
            override fun transform(link: String): ImageData? {
                val painter = ImageLoader.rememberImagePainterFromUrl(link)
                return painter?.let {
                    ImageData(
                        painter = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }

    var showVersionDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок: Логотип, Название, Автор, Кнопка Установить
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val painter = ImageLoader.rememberImagePainterFromUrl(project.iconUrl)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    painter?.let {
                        Image(
                            painter = it,
                            contentDescription = "${project.title} icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Placeholder icon",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.headlineMedium)
                    Text("by ${project.team}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(project.description, style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(Modifier.width(16.dp))

                Button(
                    onClick = { showVersionDialog = true },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(Res.string.install))
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
        }

        // Описание Markdown
        item {
            Markdown(
                content = project.body,
                imageTransformer = imageTransformer
            )
        }
    }

    // Диалог выбора версии
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text(stringResource(Res.string.versions)) },
            text = {
                if (projectVersions.isEmpty()) {
                    Text("Нет доступных версий")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(projectVersions) { version ->
                            Card(
                                onClick = {
                                    showVersionDialog = false
                                    onInstallClick(version)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(version.name, style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text(version.versionType, modifier = Modifier.padding(4.dp))
                                        }
                                        Text("MC: ${version.gameVersions.take(3).joinToString()}${if(version.gameVersions.size > 3) "..." else ""}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                                        if (version.loaders.isNotEmpty()) {
                                            Text("Loaders: ${version.loaders.take(2).joinToString()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
