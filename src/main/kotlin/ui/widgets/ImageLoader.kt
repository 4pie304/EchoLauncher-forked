package ui.widgets

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import funlauncher.managers.CacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object ImageLoader {
    private val inMemoryCache = ConcurrentHashMap<String, ImageBitmap>()
    private lateinit var cacheManager: CacheManager // Will be initialized in main

    fun init(cacheManager: CacheManager) {
        this.cacheManager = cacheManager
    }

    @Composable
    fun rememberImageBitmap(filePath: String?): ImageBitmap? {
        var imageBitmap by remember(filePath) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(filePath) {
            if (filePath == null) {
                imageBitmap = null
                return@LaunchedEffect
            }

            if (inMemoryCache.containsKey(filePath)) {
                imageBitmap = inMemoryCache[filePath]
                return@LaunchedEffect
            }

            imageBitmap = withContext(Dispatchers.IO) {
                val file = File(filePath)
                if (file.exists()) {
                    try {
                        file.inputStream().use { stream ->
                            loadImageBitmap(stream).also { bitmap ->
                                inMemoryCache[filePath] = bitmap
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }
            }
        }
        return imageBitmap
    }

    @Composable
    fun rememberImageBitmapFromUrl(url: String?): ImageBitmap? {
        var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(url) {
            if (url == null || url.isBlank()) {
                imageBitmap = null
                return@LaunchedEffect
            }

            if (inMemoryCache.containsKey(url)) {
                imageBitmap = inMemoryCache[url]
                return@LaunchedEffect
            }

            imageBitmap = withContext(Dispatchers.IO) {
                val cacheFile = cacheManager.getImageCacheFile(url)

                // Попытка загрузить из кэша
                if (cacheFile.exists()) {
                    try {
                        // Читаем файл в байты, чтобы избежать проблем с потоками
                        val bytes = cacheFile.readBytes()
                        ByteArrayInputStream(bytes).use { stream ->
                            loadImageBitmap(stream).also { bitmap ->
                                inMemoryCache[url] = bitmap
                            }
                        }
                    } catch (e: Exception) {
                        println("ImageLoader: Error loading image from disk cache for URL: $url. Deleting file. Error: ${e.message}")
                        cacheFile.delete()
                        null
                    }
                } else {
                    null
                } ?: try {
                    // Если в кэше нет или ошибка, качаем
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "MateriaKraft-Launcher")
                    connection.connect()

                    if (connection.responseCode == 200) {
                        connection.inputStream.use { input ->
                            // Читаем весь поток в память
                            val buffer = ByteArrayOutputStream()
                            input.copyTo(buffer)
                            val bytes = buffer.toByteArray()

                            // Создаем Bitmap из байтов
                            val bitmap = ByteArrayInputStream(bytes).use { loadImageBitmap(it) }
                            
                            // Сохраняем в кэш на диске
                            try {
                                cacheFile.parentFile?.mkdirs()
                                cacheFile.writeBytes(bytes)
                            } catch (e: Exception) {
                                println("ImageLoader: Failed to save to cache: ${e.message}")
                            }

                            // Сохраняем в память
                            inMemoryCache[url] = bitmap
                            bitmap
                        }
                    } else {
                        println("ImageLoader: Server returned ${connection.responseCode} for URL: $url")
                        null
                    }
                } catch (e: Exception) {
                    println("ImageLoader: Error downloading image from URL: $url. Error: ${e.message}")
                    null
                }
            }
        }
        return imageBitmap
    }
}
