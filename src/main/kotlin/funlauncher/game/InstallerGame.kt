/*
 * Copyright 2025 Chokopieum Software
 *
 * НЕ ЯВЛЯЕТСЯ ОФИЦИАЛЬНЫМ ПРОДУКТОМ MINECRAFT. НЕ ОДОБРЕНО И НЕ СВЯЗАНО С КОМПАНИЕЙ MOJANG ИЛИ MICROSOFT.
 * Распространяется по лицензии MIT.
 * GITHUB: https://github.com/Chokopieum-Software/MateriaKraft-Launcher
 */

package funlauncher.game

import funlauncher.MinecraftBuild
import funlauncher.auth.Account
import funlauncher.managers.BuildManager
import funlauncher.managers.PathManager
import funlauncher.net.DownloadManager
import funlauncher.net.FileDownloader
import io.ktor.client.plugins.*
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.io.path.exists
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.job
import org.slf4j.LoggerFactory

/**
 * Main installer class that coordinates the launch process.
 * It delegates tasks to VersionMetadataFetcher, FileDownloader, and GameLauncher.
 */
class MinecraftInstaller(private val build: MinecraftBuild, private val buildManager: BuildManager) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val pathManager: PathManager = PathManager(PathManager.getDefaultAppDataDirectory())

    suspend fun launchGame(
        account: Account, javaPath: String, maxRamMb: Int, javaArgs: String, envVars: String
    ): Process {
        val task = DownloadManager.startTask("Minecraft ${build.version}", coroutineContext.job)
        try {
            logger.info("Starting launch for ${build.name} (${build.version})")
            logger.info("System: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}, Java: $javaPath")

            // 1. Fetch Version Metadata
            DownloadManager.updateTask(task.id, 0.05f, "Получение метаданных...")
            val metadataFetcher = VersionMetadataFetcher(buildManager, pathManager)
            val versionInfo = metadataFetcher.getVersionInfo(build, task)

            // 2. Download Files
            DownloadManager.updateTask(task.id, 0.1f, "Загрузка файлов...")
            val fileDownloader = FileDownloader(versionInfo, pathManager, buildManager)
            fileDownloader.downloadRequiredFiles(build) { progress, status ->
                DownloadManager.updateTask(task.id, 0.1f + progress * 0.8f, status)
            }

            // 3. Create Payload and Launch
            DownloadManager.updateTask(task.id, 0.95f, "Запуск игры...")
            val gameLauncher = GameLauncher(versionInfo, build, pathManager)
            val payload = gameLauncher.createLaunchPayload(account, javaPath, maxRamMb, javaArgs, envVars)
            
            val processBuilder = ProcessBuilder(payload.command)
                .directory(File(payload.workDir))
                .redirectErrorStream(true) // Объединяем stderr и stdout

            // Добавляем переменные окружения
            processBuilder.environment().putAll(payload.environment)

            logger.info("Starting process with command: ${payload.command.joinToString(" ")}")
            val process = withContext(Dispatchers.IO) {
                processBuilder.start()
            }

            DownloadManager.updateTask(task.id, 1.0f, "Игра запущена")
            logger.info("Game started successfully.")
            delay(2000) // Даем увидеть 100%
            return process

        } catch (e: Exception) {
            handleLaunchException(e)
        } finally {
            DownloadManager.removeTask(task.id)
        }
    }

    private fun handleLaunchException(e: Exception): Nothing {
        logger.error("Launch failed", e)
        when (e) {
            is UnknownHostException, is ConnectException, is HttpRequestTimeoutException -> {
                val versionId = build.modloaderVersion ?: build.version
                val jsonFile = pathManager.getGlobalVersionsDir().resolve(versionId).resolve("$versionId.json")
                val message = if (jsonFile.exists()) {
                    "Не удалось скачать некоторые файлы игры. Проверьте подключение к интернету и попробуйте снова."
                } else {
                    "Не удалось получить информацию о версии '$versionId'. Для первого запуска этой версии требуется подключение к интернету."
                }
                throw IllegalStateException(message, e)
            }
            else -> throw e // rethrow other exceptions
        }
    }
}