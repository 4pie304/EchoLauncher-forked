package funlauncher.utils

object VersionMatcher {

    /**
     * Сравнивает две версии Minecraft, чтобы определить, совместима ли версия сборки с версией игры, указанной для мода/ресурспака.
     *
     * @param buildVersion версия сборки (например, "1.12.2-fabric-0.14.0").
     * @param gameVersion версия игры, указанная для модификации (например, "1.12" или "1.12.2").
     * @return true, если версия сборки совместима с версией игры.
     */
    fun isCompatible(buildVersion: String, gameVersion: String): Boolean {
        try {
            // 1. Извлекаем "чистую" версию из версии сборки
            val cleanBuildVersion = buildVersion.split("-").first()

            // 2. Разбиваем версии на компоненты (major, minor, patch)
            val buildParts = cleanBuildVersion.split(".").map { it.toInt() }
            val gameParts = gameVersion.split(".").map { it.toInt() }

            // 3. Сравниваем компоненты
            if (gameParts.size > buildParts.size) {
                // Версия игры более специфична, чем версия сборки (например, игра для 1.12.2, а сборка 1.12) - несовместимо
                return false
            }

            for (i in gameParts.indices) {
                if (buildParts[i] != gameParts[i]) {
                    return false
                }
            }
            
            return true
        } catch (e: NumberFormatException) {
            // Если не удалось распарсить, используем старый метод
            return buildVersion.startsWith(gameVersion)
        }
    }
}