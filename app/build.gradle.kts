plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt") // Asegúrate de que esta línea esté aquí
    id("com.apollographql.apollo3") version "3.8.2"
}

android {
    namespace = "com.example.colorblend"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.colorblend"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
kotlinOptions {
        jvmTarget = "1.8"
    }

    buildToolsVersion = "34.0.0"

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

apollo {
    service("anilist") {
        packageName.set("com.example.colorblend.graphql")
        schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // Dependencia para ViewModel
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation ("androidx.viewpager2:viewpager2:1.0.0")// Dependencia para LiveData
    implementation("androidx.recyclerview:recyclerview:1.3.0") // Agregar RecyclerView
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("androidx.security:security-crypto:1.1.0-alpha06")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation(libs.junit)
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.2")
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    // Agregar la dependencia de Glide para cargar imágenes
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0") // Para utilizar Glide con Kotlin
    implementation ("androidx.documentfile:documentfile:1.0.1")
    implementation ("androidx.media:media:1.7.0")
// Apollo GraphQL
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
}
