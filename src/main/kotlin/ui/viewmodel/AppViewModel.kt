package ui.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.management.OperatingSystemMXBean
import funlauncher.*
import funlauncher.auth.Account
import funlauncher.auth.AccountManager
import funlauncher.game.MinecraftInstaller
import funlauncher.game.VersionMetadataFetcher
import funlauncher.managers.BuildManager
import funlauncher.managers.JavaManager
import funlauncher.net.JavaDownloader
import kotlinx.coroutines.*
import state.AppState
import ui.AppTab
import java.lang.management.ManagementFactory

class AppViewModel(
    private val appState: AppState,
    val buildManager: BuildManager,
    val javaManager: JavaManager,
    val accountManager: AccountManager,
    val javaDownloader: JavaDownloader,
    val versionMetadataFetcher: VersionMetadataFetcher,
    private val onSettingsChange: (AppSettings) -> Unit
) {
    val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // --- UI State ---
    var currentTab by mutableStateOf(AppTab.Home)
    val buildList = mutableStateListOf(*appState.builds.toTypedArray())
    val buildsPendingDeletion = mutableStateListOf<String>()
    var accounts by mutableStateOf(appState.accounts)
    var currentAccount by mutableStateOf(accounts.firstOrNull())

    var daemonStatus by mutableStateOf("STOPPED")
    val isGameRunning by derivedStateOf { daemonStatus == "RUNNING" }
    
    var showAddBuildDialog by mutableStateOf(false)
    var showJavaManagerWindow by mutableStateOf(false)
    var showAccountScreen by mutableStateOf(false)
    var showDownloadsPopup by mutableStateOf(false)
    var showBuildSettingsScreen by mutableStateOf<MinecraftBuild?>(null)
    var errorDialogMessage by mutableStateOf<String?>(null)
    var buildToDelete by mutableStateOf<MinecraftBuild?>(null)
    var showGameConsole by mutableStateOf(false)
    var showRamWarningDialog by mutableStateOf<MinecraftBuild?>(null)

    var isLaunchingBuildId by mutableStateOf<String?>(null)
    var showCheckmark by mutableStateOf(false)

    // Added runningBuild property to fix the error in HomeViewModel
    var runningBuild by mutableStateOf<MinecraftBuild?>(null)
    
    // Store reference to running process
    private var runningProcess: Process? = null

    // Test Build states
    var showTestBuildWarning by mutableStateOf(false)
    var testBuildMessage by mutableStateOf<String?>(null)
    var testBuildTitle by mutableStateOf<String?>(null)

    init {
        synchronizeBuilds()
        checkTestBuild()
    }

    private fun checkTestBuild() {
        viewModelScope.launch {
            val props = java.util.Properties()
            try {
                this@AppViewModel.javaClass.classLoader.getResourceAsStream("app.properties")?.use { stream ->
                    props.load(stream)
                }
            } catch (e: Exception) {
                // ignore
            }
            
            val isTestBuild = props.getProperty("isTestBuild", "false").toBoolean()
            if (isTestBuild) {
                val currentHash = props.getProperty("gitHash", "")
                val latestHash = withContext(Dispatchers.IO) { funlauncher.net.GithubChecker.getLatestCommitHash() }
                
                if (latestHash != null && currentHash.isNotEmpty() && latestHash != currentHash) {
                    testBuildTitle = "Внимание: Устаревшая сборка"
                    testBuildMessage = "Эта тестовая сборка устарела.\nНа GitHub найден более новый коммит ($latestHash).\nВаш коммит ($currentHash).\n\nПожалуйста, попросите новый установщик у человека, от которого вы получили этот файл."
                } else {
                    testBuildTitle = "Тестовая сборка"
                    testBuildMessage = "Данная сборка была скомпилирована из ветки разработки и может содержать ошибки.\nПожалуйста, сообщайте об ошибках разработчику.\n\nНажмите Ctrl + ` для сохранения логов на рабочий стол."
                }
                showTestBuildWarning = true
            }
        }
    }

    private fun synchronizeBuilds() {
        viewModelScope.launch {
            val (synchronizedBuilds, newCount) = withContext(Dispatchers.IO) {
                buildManager.synchronizeBuilds()
            }
            if (synchronizedBuilds.size != buildList.size || synchronizedBuilds != buildList) {
                buildList.clear()
                buildList.addAll(synchronizedBuilds)
            }
        }
    }

    fun refreshBuilds() {
        viewModelScope.launch {
            val freshBuilds = withContext(Dispatchers.IO) {
                buildManager.loadBuilds()
            }
            buildList.clear()
            buildList.addAll(freshBuilds)
        }
    }

    private suspend fun launchMinecraft(build: MinecraftBuild, javaPath: String, account: Account) {
        runCatching {
            if (appState.settings.showConsoleOnLaunch) {
                showGameConsole = true
            }
            val installer = MinecraftInstaller(build, buildManager)
            val finalMaxRam = build.maxRamMb ?: appState.settings.maxRamMb
            val finalJavaArgs = build.javaArgs ?: appState.settings.javaArgs
            val finalEnvVars = build.envVars ?: appState.settings.envVars

            // When launching directly, we might want to set status manually
            daemonStatus = "RUNNING"
            runningBuild = build

            withContext(Dispatchers.IO) {
                runningProcess = installer.launchGame(
                    account = account,
                    javaPath = javaPath,
                    maxRamMb = finalMaxRam,
                    javaArgs = finalJavaArgs,
                    envVars = finalEnvVars
                )
                
                // Wait for process to exit
                runningProcess?.waitFor()
                
                withContext(Dispatchers.Main) {
                    isLaunchingBuildId = null
                    runningBuild = null
                    daemonStatus = "STOPPED"
                    runningProcess = null
                }
            }
        }.onFailure { e ->
            e.printStackTrace()
            errorDialogMessage = "Ошибка запуска: ${e.message}"
            isLaunchingBuildId = null
            runningBuild = null
            daemonStatus = "STOPPED"
            runningProcess = null
        }
    }

    private fun performLaunch(build: MinecraftBuild) {
        isLaunchingBuildId = build.name
        viewModelScope.launch {
            val useAutoJava = build.javaPath.isNullOrBlank() && appState.settings.javaPath.isBlank()
            val account = currentAccount ?: run {
                errorDialogMessage = "Сначала выберите аккаунт!"
                isLaunchingBuildId = null
                return@launch
            }

            if (useAutoJava) {
                val recommendedVersion = javaManager.getRecommendedJavaVersion(build.version)
                val installations = withContext(Dispatchers.IO) { javaManager.findJavaInstallations() }
                val allJavas = installations.launcher + installations.system

                // 1. Ищем точное совпадение
                var suitableJava = allJavas.firstOrNull { it.version == recommendedVersion && it.is64Bit }

                // 2. Если не нашли, ищем любую подходящую (новее или равную)
                if (suitableJava == null) {
                    suitableJava = allJavas.filter { it.version >= recommendedVersion && it.is64Bit }
                                          .maxByOrNull { it.version }
                }

                if (suitableJava != null) {
                    launchMinecraft(build, suitableJava.path, account)
                } else {
                    // 3. Если ничего не нашли, качаем рекомендованную
                    javaDownloader.downloadAndUnpack(recommendedVersion) { result ->
                        viewModelScope.launch {
                            result.fold(
                                onSuccess = { downloadedJava -> launchMinecraft(build, downloadedJava.path, account) },
                                onFailure = {
                                    errorDialogMessage = "Не удалось скачать Java: ${it.message}"
                                    isLaunchingBuildId = null
                                }
                            )
                        }
                    }
                }
            } else {
                val finalJavaPath = build.javaPath ?: appState.settings.javaPath
                launchMinecraft(build, finalJavaPath, account)
            }
        }
    }

    fun onLaunchClick(build: MinecraftBuild) {
        if (currentAccount == null) {
            errorDialogMessage = "Сначала выберите аккаунт!"
            return
        }
        
        // If a game is already running, this button acts as a STOP button
        if (isGameRunning && runningBuild == build) {
            stopGame()
            return
        } else if (isGameRunning) {
            errorDialogMessage = "Уже запущена другая сборка!"
            return
        }

        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val freeMemory = osBean.freeMemorySize / (1024 * 1024)
        val allocatedRam = build.maxRamMb ?: appState.settings.maxRamMb

        if (allocatedRam > freeMemory) {
            showRamWarningDialog = build
        } else {
            performLaunch(build)
        }
    }
    
    fun stopGame() {
        runningProcess?.let {
            if (it.isAlive) {
                it.destroy()
                // Force kill if it doesn't stop gracefully
                viewModelScope.launch(Dispatchers.IO) {
                    delay(3000)
                    if (it.isAlive) {
                        it.destroyForcibly()
                    }
                }
            }
        }
        daemonStatus = "STOPPED"
        runningBuild = null
        isLaunchingBuildId = null
        runningProcess = null
    }

    fun onConfirmRamWarning(build: MinecraftBuild) {
        showRamWarningDialog = null
        performLaunch(build)
    }

    fun onDeleteBuildClick(build: MinecraftBuild) {
        buildToDelete = build
    }

    fun onConfirmDelete(build: MinecraftBuild) {
        viewModelScope.launch {
            buildsPendingDeletion.add(build.name)
            buildToDelete = null
            delay(400)
            withContext(Dispatchers.IO) { buildManager.deleteBuild(build.name) }
            buildList.removeIf { it.name == build.name }
            buildsPendingDeletion.remove(build.name)
        }
    }

    fun onAddBuild(name: String, version: String, type: String, imagePath: String?) {
        viewModelScope.launch {
            runCatching {
                val buildType = BuildType.valueOf(type)
                withContext(Dispatchers.IO) { buildManager.addBuild(name, version, buildType, imagePath) }
                refreshBuilds()
                showAddBuildDialog = false
            }.onFailure { e ->
                errorDialogMessage = e.message
            }
        }
    }

    fun onSaveBuildSettings(newName: String, newVersion: String, newType: String, newImagePath: String?, javaPath: String?, maxRam: Int?, javaArgs: String?, envVars: String?) {
        viewModelScope.launch {
            runCatching {
                val buildType = BuildType.valueOf(newType)
                showBuildSettingsScreen?.let {
                    withContext(Dispatchers.IO) {
                        buildManager.updateBuildSettings(
                            oldName = it.name,
                            newName = newName,
                            newVersion = newVersion,
                            newType = buildType,
                            newImagePath = newImagePath,
                            newJavaPath = javaPath,
                            newMaxRam = maxRam,
                            newJavaArgs = javaArgs,
                            newEnvVars = envVars
                        )
                    }
                }
                refreshBuilds()
                showBuildSettingsScreen = null
            }.onFailure { e ->
                errorDialogMessage = e.message
            }
        }
    }
    
    fun onAccountSelected(account: Account) {
        currentAccount = account
        showAccountScreen = false
    }

    fun onSettingsChanged(newSettings: AppSettings) {
        onSettingsChange(newSettings)
    }

    fun cancelScope() {
        viewModelScope.cancel()
    }
}