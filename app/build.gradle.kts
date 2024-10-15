plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")  // Sử dụng cú pháp chính xác để áp dụng plugin
}

android {
    namespace = "com.example.yogaclass"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.yogaclass"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase BoM - quản lý phiên bản các dependency Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    // Firebase Realtime Database SDK
    implementation("com.google.firebase:firebase-database")
    implementation(libs.firebase.auth)

    // WorkManager
    implementation("androidx.work:work-runtime:2.7.0")  // Thêm WorkManager để quản lý đồng bộ

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
