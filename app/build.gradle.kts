plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cl.easymaipu.formularios"
    compileSdk = 34

    defaultConfig {
        applicationId = "cl.easymaipu.formularios"
        minSdk = 24
        targetSdk = 34
        val build = (project.findProperty("build") as String?)?.toInt() ?: 1
        versionCode = build
        versionName = "1.$build"
    }

    signingConfigs {
        create("fija") {
            storeFile = file("firma.jks")
            storePassword = "f9bf512797e60f55"
            keyAlias = "formularios"
            keyPassword = "f9bf512797e60f55"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
}
