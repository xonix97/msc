plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
val embeddedClash = providers.gradleProperty("msc.embedClash").getOrElse("false").toBoolean()
android {
    namespace = "org.msc.app"
    compileSdk = 36
    defaultConfig {
        applicationId = providers.gradleProperty("msc.applicationId").getOrElse("org.msc.app")
        minSdk = 26
        targetSdk = 36
        versionCode = if (embeddedClash) 3 else 2
        versionName = if (embeddedClash) "0.3.0-embedded-alpha" else "0.2.0-local"
        buildConfigField("boolean", "EMBEDDED_CLASH", embeddedClash.toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { jniLibs.useLegacyPackaging = embeddedClash }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    if (embeddedClash) implementation(project(":embedded-clash"))
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.core:core-ktx:1.17.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
