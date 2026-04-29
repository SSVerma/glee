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
}

val gleeProperties = Properties().apply {
    val file = rootProject.file("glee.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val hfTokenValue = gleeProperties.getProperty("HF_TOKEN") ?: ""

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
                implementation(npm("@mediapipe/tasks-genai", "0.10.14"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(npm("@mediapipe/tasks-genai", "0.10.14"))
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

// Fixed Task to generate config file with the HF token (CC compatible)
val generateGleeConfig = tasks.register("generateGleeConfig") {
    val hfToken = hfTokenValue
    val outputDir = layout.buildDirectory.dir("generated/glee/kotlin/in/ssverma/glee")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        dir.resolve("GleeConfig.kt").writeText("""
            package `in`.ssverma.glee
            
            /**
             * Generated configuration file. Do not edit manually.
             */
            object GleeConfig {
                const val HF_TOKEN = "$hfToken"
            }
        """.trimIndent())
    }
}

// Ensure all Kotlin compilation and KSP tasks depend on our generation task
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateGleeConfig)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// Declare explicit dependency for KSP tasks to avoid implicit dependency warning
tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateGleeConfig)
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
    namespace = "in.ssverma.glee"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "in.ssverma.glee"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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
        mainClass = "in.ssverma.glee"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "in.ssverma.glee"
            packageVersion = "1.0.0"
        }
    }
}
