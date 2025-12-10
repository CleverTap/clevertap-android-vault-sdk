import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

val libraryVersion = "1.0.0"
val artifact = "clevertap-vault-sdk"
val libraryDescription = "The CleverTap Vault SDK for Android - Secure tokenization of PII data"
val publishedGroupId = "com.clevertap.android"

val siteUrl = "https://github.com/CleverTap/clevertap-android-vault-sdk"
val gitUrl = "https://github.com/CleverTap/clevertap-android-vault-sdk.git"

val licenseName = "The Apache Software License, Version 2.0"
val licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"

val developerId = "clevertap"
val developerName = "CleverTap"
val developerEmail = "support@clevertap.com"

val versionParts = libraryVersion.split(".")
val major = versionParts[0]
val minor = versionParts[1]
val patch = versionParts[2]

version = libraryVersion
group = publishedGroupId

android {
    namespace = "com.clevertap.android.vault.sdk"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        buildConfigField("int", "VERSION_CODE", "${major}0${minor}0${patch}")
        buildConfigField("String", "VERSION_NAME", "\"$libraryVersion\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "SDK_VERSION_STRING",
                "\"!SDK-VERSION-STRING!:$publishedGroupId:$artifact:$major.$minor.$patch.0\""
            )
        }
        release {
            buildConfigField(
                "String",
                "SDK_VERSION_STRING",
                "\"!SDK-VERSION-STRING!:$publishedGroupId:$artifact:$major.$minor.$patch.0\""
            )
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
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    libraryVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (name.lowercase().contains("release")) {
                outputImpl.outputFileName = "$artifact-$libraryVersion.aar"
            } else if (name.lowercase().contains("debug")) {
                outputImpl.outputFileName = "$artifact-$name-$libraryVersion.aar"
            }
        }
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs("-Xmx4g", "-noverify")
            }
        }
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.coroutine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val localProperties: Properties = gradleLocalProperties(rootDir, providers)
localProperties.forEach { name, value ->
    ext[name.toString()] = value
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(publishedGroupId, artifact, libraryVersion)

    pom {
        name.set(artifact)
        description.set(libraryDescription)
        url.set(siteUrl)

        licenses {
            license {
                name.set(licenseName)
                url.set(licenseUrl)
            }
        }

        developers {
            developer {
                id.set(developerId)
                name.set(developerName)
                email.set(developerEmail)
            }
        }

        scm {
            connection.set("scm:git:$gitUrl")
            developerConnection.set("scm:git:$gitUrl")
            url.set(siteUrl)
        }
    }
}