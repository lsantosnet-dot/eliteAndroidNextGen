plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mercador.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mercador.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // As telas (Compose) vivem em :ui e são compiladas aqui também, sem as classes só de PC
    // (janela e renderização de PNG). Um só código para o desktop e para o Android.
    sourceSets["main"].java.srcDir("../ui/src/main/kotlin")

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Classes de :ui que só existem no PC (janela, Skia): ficam de fora do APK.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude("**/com/mercador/ui/Main.kt", "**/com/mercador/ui/Preview.kt")
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
