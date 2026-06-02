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
import org.jetbrains.compose.resources.stringResource
import org.chokopieum.software.materia_launcher.generated.resources.*
import ui.screens.modifications.FilterPanel
import ui.screens.modifications.InstallModificationDialog
import ui.screens.modifications.ModificationDetails
import ui.screens.modifications.ModificationList
import ui.screens.modifications.ModificationsViewModel
import java.io.File
import kotlinx.coroutines.launch
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

    // Эффекты для обновления фильтров и поиска
    LaunchedEffect(viewModel.selectedType, viewModel.allCategories.size, viewModel.allLoaders.size) {
        viewModel.filterCategoriesAndLoaders()
    }

    LaunchedEffect(viewModel.selectedBuildForFilter) {
        viewModel.applyBuildFilter()
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
                                modifier = Modifier.widthIn(max = 400.dp).padding(end = 16.dp),
                                singleLine = true
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
                AnimatedVisibility(visible = viewModel.selectedProject == null) {
                    FilterPanel(viewModel = viewModel)
                }

                // Основной контент
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Crossfade(targetState = viewModel.selectedProject) { project ->
                        if (project != null) {
                            ModificationDetails(
                                project = project,
                                projectVersions = viewModel.projectVersions,
                                onInstallClick = { version ->
                                    if (viewModel.selectedType == ModificationType.MODPACKS) {
                                        viewModel.modpackInstaller.install(version) {
                                            onModpackInstalled()
                                        }
                                        viewModel.selectedProject = null
                                    } else {
                                        viewModel.versionToInstall = version
                                    }
                                }
                            )
                        } else {
                            ModificationList(viewModel = viewModel)
                        }
                    }
                    if (viewModel.isLoadingProject) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            // Диалог установки
            viewModel.versionToInstall?.let { version ->
                val compatibleBuilds = viewModel.allBuilds.filter { build ->
                    version.gameVersions.any { gameVersion -> build.version.contains(gameVersion) } &&
                            (version.loaders.isEmpty() || version.loaders.any { loader -> build.type.name.contains(loader, ignoreCase = true) })
                }
                InstallModificationDialog(
                    compatibleBuilds = compatibleBuilds,
                    onDismiss = { viewModel.versionToInstall = null },
                    onInstall = { build ->
                        scope.launch {
                            try {
                                val fileToDownload = version.files.firstOrNull { it.primary } ?: version.files.first()
                                val destinationDir = File(build.installPath, viewModel.selectedType.installDir)
                                val destinationFile = File(destinationDir, fileToDownload.filename)
                                val task = DownloadManager.startTask("Скачивание ${version.name}", coroutineContext.job)

                                modificationDownloader.download(fileToDownload, destinationFile) { progress, status ->
                                    DownloadManager.updateTask(task.id, progress, status)
                                }
                                snackbarHostState.showSnackbar("Загрузка ${version.name} началась")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Ошибка установки: ${e.message}")
                            } finally {
                                viewModel.versionToInstall = null
                            }
                        }
                    }
                )
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
                    ModificationType.entries.forEach { type ->
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