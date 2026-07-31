import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

/**
 * Параметры релизной подписи. Лежат в keystore.properties в корне проекта —
 * файл НЕ попадает в репозиторий (см. .gitignore), потому что содержит пароли.
 * Образец — keystore.properties.example.
 *
 * Зачем это здесь. Раньше в Gradle подписи не было вовсе: `assembleRelease`
 * выдавал НЕподписанный APK, и подписывать приходилось мастером Android Studio.
 * Оттуда же росла путаница с ключами — отладочная сборка подписана ключом
 * `CN=Android Debug`, релизная своим, а Android не даёт обновить приложение,
 * если подпись сменилась: «конфликтует с другим пакетом». Теперь ключ один и
 * задан в проекте, поэтому релизы обновляются поверх друг друга.
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystorePropsFile.exists() &&
    keystoreProps.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true

android {
    namespace  = "com.nexory.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexory.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 2
        versionName   = "1.2.0"

        // Адрес бэкенда — единственное место, где он задаётся.
        // Остальные адреса (REST, WebSocket, юр. документы) выводятся из него
        // в NexoryConfig. Раньше здесь стоял неиспользуемый api.nexory.app,
        // а реальный адрес был захардкожен в коде — источники разъезжались.
        //
        // ⚠️ ПЕРЕД РЕЛИЗОМ В GOOGLE PLAY заменить на https://<домен>
        buildConfigField("String", "SERVER_ORIGIN", "\"http://186.246.12.170:3000\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile     = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias      = keystoreProps.getProperty("keyAlias")
                keyPassword   = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Без keystore.properties APK останется неподписанным — это лучше,
            // чем молча подписать отладочным ключом и получить сборку, которую
            // невозможно поставить поверх релизной
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
        }
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

    // ---- Room (локальный кэш для оффлайн-режима) ----
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ---- Coil (загрузка изображений) ----
    implementation("io.coil-kt:coil-compose:2.7.0")

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