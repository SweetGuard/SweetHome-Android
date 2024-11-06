plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.example.sweethome"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sweethome"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
//            buildConfigField("String", "SERVER_URL", "\"http://125.131.208.226:8000\"")
            buildConfigField("String", "SERVER_URL", "\"http://192.168.0.184:8000\"")
            buildConfigField("String", "WS_SERVER_URL", "\"ws://192.168.0.184:8000/ws/audio\"")
            isMinifyEnabled = false
        }
        release {
//            buildConfigField("String", "SERVER_URL", "\"http://125.131.208.226:8000\"")
            buildConfigField("String", "SERVER_URL", "\"http://192.168.0.184:8000\"")
            buildConfigField("String", "WS_SERVER_URL", "\"ws://192.168.0.184:8000/ws/audio\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 비동기 작업
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    // 서버 통신
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 네트워크 통신 로깅, 최적화
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    // 백그라운드 작업 스케줄링
    implementation("androidx.work:work-runtime-ktx:2.7.0")
}