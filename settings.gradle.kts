pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mercador"

// :core é Kotlin/JVM puro (geração, economia, facções, storylets), testável sem Android SDK.
include(":core")
include(":ui")
// :ui é a interface Compose (dossiê noir): telas puras sobre o :core, testáveis e renderizáveis sem dispositivo.
