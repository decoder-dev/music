plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)

    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    // Добавьте зависимость для Compose
    implementation("androidx.compose.runtime:runtime:1.6.1")
}