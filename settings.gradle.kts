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
// :app (Jetpack Compose) entra na etapa 2, quando houver laço de comércio para mostrar.
