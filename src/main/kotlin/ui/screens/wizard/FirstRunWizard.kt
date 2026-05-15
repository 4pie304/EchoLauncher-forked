/*
 * Copyright 2025 Chokopieum Software
 *
 * НЕ ЯВЛЯЕТСЯ ОФИЦИАЛЬНЫМ ПРОДУКТОМ MINECRAFT. НЕ ОДОБРЕНО И НЕ СВЯЗАНО С КОМПАНИЕЙ MOJANG ИЛИ MICROSOFT.
 * Распространяется по лицензии MIT.
 * GITHUB: https://github.com/Chokopieum-Software/MateriaKraft-Launcher
 */

package ui.screens.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import funlauncher.auth.AccountManager
import funlauncher.AppSettings
import funlauncher.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class WizardStep {
    WELCOME,
    APPEARANCE,
    ACCOUNT,
    FINISH
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FirstRunWizard(
    accountManager: AccountManager?,
    initialTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    onWizardComplete: (AppSettings) -> Unit
) {
    var currentStep by remember { mutableStateOf(WizardStep.WELCOME) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun handleLogin() {
        if (accountManager == null) {
            authSuccess = true
            return
        }
        scope.launch(Dispatchers.IO) {
            isAuthenticating = true
            authError = null
            authSuccess = false
            try {
                val success = accountManager.loginWithMicrosoft()
                if (success) {
                    authSuccess = true
                } else {
                    authError = "Не удалось войти в аккаунт Microsoft."
                }
            } catch (e: Exception) {
                authError = "Критическая ошибка: ${e.message}"
            } finally {
                isAuthenticating = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок и прогресс
            Text(
                text = "Настройка Materia Launcher",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (currentStep.ordinal + 1) / WizardStep.values().size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Контент шагов
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = currentStep) { step ->
                    when (step) {
                        WizardStep.WELCOME -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Добро пожаловать в Materia!", style = MaterialTheme.typography.headlineLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Давайте пройдем быструю настройку лаунчера для его первого запуска.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        WizardStep.APPEARANCE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Внешний вид", style = MaterialTheme.typography.headlineSmall)
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("Тема", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val themeOptions = Theme.values().map { it.name }
                                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                            themeOptions.forEachIndexed { index, label ->
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                                                    onClick = { onThemeChange(Theme.values()[index]) },
                                                    selected = initialTheme.name == label
                                                ) { Text(label) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        WizardStep.ACCOUNT -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Аккаунт Minecraft", style = MaterialTheme.typography.headlineSmall)
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Для игры требуется лицензионный аккаунт", style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        when {
                                            isAuthenticating -> CircularProgressIndicator()
                                            authSuccess -> Text("Вы успешно вошли!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                            else -> Button(onClick = ::handleLogin, modifier = Modifier.height(48.dp)) {
                                                Text("Войти через Microsoft")
                                            }
                                        }
                                        
                                        authError?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        WizardStep.FINISH -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Всё готово!", style = MaterialTheme.typography.headlineLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Настройка завершена. Теперь вы можете наслаждаться игрой.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            // Навигация
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep != WizardStep.WELCOME) {
                    OutlinedButton(onClick = { 
                        currentStep = WizardStep.values()[currentStep.ordinal - 1] 
                    }) {
                        Text("Назад")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentStep != WizardStep.FINISH) {
                    Button(onClick = { 
                        currentStep = WizardStep.values()[currentStep.ordinal + 1] 
                    }) {
                        Text("Далее")
                    }
                } else {
                    Button(onClick = {
                        val finalSettings = AppSettings(
                            theme = initialTheme
                        )
                        onWizardComplete(finalSettings)
                    }) {
                        Text("Завершить")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FirstRunWizardPreview() {
    var theme by remember { mutableStateOf(Theme.Dark) }
    
    MaterialTheme {
        FirstRunWizard(
            accountManager = null, // Mock account manager can be null for preview
            initialTheme = theme,
            onThemeChange = { theme = it },
            onWizardComplete = {}
        )
    }
}