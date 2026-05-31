/*
 * Copyright 2025 Chokopieum Software
 *
 * НЕ ЯВЛЯЕТСЯ ОФИЦИАЛЬНЫМ ПРОДУКТОМ MINECRAFT. НЕ ОДОБРЕНО И НЕ СВЯЗАНО С КОМПАНИЕЙ MOJANG ИЛИ MICROSOFT.
 * Распространяется по лицензии MIT.
 * GITHUB: https://github.com/Chokopieum-Software/MateriaKraft-Launcher
 */

package ui.viewmodel

import androidx.compose.runtime.*
import com.sun.management.OperatingSystemMXBean
import funlauncher.*
import funlauncher.auth.Account
import funlauncher.auth.AccountManager
import funlauncher.game.MinecraftInstaller
import funlauncher.game.VersionMetadataFetcher
import funlauncher.managers.BuildManager
import funlauncher.managers.JavaManager
import funlauncher.net.JavaDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)
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
    var showCheckmark by mutableStateOf(false)
    var isLaunchingBuildId by mutableStateOf<String?>(null)
    var runningBuild by mutableStateOf<MinecraftBuild?>(null)
    var showTestBuildWarning by mutableStateOf(false)
    var testBuildTitle by mutableStateOf<String?>(null)
    var testBuildMessage by mutableStateOf<String?>(null)
    
    private var runningProcess by mutableStateOf<Process?>(null)
    val gameOutput = MutableSharedFlow<String>(extraBufferCapacity = 1000)

    init {
        accountManager.accountsFlow
            .onEach { updatedAccounts ->
                accounts = updatedAccounts
                if (currentAccount == null || currentAccount !in updatedAccounts) {
                    currentAccount = updatedAccounts.firstOrNull()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSettingsChanged(newSettings: AppSettings) {
        onSettingsChange(newSettings)
    }

    fun refreshBuilds() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedBuilds = buildManager.loadBuilds()
            withContext(Dispatchers.Main) {
                buildList.clear()
                buildList.addAll(updatedBuilds)
            }
        }
    }

    fun cancelScope() {
        viewModelScope.cancel()
        runningProcess?.destroy()
    }

    private fun stopGame() {
        runningProcess?.destroy()
        runningProcess = null
        runningBuild = null
        daemonStatus = "STOPPED"
    }

    private suspend fun attachToProcess(process: Process) {
        withContext(Dispatchers.IO) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    logger.info("[Minecraft] $line") // Логируем в консоль IDE/терминала
                    gameOutput.tryEmit(line) // Используем tryEmit чтобы не блокироваться
                }
            }
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
            val finalEnvVars = build.envVars ?: appState.settings.envVars ?: ""

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
                
                runningProcess?.let { attachToProcess(it) }
                
                // Wait for process to exit
                val exitCode = runningProcess?.waitFor() ?: -1
                logger.info("Minecraft process exited with code $exitCode")
                
                withContext(Dispatchers.Main) {
                    isLaunchingBuildId = null
                    runningBuild = null
                    daemonStatus = "STOPPED"
                    showGameConsole = false
                }
            }
        }.onFailure {
            logger.error("Error during launch", it)
            withContext(Dispatchers.Main) {
                errorDialogMessage = it.message ?: "Неизвестная ошибка при запуске."
                isLaunchingBuildId = null
                runningBuild = null
                daemonStatus = "STOPPED"
            }
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
                            if (result.isSuccess) {
                                launchMinecraft(build, result.getOrThrow().path, account)
                            } else {
                                errorDialogMessage = "Не удалось найти или скачать Java ${recommendedVersion}. Укажите путь вручную."
                                isLaunchingBuildId = null
                            }
                        }
                    }
                }
            } else {
                val javaPath = build.javaPath ?: appState.settings.javaPath
                launchMinecraft(build, javaPath, account)
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

    fun onAddBuild(name: String, version: String, type: String, imagePath: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            buildManager.addBuild(name, version, BuildType.valueOf(type), imagePath)
            withContext(Dispatchers.Main) {
                refreshBuilds()
                showAddBuildDialog = false
            }
        }
    }

    fun onSaveBuildSettings(
        newName: String,
        newVersion: String,
        newType: BuildType,
        newImagePath: String?,
        javaPath: String?,
        maxRam: Int?,
        javaArgs: String?,
        envVars: String?
    ) {
        showBuildSettingsScreen?.let { build ->
            viewModelScope.launch(Dispatchers.IO) {
                buildManager.updateBuildSettings(
                    oldName = build.name,
                    newName = newName,
                    newVersion = newVersion,
                    newType = newType,
                    newImagePath = newImagePath,
                    newJavaPath = javaPath,
                    newMaxRam = maxRam,
                    newJavaArgs = javaArgs,
                    newEnvVars = envVars
                )
                withContext(Dispatchers.Main) {
                    refreshBuilds()
                    showBuildSettingsScreen = null
                }
            }
        }
    }

    fun onAccountSelected(account: Account) {
        currentAccount = account
        showAccountScreen = false
    }

    fun onConfirmRamWarning(build: MinecraftBuild) {
        showRamWarningDialog = null
        performLaunch(build)
    }

    fun onConfirmDelete(build: MinecraftBuild) {
        buildsPendingDeletion.add(build.name)
        viewModelScope.launch(Dispatchers.IO) {
            buildManager.deleteBuild(build.name)
            withContext(Dispatchers.Main) {
                buildList.remove(build)
                buildsPendingDeletion.remove(build.name)
                buildToDelete = null
            }
        }
    }

    fun onDeleteBuildClick(build: MinecraftBuild) {
        buildToDelete = build
    }

    fun onBuildsReordered(from: Int, to: Int) {
        if (from == to) return
        val currentBuilds = buildList.toList()
        val mutableBuilds = currentBuilds.toMutableList()
        val item = mutableBuilds.removeAt(from)
        mutableBuilds.add(to, item)

        buildList.clear()
        buildList.addAll(mutableBuilds)

        // Save order to DB
        viewModelScope.launch(Dispatchers.IO) {
            buildManager.reorderBuilds(mutableBuilds)
        }
    }
}