plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("application")
}

group = "org.chokopieum.software.ui"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.components.resources)
}

compose.resources {
    packageOfResClass = "org.chokopieum.software.ui.generated.resources"
    publicResClass = true
    generateResClass = always
}

// Задаём главный класс по умолчанию
application {
    mainClass.set("org.chokopieum.software.ui.MateriaCircularProgressIndicatorPreviewKt")
}

// Отдельная задача для запуска превью индикатора загрузки
tasks.register<JavaExec>("runProgressIndicatorPreview") {
    group = "application"
    description = "Run MateriaCircularProgressIndicator Preview"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.chokopieum.software.ui.MateriaCircularProgressIndicatorPreviewKt")
}
