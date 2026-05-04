plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun readFujiNetRuntimeVersion(): String {
    val versionHeader = rootProject.file("tools/fujinet/work/fujinet-firmware/include/version.h")
    if (!versionHeader.isFile) {
        return "fujinet-runtime-v1"
    }
    val match = Regex("""#define\s+FN_VERSION_FULL\s+"([^"]+)"""")
        .find(versionHeader.readText())
    return match?.groupValues?.get(1) ?: "fujinet-runtime-v1"
}

val fujiNetRuntimeVersion = readFujiNetRuntimeVersion()

fun readAtari800Version(): String {
    val sourceScript = rootProject.file("tools/atari800/build-atari800-source.sh")
    if (!sourceScript.isFile) {
        return "Unknown"
    }
    val scriptText = sourceScript.readText()
    val branch = Regex("""UPSTREAM_BRANCH="([^"]+)"""")
        .find(scriptText)
        ?.groupValues
        ?.get(1)
    val commit = Regex("""UPSTREAM_COMMIT="([^"]+)"""")
        .find(scriptText)
        ?.groupValues
        ?.get(1)
    val shortCommit = commit?.take(8)
    return when {
        !branch.isNullOrBlank() && !shortCommit.isNullOrBlank() -> "$branch ($shortCommit)"
        !shortCommit.isNullOrBlank() -> shortCommit
        !branch.isNullOrBlank() -> branch
        else -> "Unknown"
    }
}

val atari800Version = readAtari800Version()

val prepareAtari800Source by tasks.registering(Exec::class) {
    group = "build setup"
    description = "Fetches and stages the pinned Atari800 upstream source tree."
    workingDir = rootProject.projectDir
    commandLine("bash", rootProject.file("tools/atari800/build-atari800-source.sh").absolutePath)
    inputs.file(rootProject.file("tools/atari800/build-atari800-source.sh"))
    inputs.dir(rootProject.file("tools/atari800/patches"))
    outputs.dir(project.file("src/main/cpp-generated/atari800"))
}

val prepareFujiNetRuntime by tasks.registering(Exec::class) {
    group = "build setup"
    description = "Builds the pinned FujiNet Android runtime for all packaged ABIs."
    workingDir = rootProject.projectDir
    commandLine("bash", rootProject.file("tools/fujinet/build-fujinet.sh").absolutePath, "--all-abis")
    inputs.file(rootProject.file("tools/fujinet/build-fujinet.sh"))
    inputs.dir(rootProject.file("tools/fujinet/patches"))
    inputs.dir(rootProject.file("tools/fujinet/support"))
    outputs.dir(project.file("src/main/assets-generated/fujinet"))
    outputs.dir(project.file("src/main/jniLibs-generated"))
}

tasks.configureEach {
    if (name.contains("Release") || name == "preBuild") {
        dependsOn(prepareFujiNetRuntime)
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareAtari800Source)
}

tasks.matching { task ->
    task.name.startsWith("configureCMake") || task.name.startsWith("buildCMake")
}.configureEach {
    dependsOn(prepareAtari800Source)
}

tasks.matching { task ->
    task.name.startsWith("merge") && (
        task.name.endsWith("Assets")
            || task.name.endsWith("JniLibFolders")
            || task.name.endsWith("NativeLibs")
    )
}.configureEach {
    dependsOn(prepareFujiNetRuntime)
}

android {
    namespace = "com.mantismoonlabs.fujinetgo800"
    compileSdk = 36
    ndkVersion = "30.0.14904198"
    flavorDimensions += "branding"

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.1.0"
        buildConfigField("String", "ATARI800_VERSION", "\"${atari800Version}\"")
        buildConfigField("String", "FUJINET_RUNTIME_VERSION", "\"${fujiNetRuntimeVersion}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
            }
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        debug {
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    productFlavors {
        create("mantis") {
            dimension = "branding"
            applicationId = "com.mantismoonlabs.fujinetgo800"
            buildConfigField("String", "BRAND_MEDIA_SELECTION_COMMENT", "\"Fuji800a media selections\"")
            buildConfigField("String", "BRAND_SYSTEM_ROM_SELECTION_COMMENT", "\"Fuji800a system ROM selections\"")
            buildConfigField("String", "BRAND_EXTERNAL_MEDIA_DIR_NAME", "\"FujiNet Go 800\"")
        }
        create("oss") {
            dimension = "branding"
            applicationId = "org.fujinetwifi.fujinetgo800.oss"
            buildConfigField("String", "BRAND_MEDIA_SELECTION_COMMENT", "\"FujiNet Go 800 OSS media selections\"")
            buildConfigField("String", "BRAND_SYSTEM_ROM_SELECTION_COMMENT", "\"FujiNet Go 800 OSS system ROM selections\"")
            buildConfigField("String", "BRAND_EXTERNAL_MEDIA_DIR_NAME", "\"FujiNet Go 800 OSS\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    sourceSets {
        getByName("main") {
            assets.srcDir("src/main/assets-generated/fujinet")
            jniLibs.srcDir("src/main/jniLibs-generated")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
    testImplementation(libs.androidx.lifecycle.viewmodel.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
