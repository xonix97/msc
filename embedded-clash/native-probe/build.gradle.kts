plugins { id("com.android.application") version "9.5.0" }
android {
    namespace = "org.msc.nativeprobe"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.msc.nativeprobe"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    sourceSets.getByName("main").jniLibs.srcDir("../src/main/jniLibs")
    packaging { jniLibs.useLegacyPackaging = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
