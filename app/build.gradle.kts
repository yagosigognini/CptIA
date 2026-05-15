import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")
}

// ✅ Função para ler o local.properties
fun getLocalProperty(key: String, project: org.gradle.api.Project): String {
    val properties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }
    return properties.getProperty(key) ?: ""
}


android {
    namespace = "br.com.CapitularIA"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.CapitularIA"
        minSdk = 29
        targetSdk = 36
        versionCode = 10
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Adiciona a chave da API ao BuildConfig
        //    Certifique-se que 'getLocalProperty("GOOGLE_BOOKS_API_KEY", project)' está correto
        buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"${getLocalProperty("GOOGLE_BOOKS_API_KEY", project)}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ✅ Adiciona a chave da API ao BuildConfig de Release também
            buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"${getLocalProperty("GOOGLE_BOOKS_API_KEY", project)}\"")
        }
        // ✅ Opcional, mas recomendado: Adicione ao Debug também se precisar testar
        debug {
            buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"${getLocalProperty("GOOGLE_BOOKS_API_KEY", project)}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true // Mantém isso
    }
}

dependencies {
    // --- Padrão & UI ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // --- Jetpack Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
    implementation(libs.androidx.compose.runtime.livedata)

    // --- Firebase (BOM - Bill of Materials) ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // --- ViewModel & Navegação ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- Carregamento de Imagens ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- NETWORK & JSON PARSING (RETROFIT + GSON) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation(libs.androidx.compose.foundation)

    // --- Testes ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}