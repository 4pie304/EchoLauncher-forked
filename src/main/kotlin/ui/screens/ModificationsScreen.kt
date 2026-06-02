package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import funlauncher.managers.BuildManager
import funlauncher.managers.CacheManager
import funlauncher.managers.PathManager
import funlauncher.net.Version
import funlauncher.utils.VersionMatcher
import org.jetbrains.compose.resources.stringResource
import org.chokopieum.software.materia_launcher.generated.resources.*
import ui.screens.modifications.*
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import funlauncher.net.DownloadManager
import funlauncher.net.ModificationDownloader
import kotlinx.coroutines.job
import kotlin.coroutines.coroutineContext

enum class FilterState {
    INCLUDED, EXCLUDED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ModificationsScreen(
    onBack: () -> Unit,
    buildManager: BuildManager,
    onModpackInstalled: () -> Unit,
    pathManager: PathManager,
    snackbarHostState: SnackbarHostState,
    cacheManager: CacheManager
) {
    val scope = rememberCoroutineScope()
    
    // Инициализация ViewModel
    val viewModel = remember {
        ModificationsViewModel(buildManager, pathManager, cacheManager, scope).apply {
            init()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onDispose()
        }
    }
    
    val modificationDownloader = remember { ModificationDownloader() }

    val contentPaddingBottom by animateDpAsState(80.dp)

    fun installVersion(version: Version, build: funlauncher.MinecraftBuild) {
        scope.launch {
            var taskId: String? = null
            try {
                val fileToDownload = version.files.firstOrNull { it.primary } ?: version.files.first()
                val destinationDir = File(build.installPath, viewModel.selectedType.installDir)
                val destinationFile = File(destinationDir, fileToDownload.filename)
                val task = DownloadManager.startTask("Скачивание ${version.name}", coroutineContext.job)
                taskId = task.id

                modificationDownloader.download(fileToDownload, destinationFile) { progress, status ->
                    DownloadManager.updateTask(task.id, progress, status)
                }
                DownloadManager.updateTask(task.id, 1f, "Завершено")
                snackbarHostState.showSnackbar("Установлено в '${build.name}'")
                delay(3000)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Ошибка установки: ${e.message}")
                taskId?.let { DownloadManager.updateTask(it, 0f, "Ошибка: ${e.message}") }
                delay(5000)
            } finally {
                taskId?.let { DownloadManager.removeTask(it) }
            }
        }
    }

    // Эффекты для обновления фильтров и поиска
    LaunchedEffect(viewModel.selectedType, viewModel.allCategories.size, viewModel.allLoaders.size) {
        viewModel.filterCategoriesAndLoaders()
    }

    LaunchedEffect(viewModel.selectedBuildForFilter) {
        viewModel.applyBuildFilter()
        // Если выбрана сборка и мы были на вкладке модпаков, переключимся на моды
        if (viewModel.selectedBuildForFilter != null && viewModel.selectedType == ModificationType.MODPACKS) {
            viewModel.selectedType = ModificationType.MODS
        }
    }

    LaunchedEffect(
        viewModel.searchQuery,
        viewModel.selectedType,
        viewModel.selectedVersions.toList(),
        viewModel.selectedLoaders.toMap(),
        viewModel.selectedCategories.toMap()
    ) {
        viewModel.search()
    }

    if (viewModel.showBuildSelectionDialog) {
        BuildSelectionDialog(
            builds = viewModel.allBuilds,
            onDismissRequest = { viewModel.showBuildSelectionDialog = false },
            onBuildSelected = { build ->
                viewModel.selectedBuildForFilter = build
                viewModel.showBuildSelectionDialog = false
            }
        )
    }

    if (viewModel.showVersionSelectionDialog) {
        VersionSelectionDialog(
            versions = viewModel.allVanillaVersions,
            selectedVersions = viewModel.selectedVersions,
            onDismissRequest = { viewModel.showVersionSelectionDialog = false }
        )
    }

    if (viewModel.showInstallDialog) {
        VersionInstallDialog(
            versions = viewModel.getFilteredVersionsForInstall().ifEmpty { viewModel.projectVersions },
            onDismissRequest = { viewModel.showInstallDialog = false },
            onInstallClick = { version ->
                viewModel.showInstallDialog = false
                
                val compatibleBuilds = viewModel.allBuilds.filter { build ->
                    version.gameVersions.any { gameVersion -> VersionMatcher.isCompatible(build.version, gameVersion) } &&
                            (version.loaders.isEmpty() || version.loaders.any { loader -> build.type.name.contains(loader, ignoreCase = true) })
                }

                val targetBuild = viewModel.selectedBuildForFilter?.let {
                    if (compatibleBuilds.contains(it)) it else null
                } ?: compatibleBuilds.firstOrNull()

                if (targetBuild == null) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Не найдено совместимых сборок для этой версии.")
                    }
                    return@VersionInstallDialog
                }
                installVersion(version, targetBuild)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(targetState = viewModel.selectedProject) { project ->
                        if (project == null) {
                            OutlinedTextField(
                                value = viewModel.searchQuery,
                                onValueChange = { viewModel.searchQuery = it },
                                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                                modifier = Modifier.fillMaxWidth(0.6f).padding(end = 16.dp),
                                singleLine = true,
                                leadingIcon = {
                                    IconButton(onClick = { viewModel.isFilterPanelVisible = !viewModel.isFilterPanelVisible }) {
                                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                                    }
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                                }
                            )
                        } else {
                            Text(project.title)
                        }
                    }
                },
                navigationIcon = {
                    AnimatedVisibility(visible = viewModel.selectedProject != null) {
                        IconButton(onClick = { viewModel.selectedProject = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                        }
                    }
                },
                actions = {
                    Button(onClick = { viewModel.showBuildSelectionDialog = true }) {
                        Text(viewModel.selectedBuildForFilter?.name ?: "Выбрать сборку")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать сборку")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(modifier = Modifier.fillMaxSize().padding(bottom = contentPaddingBottom)) {
                
                // Левое меню с фильтрами
                AnimatedVisibility(visible = viewModel.isFilterPanelVisible && viewModel.selectedProject == null) {
                    FilterPanel(viewModel = viewModel)
                }

                // Основной контент
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Crossfade(targetState = viewModel.selectedProject) { project ->
                        if (project != null) {
                            ModificationDetails(
                                project = project,
                                isLoadingDetails = viewModel.isLoadingProjectDetails,
                                onInstallClick = {
                                    val filteredVersions = viewModel.getFilteredVersionsForInstall()
                                    
                                    when {
                                        // Если выбрана сборка и найдена ровно одна совместимая версия
                                        viewModel.selectedBuildForFilter != null && filteredVersions.size == 1 -> {
                                            installVersion(filteredVersions.first(), viewModel.selectedBuildForFilter!!)
                                        }
                                        // Во всех остальных случаях (не выбрана сборка, или найдено 0 или >1 версий) - показываем диалог
                                        else -> {
                                            viewModel.showInstallDialog = true
                                        }
                                    }
                                }
                            )
                        } else {
                            ModificationList(viewModel = viewModel)
                        }
                    }
                }
            }

            // NavigationBar (нижняя панель)
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { offset -> offset }),
                exit = slideOutVertically(targetOffsetY = { offset -> offset }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                NavigationBar(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .width(500.dp)
                        .height(64.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onBack,
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) },
                        label = { Text(stringResource(Res.string.back)) }
                    )
                    val modificationTypes = if (viewModel.selectedBuildForFilter != null) {
                        ModificationType.entries.filter { it != ModificationType.MODPACKS }
                    } else {
                        ModificationType.entries
                    }
                    modificationTypes.forEach { type ->
                        NavigationBarItem(
                            selected = viewModel.selectedType == type,
                            onClick = { viewModel.selectedType = type },
                            icon = { Icon(type.icon, contentDescription = type.displayName) },
                            label = { Text(type.displayName) }
                        )
                    }
                }
            }
        }
    }
}

enum class ModificationType(
    val displayName: String,
    val icon: ImageVector,
    val projectType: String,
    val installDir: String
) {
    MODPACKS("Модпаки", Icons.Default.Home, "modpack", ""),
    MODS("Моды", Icons.Default.Star, "mod", "mods"),
    RESOURCE_PACKS("Ресурс-паки", Icons.Default.Settings, "resourcepack", "resourcepacks"),
    DATAPACKS("Датапаки", Icons.Default.DateRange, "datapack", "datapacks"),
    SHADERS("Шейдеры", Icons.Default.Build, "shader", "shaderpacks")
}