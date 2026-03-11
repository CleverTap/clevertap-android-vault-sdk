import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.clevertap.demo.ctzeropii"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.clevertap.demo.ctzeropii"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OAUTH_URL",
            "\"${localProperties.getProperty("OAUTH_URL", "")}\""
        )
        buildConfigField(
            "String",
            "OAUTH_CLIENT_ID",
            "\"${localProperties.getProperty("OAUTH_CLIENT_ID", "")}\""
        )
        buildConfigField(
            "String",
            "OAUTH_CLIENT_SECRET",
            "\"${localProperties.getProperty("OAUTH_CLIENT_SECRET", "")}\""
        )
        buildConfigField("String", "API_URL", "\"${localProperties.getProperty("API_URL", "")}\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(project(":clevertap-android-zeropii-sdk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)

    // Networking (for OAuth token provider)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}