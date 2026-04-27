plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.light.sdk)
}

group = property("sdkGroup") as String
version = property("sdkVersion") as String

android {
    namespace = "com.thelightphone.sdk"
    compileSdk = rootProject.ext["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.ext["minSdk"] as Int
        // TODO inject
        buildConfigField("String", "LIGHT_SERVER_PACKAGE", "\"com.thelightphone.sdk.emulator\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(rootProject.ext["jvmTarget"] as String)
        targetCompatibility = JavaVersion.toVersion(rootProject.ext["jvmTarget"] as String)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(rootProject.ext["jvmTarget"] as String))
    }
}

dependencies {
    api(project(":sdk:shared"))
    api(project(":sdk:ui"))

    api(libs.compose.activity)
    api(libs.kotlinx.coroutines)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.datastore.preferences)
    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.json)
    lintChecks(project(":lint-rules"))

    testImplementation(libs.kotlin.test)
}
