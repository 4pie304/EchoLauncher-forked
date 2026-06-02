package ui.screens.modifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import funlauncher.MinecraftBuild
import funlauncher.game.VersionMetadataFetcher
import funlauncher.managers.BuildManager
import funlauncher.managers.CacheManager
import funlauncher.managers.PathManager
import funlauncher.modpack.ModpackInstaller
import funlauncher.net.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.screens.FilterState
import ui.screens.ModificationType

class ModificationsViewModel(
    private val buildManager: BuildManager,
    private val pathManager: PathManager,
    private val cacheManager: CacheManager,
    private val coroutineScope: CoroutineScope
) {
    private val modrinthApi = ModrinthApi(cacheManager)
    private val modificationDownloader = ModificationDownloader()
    private val versionMetadataFetcher = VersionMetadataFetcher(buildManager, pathManager)
    val modpackInstaller = ModpackInstaller(buildManager, modrinthApi, pathManager, coroutineScope)

    var searchQuery by mutableStateOf("")
    var searchResult by mutableStateOf<SearchResult?>(null)
    var isLoading by mutableStateOf(false)

    // New state for build filter
    var selectedBuildForFilter by mutableStateOf<MinecraftBuild?>(null)
    var buildDropdownExpanded by mutableStateOf(false)

    // Pagination states
    var currentPage by mutableStateOf(0)
    val pageSize = 20
    var isLoadingMore by mutableStateOf(false)
    var hasMoreResults by mutableStateOf(true)


    var selectedProject by mutableStateOf<Project?>(null)
    var projectVersions by mutableStateOf<List<Version>>(emptyList())
    var isLoadingProject by mutableStateOf(false)

    var versionToInstall by mutableStateOf<Version?>(null)
    val allBuilds = mutableStateListOf<MinecraftBuild>()

    var selectedType by mutableStateOf(ModificationType.MODS) // Default to MODS
    val allVanillaVersions = mutableStateListOf<String>() // Храним все версии
    val selectedVersions = mutableStateListOf<String>()

    val allCategories = mutableStateListOf<ModrinthCategoryTag>()
    val allLoaders = mutableStateListOf<ModrinthLoaderTag>()

    val filteredCategories = mutableStateListOf<ModrinthCategoryTag>()
    val filteredLoaders = mutableStateListOf<ModrinthLoaderTag>()

    val selectedCategories = mutableStateMapOf<String, FilterState>() // Key is category.name
    val selectedLoaders = mutableStateMapOf<String, FilterState>() // Key is loader.name

    var versionLoadTrigger by mutableStateOf(0)

    // Состояния для сворачиваемых списков
    var versionsExpanded by mutableStateOf(true)
    var categoriesExpanded by mutableStateOf(true)
    var loadersExpanded by mutableStateOf(true)

    // Состояние для отображения всех версий (релизы vs все)
    var showOnlyReleaseVersions by mutableStateOf(true)

    fun init() {
        coroutineScope.launch {
            allBuilds.addAll(buildManager.loadBuilds())
        }
        loadInitialData()
        loadVersions()
    }

    private fun loadInitialData() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fetchedCategories = modrinthApi.getCategories()
                withContext(Dispatchers.Main) {
                    allCategories.addAll(fetchedCategories)
                }
            } catch (e: Exception) {
                // Handle error
            }
            try {
                val fetchedLoaders = modrinthApi.getLoaders()
                withContext(Dispatchers.Main) {
                    allLoaders.addAll(fetchedLoaders)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadVersions() {
        coroutineScope.launch(Dispatchers.IO) {
            val cachedVersions = cacheManager.getOrFetch<List<String>>("vanilla_versions") {
                versionMetadataFetcher.getVanillaVersions()
            }
            if (cachedVersions != null) {
                withContext(Dispatchers.Main) {
                    allVanillaVersions.clear()
                    allVanillaVersions.addAll(cachedVersions)
                }
            }

            // Background update
            try {
                val freshVersions = versionMetadataFetcher.getVanillaVersions()
                if (freshVersions.sorted() != cachedVersions?.sorted()) {
                    withContext(Dispatchers.Main) {
                        allVanillaVersions.clear()
                        allVanillaVersions.addAll(freshVersions)
                    }
                    // Update cache in the background
                    cacheManager.getOrFetch<List<String>>("vanilla_versions") { freshVersions }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onDispose() {
        modrinthApi.close()
    }

    fun buildFacetsList(): MutableList<List<String>> {
        val facetsList = mutableListOf<List<String>>()

        // Project Type
        val actualProjectType = if (selectedType == ModificationType.DATAPACKS) {
            ModificationType.MODS.projectType // Datapacks используют project_type 'mod' для поиска
        } else {
            selectedType.projectType
        }
        facetsList.add(listOf("project_type:$actualProjectType"))

        // Game Versions
        if (selectedVersions.isNotEmpty()) {
            facetsList.add(selectedVersions.map { "versions:$it" })
        }

        // Categories
        val includedCategories = selectedCategories.filter { it.value == FilterState.INCLUDED }.keys
        if (includedCategories.isNotEmpty()) {
            facetsList.add(includedCategories.map { "categories:$it" })
        }

        // Loaders
        val includedLoaders = selectedLoaders.filter { it.value == FilterState.INCLUDED }.keys
        if (includedLoaders.isNotEmpty()) {
            facetsList.add(includedLoaders.map { "categories:$it" })
        }

        return facetsList
    }

    // Helper to manually construct the facets string in the format Modrinth API expects
    fun buildFacetsString(facetsList: List<List<String>>): String {
        return facetsList.joinToString(separator = ",", prefix = "[", postfix = "]") { innerList ->
            innerList.joinToString(separator = ",", prefix = "[", postfix = "]") { facet ->
                "\"$facet\""
            }
        }
    }

    fun search() {
        if (selectedProject != null) return

        isLoading = true
        searchResult = null
        currentPage = 0
        hasMoreResults = true

        val facetsList = buildFacetsList()
        val facetsJson = buildFacetsString(facetsList)

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    modrinthApi.search(query = searchQuery, facets = facetsJson, offset = currentPage * pageSize, limit = pageSize)
                }
                searchResult = result
                hasMoreResults = result.hits.size == pageSize
            } catch (e: Exception) {
                searchResult = SearchResult(emptyList(), 0, pageSize, 0)
                hasMoreResults = false
            } finally {
                isLoading = false
            }
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || !hasMoreResults || isLoading) return

        isLoadingMore = true
        currentPage++

        val facetsList = buildFacetsList()
        val facetsJson = buildFacetsString(facetsList)

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    modrinthApi.search(query = searchQuery, facets = facetsJson, offset = currentPage * pageSize, limit = pageSize)
                }

                searchResult = searchResult?.copy(
                    hits = searchResult!!.hits + result.hits
                ) ?: result

                hasMoreResults = result.hits.size == pageSize
            } catch (e: Exception) {
                currentPage--
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun loadProjectDetails(projectId: String, onProjectLoaded: (Project) -> Unit) {
        isLoadingProject = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fullProject = modrinthApi.getProject(projectId)
                val versions = modrinthApi.getProjectVersions(projectId)
                withContext(Dispatchers.Main) {
                    selectedProject = fullProject
                    projectVersions = versions
                    onProjectLoaded(fullProject)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingProject = false
                }
            }
        }
    }

    fun filterCategoriesAndLoaders() {
        filteredCategories.clear()
        selectedCategories.clear() // Clear selections when type changes
        filteredLoaders.clear()
        selectedLoaders.clear() // Clear selections when type changes

        val targetProjectType = if (selectedType == ModificationType.DATAPACKS) {
            ModificationType.MODS.projectType // Datapacks use mod project type for categories
        } else {
            selectedType.projectType
        }

        filteredCategories.addAll(allCategories.filter { it.project_type == targetProjectType })

        // Исправленный список серверных ядер, которые нужно исключать из модов
        val serverCoreLoadersToExclude = setOf("paper", "spigot", "purpur")

        filteredLoaders.addAll(allLoaders.filter { loader ->
            loader.supported_project_types.contains(selectedType.projectType) &&
                    !(selectedType == ModificationType.MODS && serverCoreLoadersToExclude.contains(loader.name.lowercase()))
        })
    }

    fun applyBuildFilter() {
        val build = selectedBuildForFilter
        if (build != null) {
            // 1. Find the best matching game version from the build's version string.
            val fullVersion = build.version // e.g., "1.21.11-fabric-0.19.2"
            val bestMatch = allVanillaVersions
                .filter { vanillaVersion -> fullVersion.startsWith(vanillaVersion) }
                .maxByOrNull { it.length }

            selectedVersions.clear()
            if (bestMatch != null) {
                selectedVersions.add(bestMatch)
            }

            // 2. Set Loader from build.type
            val buildLoaderName = build.type.name.lowercase() // e.g., "fabric", "forge"
            val matchingLoader = allLoaders.find { it.name.lowercase() == buildLoaderName }
            selectedLoaders.clear()
            if (matchingLoader != null) {
                selectedLoaders[matchingLoader.name] = FilterState.INCLUDED
            }
        }
    }
}