plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "app.saltabeat"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.saltabeat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += "arm64-v8a" }
    }
    signingConfigs {
        create("release") {
            val path = System.getenv("KEYSTORE_PATH")
            if (path != null) {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = true }
    testOptions { unitTests.isIncludeAndroidResources = true }

}
dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.datastore)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.android.test.core)
    testImplementation(libs.compose.test)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.compose.test.manifest)
}

tasks.withType<Test>().configureEach {
    val testHome = rootProject.layout.buildDirectory.dir("test-home").get().asFile
    testHome.mkdirs()
    val fontCache=rootProject.layout.buildDirectory.dir("font-cache").get().asFile
    fontCache.mkdirs()
    environment("XDG_CACHE_HOME",fontCache.absolutePath)
    systemProperty("user.home", testHome.absolutePath)
    listOf("https.proxyHost", "https.proxyPort", "http.proxyHost", "http.proxyPort", "javax.net.ssl.trustStore").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    systemProperty("robolectric.graphicsMode", "NATIVE")
    systemProperty("roborazzi.test.record", "true")
    systemProperty("robolectric.pixelCopyRenderMode", "hardware")
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
}
