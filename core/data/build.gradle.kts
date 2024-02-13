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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_10
        targetCompatibility = JavaVersion.VERSION_1_10
    }
    buildToolsVersion = "34.0.0"
    ndkVersion = "25.1.8937393"
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.formatting)
}
