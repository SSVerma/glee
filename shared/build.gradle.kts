import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
}

// 1. Config Generation
apply(from = "../gradle/config-generator.gradle.kts")

// 2. Web Deployment
apply(from = "../gradle/web-deploy.gradle.kts")

// 3. Android Release
apply(from = "../gradle/android-release.gradle.kts")

val releaseProperties = Properties().apply {
    val file = rootProject.file("release.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

// Centralized Configuration
val buildType =
    if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }) {
        BuildType.Release
    } else {
        BuildType.Debug
    }

val config = ProjectConfig.get(buildType, rootProject.projectDir)

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.litertlm.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)

            implementation(libs.androidx.sqlite.bundled)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.litertlm.jvm)
            implementation(libs.ktor.client.cio)

            implementation(libs.androidx.sqlite.bundled)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(npm("@mediapipe/tasks-genai", "0.10.20"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(npm("@mediapipe/tasks-genai", "0.10.20"))
            }
        }
        commonMain {
            // Register generated directory
            kotlin.srcDir(layout.buildDirectory.dir("generated/glee/kotlin"))

            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.materialIconsExtended)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                // Adaptive
                implementation(libs.compose.adaptive)
                implementation(libs.compose.adaptive.layout)
                implementation(libs.compose.adaptive.navigation)
                implementation(libs.compose.windowSizeClass)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinxJson)

                // Coil
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                // Navigation 3
                implementation(libs.navigation3.ui)

                // Okio
                implementation(libs.okio)

                // FileKit
                implementation(libs.filekit.compose)
                implementation(libs.filekit.core)

                // Markdown
                implementation(project(":core-markdown"))

                // Settings
                implementation(libs.multiplatform.settings.no.arg)
                implementation(libs.multiplatform.settings.coroutines)

                // Room
                implementation(libs.androidx.room3.runtime)

                // Serialization
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
    add("kspJs", libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
}

android {
    namespace = config.android.namespace
    compileSdk = config.android.compileSdk

    defaultConfig {
        applicationId = config.android.applicationId
        minSdk = config.android.minSdk
        targetSdk = config.android.targetSdk
        versionCode = config.android.versionCode
        versionName = config.android.versionName

        manifestPlaceholders["appName"] = config.meta.appName
    }

    signingConfigs {
        create("release") {
            storeFile = releaseProperties.getProperty("storeFile")?.let { file(it) }
            storePassword = releaseProperties.getProperty("storePassword")
            keyAlias = releaseProperties.getProperty("keyAlias")
            keyPassword = releaseProperties.getProperty("keyPassword")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (signingConfigs.getByName("release").storeFile?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = config.desktop.mainClass

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = config.meta.baseName
            packageVersion = config.meta.version

            description = config.desktop.description
            copyright = config.desktop.copyright
            vendor = config.desktop.vendor

            macOS {
                bundleID = config.desktop.mac.bundleId
                dockName = config.desktop.mac.dockName
                if (config.desktop.mac.iconFile.exists()) {
                    iconFile.set(config.desktop.mac.iconFile)
                }
            }
            windows {
                menuGroup = config.desktop.win.menuGroup
                upgradeUuid = config.desktop.win.upgradeUuid
                if (config.desktop.win.iconFile.exists()) {
                    iconFile.set(config.desktop.win.iconFile)
                }
            }
            linux {
                packageName = config.desktop.linux.packageName
                // maintainer = config.desktop.linux.maintainerEmail
                if (config.desktop.linux.iconFile.exists()) {
                    iconFile.set(config.desktop.linux.iconFile)
                }
            }
        }
    }
}

// Ensure the Mac Dock icon is set when running via jvmRun task
tasks.withType<JavaExec>().configureEach {
    if (name == "jvmRun" && System.getProperty("os.name").contains("Mac")) {
        val icon = file("src/jvmMain/resources/icons/icon.png")
        if (icon.exists()) {
            jvmArgs("-Xdock:icon=${icon.absolutePath}")
        }
    }
}
