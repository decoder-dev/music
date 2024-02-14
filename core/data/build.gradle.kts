plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
}

android {
    namespace = "it.decoder.compose.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}

dependencies {
    // Добавьте зависимость для Compose
    implementation("androidx.compose.runtime:runtime:1.6.1")
}