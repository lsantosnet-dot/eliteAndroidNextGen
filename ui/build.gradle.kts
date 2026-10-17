plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.runtime)
    implementation(compose.desktop.currentOs)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

// Gera os PNGs das 4 telas em docs/ui (sem janela): ./gradlew :ui:renderScreens
tasks.register<JavaExec>("renderScreens") {
    group = "documentation"
    mainClass.set("com.mercador.ui.PreviewKt")
    classpath = sourceSets["main"].runtimeClasspath
    args(rootProject.layout.projectDirectory.dir("docs/ui").asFile.path)
}
