/*
 * Copyright 2025 Chokopieum Software
 *
 * НЕ ЯВЛЯЕТСЯ ОФИЦИАЛЬНЫМ ПРОДУКТОМ MINECRAFT. НЕ ОДОБРЕНО И НЕ СВЯЗАНО С КОМПАНИЕЙ MOJANG ИЛИ MICROSOFT.
 * Распространяется по лицензии MIT.
 * GITHUB: https://github.com/Chokopieum-Software/MateriaKraft-Launcher
 */

package funlauncher.net

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.util.*

/**
 * Представляет одну задачу загрузки.
 * @param id Уникальный идентификатор задачи.
 * @param description Описание того, что загружается (например, "Java 21" или "Minecraft 1.20.4").
 * @param job Задание корутины, выполняющее загрузку, для возможности отмены.
 * @param progress Прогресс от 0.0f до 1.0f.
 * @param status Текущий текстовый статус (например, "Скачивание..." или "Распаковка...").
 */
data class DownloadTask(
    val id: String,
    val description: String,
    val job: Job,
    val progress: MutableState<Float> = mutableStateOf(0f),
    val status: MutableState<String> = mutableStateOf("В очереди...")
)

/**
 * Глобальный менеджер для отслеживания всех активных загрузок в приложении.
 * Это синглтон (object), чтобы к нему можно было получить доступ из любого места.
 */
object DownloadManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Список активных задач, который может наблюдаться Compose.
     */
    val tasks = mutableStateListOf<DownloadTask>()

    /**
     * Начинает новую задачу и добавляет ее в список отслеживания.
     * @return Созданная задача.
     */
    fun startTask(description: String, job: Job): DownloadTask {
        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            description = description,
            job = job
        )
        scope.launch {
            tasks.add(task)
        }
        return task
    }

    /**
     * Обновляет прогресс и статус существующей задачи.
     */
    fun updateTask(id: String, progress: Float, status: String) {
        scope.launch {
            tasks.find { it.id == id }?.let {
                it.progress.value = progress
                it.status.value = status
            }
        }
    }

    /**
     * Завершает задачу и удаляет ее из списка.
     */
    fun endTask(id: String) {
        scope.launch {
            tasks.removeAll { it.id == id }
        }
    }

    /**
     * Отменяет задачу, связанную с ней корутину, и удаляет из списка.
     */
    fun cancelTask(id: String) {
        scope.launch {
            tasks.find { it.id == id }?.let {
                it.job.cancel() // Отменяем корутину
                tasks.remove(it) // Удаляем из списка
            }
        }
    }
}