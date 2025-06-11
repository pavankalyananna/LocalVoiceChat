plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.localvoicechat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.localvoicechat"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
    buildFeatures {
        viewBinding = true
    }
}



dependencies {

    // Socket.IO
    implementation("io.socket:socket.io-client:2.0.0") {
        exclude(group = "org.json", module = "json")
    }

    // JSON
    implementation("org.json:json:20210307")
    implementation("io.socket:engine.io-server:6.0.0")
    implementation("io.socket:socket.io-server:4.0.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.preference.ktx)

    // Use explicit versions for these
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.multidex:multidex:2.0.1")

    // WebRTC - use the version from libs
    implementation("io.github.webrtc-sdk:android:125.6422.06.1")

    // Socket.IO for signaling
    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    // Permission handling
    implementation(libs.permissionx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}