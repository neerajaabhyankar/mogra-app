plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mogralabs.mogra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mogralabs.mogra"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // Locales the app actually ships strings for. mr and hi land here next.
        resourceConfigurations += listOf("en")
        // PyTorch ships native libraries for four ABIs and they dominate the APK; every
        // Android phone this targets is arm64. Without this the debug APK is 235 MB.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key on purpose: there is no upload key yet, and this
            // way a release build installs straight over a debug one for testing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // Compress the native libraries in the APK. Uncompressed .so files load faster and
        // cost no extra disk, but PyTorch's is 54 MB on its own and that pushes the APK past
        // what can be sent over a chat. Compressed it is roughly a third of that.
        jniLibs { useLegacyPackaging = true }
    }
    androidResources { noCompress += listOf("bin") }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.pytorch.android.lite)
    // ui-tooling is only for @Preview, which nothing here uses, and it is megabytes of dex
    testImplementation(libs.junit)
    // android.jar stubs org.json in unit tests; the real one lets the golden tests read meta.json
    testImplementation(libs.json)
}
