package ui.screens.modifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import funlauncher.net.ModrinthCategoryTag
import funlauncher.net.ModrinthLoaderTag
import org.jetbrains.compose.resources.stringResource
import org.chokopieum.software.materia_launcher.generated.resources.Res
import org.chokopieum.software.materia_launcher.generated.resources.categories
import org.chokopieum.software.materia_launcher.generated.resources.excluded
import org.chokopieum.software.materia_launcher.generated.resources.included
import org.chokopieum.software.materia_launcher.generated.resources.loaders
import org.chokopieum.software.materia_launcher.generated.resources.versions
import ui.screens.FilterState
import ui.widgets.ImageLoader

@Composable
fun FilterPanel(
    displayedVersions: List<String>,
    selectedVersions: MutableList<String>,
    filteredCategories: List<ModrinthCategoryTag>,
    selectedCategories: MutableMap<String, FilterState>,
    filteredLoaders: List<ModrinthLoaderTag>,
    selectedLoaders: MutableMap<String, FilterState>,
    showOnlyReleaseVersions: Boolean,
    onShowOnlyReleaseVersionsChange: (Boolean) -> Unit
) {
    // Состояния для сворачиваемых списков
    var versionsExpanded by remember { mutableStateOf(true) }
    var categoriesExpanded by remember { mutableStateOf(true) }
    var loadersExpanded by remember { mutableStateOf(true) }

    // Состояния для "Показать больше"
    var showAllVersionsList by remember { mutableStateOf(false) }
    var showAllCategoriesList by remember { mutableStateOf(false) }
    var showAllLoadersList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Секция версий
        Row(
            modifier = Modifier.fillMaxWidth().clickable { versionsExpanded = !versionsExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.versions), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { versionsExpanded = !versionsExpanded }) {
                Icon(
                    imageVector = if (versionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (versionsExpanded) "Свернуть версии" else "Развернуть версии"
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = versionsExpanded) {
            Column {
                val versionsToDisplay = if (showAllVersionsList) displayedVersions else displayedVersions.take(7)
                versionsToDisplay.forEach { version ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.clickable {
                                if (version in selectedVersions) selectedVersions.remove(version) else selectedVersions.add(version)
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = version in selectedVersions,
                                onCheckedChange = {
                                    if (it) selectedVersions.add(version) else selectedVersions.remove(version)
                                }
                            )
                            Text(version, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                if (displayedVersions.size > 7) {
                    TextButton(onClick = { showAllVersionsList = !showAllVersionsList }) {
                        Text(if (showAllVersionsList) "Свернуть" else "Показать больше (${displayedVersions.size - 7})")
                    }
                }
                TextButton(onClick = { onShowOnlyReleaseVersionsChange(!showOnlyReleaseVersions) }) {
                    Text(if (showOnlyReleaseVersions) "Показать все версии" else "Показать только релизы")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Секция категорий
        Row(
            modifier = Modifier.fillMaxWidth().clickable { categoriesExpanded = !categoriesExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.categories), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { categoriesExpanded = !categoriesExpanded }) {
                Icon(
                    imageVector = if (categoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (categoriesExpanded) "Свернуть категории" else "Развернуть категории"
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = categoriesExpanded) {
            Column {
                val categoriesToDisplay = if (showAllCategoriesList) filteredCategories else filteredCategories.take(7)
                categoriesToDisplay.forEach { category ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        val categoryIcon = ImageLoader.rememberImagePainterFromUrl(category.icon)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val currentState = selectedCategories[category.name]
                                val nextState = when (currentState) {
                                    null -> FilterState.INCLUDED
                                    FilterState.INCLUDED -> FilterState.EXCLUDED
                                    FilterState.EXCLUDED -> null
                                }
                                if (nextState == null) {
                                    selectedCategories.remove(category.name)
                                } else {
                                    selectedCategories[category.name] = nextState
                                }
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            categoryIcon?.let {
                                Image(
                                    painter = it,
                                    contentDescription = category.pretty_name,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                )
                            } ?: Icon(
                                imageVector = Icons.Default.Category, // Placeholder icon
                                contentDescription = category.pretty_name,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(category.pretty_name ?: category.name, modifier = Modifier.weight(1f).padding(start = 4.dp))
                            AnimatedContent(targetState = selectedCategories[category.name]) { state ->
                                when (state) {
                                    FilterState.INCLUDED -> Icon(
                                        Icons.Default.Check,
                                        contentDescription = stringResource(Res.string.included),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    FilterState.EXCLUDED -> Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.excluded),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    null -> Spacer(Modifier.size(24.dp)) // Placeholder for alignment
                                }
                            }
                        }
                    }
                }
                if (filteredCategories.size > 7) {
                    TextButton(onClick = { showAllCategoriesList = !showAllCategoriesList }) {
                        Text(if (showAllCategoriesList) "Свернуть" else "Показать больше (${filteredCategories.size - 7})")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Секция загрузчиков
        Row(
            modifier = Modifier.fillMaxWidth().clickable { loadersExpanded = !loadersExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.loaders), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { loadersExpanded = !loadersExpanded }) {
                Icon(
                    imageVector = if (loadersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (loadersExpanded) "Свернуть загрузчики" else "Развернуть загрузчики"
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = loadersExpanded) {
            Column {
                val loadersToDisplay = if (showAllLoadersList) filteredLoaders else filteredLoaders.take(7)
                loadersToDisplay.forEach { loader ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        val loaderIcon = ImageLoader.rememberImagePainterFromUrl(loader.icon)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val currentState = selectedLoaders[loader.name]
                                val nextState = when (currentState) {
                                    null -> FilterState.INCLUDED
                                    FilterState.INCLUDED -> FilterState.EXCLUDED
                                    FilterState.EXCLUDED -> null
                                }
                                if (nextState == null) {
                                    selectedLoaders.remove(loader.name)
                                } else {
                                    selectedLoaders[loader.name] = nextState
                                }
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            loaderIcon?.let {
                                Image(
                                    painter = it,
                                    contentDescription = loader.pretty_name,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                )
                            } ?: Icon(
                                imageVector = Icons.Default.Extension, // Placeholder icon
                                contentDescription = loader.pretty_name,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(loader.pretty_name ?: loader.name, modifier = Modifier.weight(1f).padding(start = 4.dp))
                            AnimatedContent(targetState = selectedLoaders[loader.name]) { state ->
                                when (state) {
                                    FilterState.INCLUDED -> Icon(
                                        Icons.Default.Check,
                                        contentDescription = stringResource(Res.string.included),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    FilterState.EXCLUDED -> Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.excluded),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    null -> Spacer(Modifier.size(24.dp)) // Placeholder for alignment
                                }
                            }
                        }
                    }
                }
                if (filteredLoaders.size > 7) {
                    TextButton(onClick = { showAllLoadersList = !showAllLoadersList }) {
                        Text(if (showAllLoadersList) "Свернуть" else "Показать больше (${filteredLoaders.size - 7})")
                    }
                }
            }
        }
    }
}
