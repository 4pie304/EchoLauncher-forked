package funlauncher.utils

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object LogCollector {
    private val logBuffer = StringBuilder()
    private val originalOut = System.out
    private val originalErr = System.err
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

    @Volatile
    private var isRedirected = false

    fun init() {
        if (!isRedirected) {
            val customOut = object : PrintStream(ByteArrayOutputStream()) {
                override fun write(buf: ByteArray, off: Int, len: Int) {
                    val message = String(buf, off, len)
                    synchronized(logBuffer) {
                        logBuffer.append(message)
                    }
                    originalOut.write(buf, off, len) // Also print to original console
                }
            }
            val customErr = object : PrintStream(ByteArrayOutputStream()) {
                override fun write(buf: ByteArray, off: Int, len: Int) {
                    val message = String(buf, off, len)
                    synchronized(logBuffer) {
                        logBuffer.append(message)
                    }
                    originalErr.write(buf, off, len) // Also print to original console
                }
            }
            System.setOut(customOut)
            System.setErr(customErr)
            isRedirected = true
            println("[LogCollector] Log redirection initialized.")
        }
    }

    fun getCollectedLogs(): String {
        synchronized(logBuffer) {
            return logBuffer.toString()
        }
    }

    fun saveLogsToDesktop(): Path? {
        val logs = getCollectedLogs()
        if (logs.isBlank()) {
            println("[LogCollector] No logs to save.")
            return null
        }

        val desktopPath = Paths.get(System.getProperty("user.home"), "Desktop")
        desktopPath.createDirectories() // Ensure desktop directory exists

        val timestamp = dateFormat.format(Date())
        val fileName = "MateriaLauncher_Logs_$timestamp.txt"
        val filePath = desktopPath.resolve(fileName)

        return try {
            filePath.writeText(logs)
            println("[LogCollector] Logs saved to $filePath")
            filePath
        } catch (e: Exception) {
            System.err.println("[LogCollector] Failed to save logs to desktop: ${e.message}")
            null
        }
    }
}
