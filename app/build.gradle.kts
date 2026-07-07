plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace  = "com.nexory.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexory.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"

        // Базовый URL задаётся здесь, не хардкодится в коде
        buildConfigField("String", "API_BASE_URL",
            "\"https://api.nexory.app/api/v1/\"")
        buildConfigField("String", "WS_BASE_URL",
            "\"wss://api.nexory.app/ws\"")
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // ---- Compose BOM — фиксирует версии всех Compose-библиотек ----
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")

    // ---- Навигация ----
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ---- Hilt DI ----
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ---- Сеть: Retrofit + OkHttp + Gson ----
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // WebSocket встроен в OkHttp — отдельный артефакт не нужен

    // ---- Gson ----
    implementation("com.google.code.gson:gson:2.11.0")

    // ---- DataStore (хранение токенов) ----
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ---- Coil (загрузка изображений) ----
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ---- uCrop (обрезка/выбор кадра фото) ----
    implementation("com.github.yalantis:ucrop:2.2.8")

    // ---- Lifecycle / ViewModel ----
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // ---- Firebase FCM ----
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ---- Coroutines ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ---- Debug ----
    debugImplementation("androidx.compose.ui:ui-tooling")
}