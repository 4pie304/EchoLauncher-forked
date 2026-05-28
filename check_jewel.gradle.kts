repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
}
configurations.create("jewelCheck")
dependencies {
    "jewelCheck"("org.jetbrains.jewel:jewel-window-decorated:+")
}
tasks.register("checkJewelVersion") {
    doLast {
        configurations["jewelCheck"].resolvedConfiguration.resolvedArtifacts.forEach {
            println("RESOLVED: " + it.moduleVersion.id)
        }
    }
}