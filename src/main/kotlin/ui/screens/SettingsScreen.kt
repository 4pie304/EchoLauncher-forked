/*
 * Copyright 2025 Chokopieum Software
 *
 * НЕ ЯВЛЯЕТСЯ ОФИЦИАЛЬНЫМ ПРОДУКТОМ MINECRAFT. НЕ ОДОБРЕНО И НЕ СВЯЗАНО С КОМПАНИЕЙ MOJANG ИЛИ MICROSOFT.
 * Распространяется по лицензии MIT.
 * GITHUB: https://github.com/Chokopieum-Software/MateriaKraft-Launcher
 */

package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import funlauncher.AppSettings
import funlauncher.Theme
import funlauncher.auth.AccountManager
import funlauncher.game.VersionMetadataFetcher
import funlauncher.openUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.chokopieum.software.materia_launcher.generated.resources.GitHub
import org.chokopieum.software.materia_launcher.generated.resources.MLicon
import org.chokopieum.software.materia_launcher.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import ui.dialogs.LaunchSettingsDialog
import ui.viewmodel.AccountViewModel
import java.io.File
import java.net.URI
import java.util.*

object AppInfo {
    val version: String
    val buildNumber: String
    val buildSource: String
    val gradleVersion: String
    val osInfo: String
    val renderApi: String

    init {
        val props = Properties()
        try {
            this.javaClass.classLoader.getResourceAsStream("app.properties").use { stream ->
                if (stream != null) {
                    props.load(stream)
                }
            }
        } catch (e: Exception) {
            // Файл может отсутствовать в IDE, это нормально
        }
        version = props.getProperty("version", "Dev")
        buildNumber = props.getProperty("buildNumber", "Local")
        buildSource = props.getProperty("buildSource", "IDE")
        gradleVersion = props.getProperty("gradleVersion", "N/A")
        osInfo = retrieveOsInfo()
        renderApi = System.getProperty("skiko.renderApi", "Unknown")
    }

    private fun retrieveOsInfo(): String {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        if (osName.startsWith("Linux")) {
            return try {
                val osReleaseFile = File("/etc/os-release")
                if (osReleaseFile.exists()) {
                    val properties = Properties()
                    properties.load(osReleaseFile.inputStream())
                    val prettyName = properties.getProperty("PRETTY_NAME", osName)
                    "$prettyName ($osArch)"
                } else {
                    "$osName ($osArch)"
                }
            } catch (e: Exception) {
                "$osName ($osArch)"
            }
        }
        return "$osName ($osArch)"
    }
}

private enum class SettingsSection(val title: String, val icon: ImageVector) {
    Appearance("Внешний вид", Icons.Default.Palette),
    Launch("Запуск игры", Icons.Default.PlayArrow),
    Cache("Кэш", Icons.Default.Refresh), // Новая секция для кэша
    About("О программе", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onOpenJavaManager: () -> Unit,
    accountManager: AccountManager,
    coroutineScope: CoroutineScope,
    versionMetadataFetcher: VersionMetadataFetcher, // Добавлен новый параметр
    snackbarHostState: SnackbarHostState // Добавлен новый параметр
) {
    var currentSection by remember { mutableStateOf(SettingsSection.Appearance) }
    val accountViewModel = remember { AccountViewModel(accountManager, coroutineScope) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxHeight().width(200.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )
            SettingsSection.values().forEach { section ->
                val isSelected = currentSection == section
                TextButton(
                    onClick = { currentSection = section },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(section.icon, contentDescription = section.title)
                        Spacer(Modifier.width(16.dp))
                        Text(section.title)
                    }
                }
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            when (currentSection) {
                SettingsSection.Appearance -> AppearanceSettings(currentSettings, onSave)
                SettingsSection.Launch -> LaunchSettings(currentSettings, onSave, onOpenJavaManager, accountViewModel)
                SettingsSection.Cache -> CacheSettings(versionMetadataFetcher, snackbarHostState, coroutineScope)
                SettingsSection.About -> AboutScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettings(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit
) {
    var showRestartDialog by remember { mutableStateOf(false) }
    val languages = remember { mapOf("ru" to "Русский", "en" to "English") }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Перезапуск требуется") },
            text = { Text("Для применения нового языка необходимо перезапустить приложение.") },
            confirmButton = {
                Button(onClick = { showRestartDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Тема", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                val themeOptions = Theme.values().map { it.name }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                            onClick = { onSave(currentSettings.copy(theme = Theme.values()[index])) },
                            selected = currentSettings.theme.name == label
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Язык (требуется перезапуск)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = languages[currentSettings.language] ?: currentSettings.language,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    if (currentSettings.language != code) {
                                        onSave(currentSettings.copy(language = code))
                                        showRestartDialog = true
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Без системной рамки (требуется перезапуск)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = currentSettings.useBorderlessWindow,
                        onCheckedChange = {
                            onSave(currentSettings.copy(useBorderlessWindow = it))
                            showRestartDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LaunchSettings(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onOpenJavaManager: () -> Unit,
    accountViewModel: AccountViewModel
) {
    var showLaunchSettingsDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Запускать консоль при запуске игры", modifier = Modifier.weight(1f))
                    Switch(
                        checked = currentSettings.showConsoleOnLaunch,
                        onCheckedChange = { onSave(currentSettings.copy(showConsoleOnLaunch = it)) }
                    )
                }
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onOpenJavaManager, modifier = Modifier.weight(1f)) {
                        Text("Управление Java")
                    }
                    Button(onClick = { showLaunchSettingsDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("Глобальные настройки запуска")
                    }
                }
                Divider()
                Button(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Очистить сессию Microsoft")
                }
            }
        }
    }

    if (showLaunchSettingsDialog) {
        LaunchSettingsDialog(
            currentSettings = currentSettings,
            onDismiss = { showLaunchSettingsDialog = false },
            onSave = onSave
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Подтверждение") },
            text = { Text("Вы уверены, что хотите выйти из всех аккаунтов Microsoft и очистить сохраненную сессию? Вам потребуется войти заново.") },
            confirmButton = {
                Button(
                    onClick = {
                        accountViewModel.logoutFromMicrosoft()
                        showLogoutConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Да, очистить")
                }
            },
            dismissButton = {
                Button(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun CacheSettings(
    versionMetadataFetcher: VersionMetadataFetcher,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    var isUpdatingCache by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Управление кэшем", style = MaterialTheme.typography.titleMedium)
                Text("Обновите кэши версий игры и загрузчиков, чтобы получить актуальную информацию.")
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isUpdatingCache = true
                            snackbarHostState.showSnackbar("Обновление кэшей версий...", duration = SnackbarDuration.Indefinite)
                            versionMetadataFetcher.prefetchVersionMetadata { status ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(status, duration = SnackbarDuration.Short) }
                            }
                            snackbarHostState.showSnackbar("Кэши версий обновлены!", duration = SnackbarDuration.Short)
                            isUpdatingCache = false
                        }
                    },
                    enabled = !isUpdatingCache,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUpdatingCache) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Обновление...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить кэши")
                        Spacer(Modifier.width(8.dp))
                        Text("Обновить кэши версий")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    var showTechInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.MLicon),
            contentDescription = "Materia Logo",
            modifier = Modifier.size(128.dp)
        )
        Text(
            text = "Materia",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text("Версия: ${AppInfo.version}", color = MaterialTheme.colorScheme.onSurface)
        Text("Сборка: ${AppInfo.buildNumber}", color = MaterialTheme.colorScheme.onSurface)

        Spacer(Modifier.height(8.dp))

        IconButton(onClick = { openUri(URI("https://github.com/Chokopieum-Software/MateriaKraft-Launcher")) }) {
            Image(
                painter = painterResource(Res.drawable.GitHub),
                contentDescription = "GitHub",
                modifier = Modifier.size(128.dp)
            )
        }

        Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Благодарности",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text("MarkAdderly (Тестирование, изображения)", color = MaterialTheme.colorScheme.onSurface)
                Text("Zioldel (Тестирование)", color = MaterialTheme.colorScheme.onSurface)
                Text("pon4iksdonut (Изображения)", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        TextButton(onClick = { showTechInfo = !showTechInfo }) {
            Text(if (showTechInfo) "Скрыть техническую информацию" else "Показать техническую информацию")
        }

        if (showTechInfo) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val techInfoItems = mapOf(
                        "Source" to AppInfo.buildSource,
                        "Gradle" to AppInfo.gradleVersion,
                        "OS" to AppInfo.osInfo,
                        "Render API" to AppInfo.renderApi,
                    )
                    techInfoItems.forEach { (key, value) ->
                        Row {
                            Text(
                                text = "${key.padEnd(12)}: ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(text = value, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(1f))

        Text(
            text = "LEGAL: NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}
