plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {

    namespace = "com.example.womensafteyapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.womensafteyapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.google.android.material:material:1.11.0")

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    // ✅ Firestore — for saving user profile & emergency contacts
    implementation("com.google.firebase:firebase-firestore:24.11.0")

    // Google Login
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Phone OTP Login
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}