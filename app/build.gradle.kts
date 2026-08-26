plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.crashlytics)
    id ("kotlin-kapt")
    id ("dagger.hilt.android.plugin")
    // La version del plugin de serialization DEBE seguir a la del compilador
    // de Kotlin. Fijarla en 1.9.20 con Kotlin 2.1.20 aborta la configuracion.
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.smartbe.hapticcontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartbe.hapticcontrol"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.20"
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        jniLibs {
            // libble24.so ya viene compilado del APK original. Si se comprime,
            // System.loadLibrary falla en algunos dispositivos.
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/{AL2.0,LGPL2.1}"
            )
        }
    }

}

dependencies {
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // collectAsStateWithLifecycle() vive aqui, no en lifecycle-runtime-ktx
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.database)
    implementation(libs.googleid)
    implementation(libs.material3)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    kapt(libs.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ui.auth)
    implementation(libs.facebook.android.sdk)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.gson)
    implementation (libs.converter.gson)
    implementation(libs.coil.compose)
    implementation(libs.barcode.scanning)
    implementation (libs.androidx.room.ktx)

    implementation(libs.okhttp)

    testImplementation(libs.bundles.testing)
    testImplementation (libs.androidx.room.testing)
    testImplementation(libs.hilt.android.testing)

    testImplementation(libs.junit)
    testImplementation (libs.kotlinx.coroutines.test)
    testImplementation (libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation (libs.junit.jupiter.api)
    testImplementation(kotlin("test"))

    implementation (libs.poi)

    implementation(libs.bundles.firebase)
    implementation(libs.google.firebase.firestore)
    implementation(libs.google.firebase.auth)

    implementation(libs.play.services.auth)

    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

    // Google Sheets API
    implementation(libs.google.api.client)
    implementation(libs.google.api.client.android)
    implementation("com.google.apis:google-api-services-sheets:v4-rev20230227-2.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")

    // OAuth2 para Google
    implementation(libs.google.auth.library.oauth2.http)

    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // CameraX para la vista de cámara
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation("androidx.bluetooth:bluetooth:1.0.0-alpha02")
    implementation("com.juul.kable:core:0.24.0")
}
kapt {
    correctErrorTypes = true
}