plugins {
    id("com.android.library")
    id("com.chaquo.python") version "17.2.0"
}
android {
    namespace = "org.msc.embeddedclash"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
chaquopy {
    defaultConfig {
        version = "3.11"
        buildPython(providers.environmentVariable("MSC_BUILD_PYTHON").getOrElse("python3.11"))
        extractPackages("clashflac")
        pip {
            install("-r", project.file("requirements-android.txt").absolutePath)
            options("--retries", "0")
        }
    }
}
tasks.named("preBuild") {
    doFirst {
        for (abi in listOf("arm64-v8a", "armeabi-v7a", "x86_64")) {
            check(project.file("src/main/jniLibs/$abi/libmsc_ffmpeg.so").isFile) {
                "Build the Android-native FFmpeg binaries first: scripts/build_clash_ffmpeg.sh (missing $abi)."
            }
        }
    }
}
