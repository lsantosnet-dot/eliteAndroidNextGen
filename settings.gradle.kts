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

// :app (Android) só entra quando há Android SDK (ANDROID_HOME / ANDROID_SDK_ROOT ou sdk.dir em local.properties;
// o Android Studio cria o local.properties sozinho). Sem SDK, :core e :ui continuam compilando e testando.
val localProps = file("local.properties")
val hasSdk = System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null ||
    (localProps.exists() && localProps.readText().contains("sdk.dir"))
if (hasSdk) include(":app") else logger.lifecycle("Android SDK não encontrado: módulo :app ignorado.")
// :ui é a interface Compose (dossiê noir): telas puras sobre o :core, testáveis e renderizáveis sem dispositivo.
