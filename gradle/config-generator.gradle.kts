import java.util.Properties

val gleeProperties = Properties().apply {
    val file = rootProject.file("glee.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val hfTokenValue = gleeProperties.getProperty("HF_TOKEN") ?: ""

// This is expected to be called from the shared module
val generateGleeConfig = tasks.register("generateGleeConfig") {
    val hfToken = hfTokenValue
    // Accessing ProjectConfig from buildSrc
    val isRelease = project.hasProperty("buildType") && project.property("buildType") == "release" ||
            gradle.startParameter.taskNames.any { task ->
                val lowerTask = task.lowercase()
                lowerTask.contains("release") || lowerTask.contains("publish") || lowerTask.contains("deploy")
            }
            
    val buildType = if (isRelease) BuildType.Release else BuildType.Debug
    val config = ProjectConfig.get(buildType, rootProject.projectDir)
    
    val outputDir = layout.buildDirectory.dir("generated/glee/kotlin/in/ssverma/glee")
    outputs.dir(outputDir)
    
    doLast {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        
        dir.resolve("AppBuildConfig.kt").writeText("""
            package `in`.ssverma.glee

            import `in`.ssverma.glee.AppBuildConfig.BuildType

            /**
             * Generated configuration file. Do not edit manually.
             */
            data class AppBuildConfig(
                val buildType: BuildType,
                val meta: AppMetadata,
                val desktop: DesktopConfig,
                val android: AndroidConfig,
                val ios: IosConfig,
                val web: WebConfig,
                val hfToken: String
            ) {
                data class AppMetadata(
                    val appName: String, 
                    val baseName: String, 
                    val version: String,
                    val buildNumber: Int
                )

                sealed class BuildType(val key: String) {
                    object Debug : BuildType("debug")
                    object Release : BuildType("release")
                }

                data class DesktopConfig(
                    val mainClass: String,
                    val description: String,
                    val copyright: String,
                    val vendor: String,
                    val isObfuscationEnabled: Boolean,
                    val mac: MacConfig,
                    val win: WinConfig,
                    val linux: LinuxConfig
                )

                data class MacConfig(
                    val bundleId: String,
                    val dockName: String
                )

                data class WinConfig(
                    val menuGroup: String,
                    val upgradeUuid: String
                )

                data class LinuxConfig(
                    val packageName: String,
                    val maintainerEmail: String
                )

                data class AndroidConfig(
                    val applicationId: String,
                    val namespace: String,
                    val compileSdk: Int,
                    val minSdk: Int,
                    val targetSdk: Int,
                    val versionCode: Int,
                    val versionName: String
                )

                data class IosConfig(
                    val applicationId: String,
                    val deploymentTarget: String,
                    val teamId: String?,
                    val iconAssetsPath: String?
                )

                data class WebConfig(
                    val pageTitle: String
                )
            }

            object GleeConfig {
                val config = AppBuildConfig(
                    buildType = ${if (config.buildType is BuildType.Release) "BuildType.Release" else "BuildType.Debug"},
                    meta = AppBuildConfig.AppMetadata(
                        appName = "${config.meta.appName}",
                        baseName = "${config.meta.baseName}",
                        version = "${config.meta.version}",
                        buildNumber = ${config.meta.buildNumber}
                    ),
                    desktop = AppBuildConfig.DesktopConfig(
                        mainClass = "${config.desktop.mainClass}",
                        description = "${config.desktop.description}",
                        copyright = "${config.desktop.copyright}",
                        vendor = "${config.desktop.vendor}",
                        isObfuscationEnabled = ${config.desktop.isObfuscationEnabled},
                        mac = AppBuildConfig.MacConfig(
                            bundleId = "${config.desktop.mac.bundleId}",
                            dockName = "${config.desktop.mac.dockName}"
                        ),
                        win = AppBuildConfig.WinConfig(
                            menuGroup = "${config.desktop.win.menuGroup}",
                            upgradeUuid = "${config.desktop.win.upgradeUuid}"
                        ),
                        linux = AppBuildConfig.LinuxConfig(
                            packageName = "${config.desktop.linux.packageName}",
                            maintainerEmail = "${config.desktop.linux.maintainerEmail}"
                        )
                    ),
                    android = AppBuildConfig.AndroidConfig(
                        applicationId = "${config.android.applicationId}",
                        namespace = "${config.android.namespace}",
                        compileSdk = ${config.android.compileSdk},
                        minSdk = ${config.android.minSdk},
                        targetSdk = ${config.android.targetSdk},
                        versionCode = ${config.android.versionCode},
                        versionName = "${config.android.versionName}"
                    ),
                    ios = AppBuildConfig.IosConfig(
                        applicationId = "${config.ios.applicationId}",
                        deploymentTarget = "${config.ios.deploymentTarget}",
                        teamId = ${if (config.ios.teamId != null) "\"${config.ios.teamId}\"" else "null"},
                        iconAssetsPath = ${if (config.ios.iconAssetsPath != null) "\"${config.ios.iconAssetsPath}\"" else "null"}
                    ),
                    web = AppBuildConfig.WebConfig(
                        pageTitle = "${config.web.pageTitle}"
                    ),
                    hfToken = "$hfToken"
                )
            }
        """.trimIndent())
    }
}

tasks.configureEach {
    if (name.startsWith("compileKotlin") || name.startsWith("ksp")) {
        dependsOn(generateGleeConfig)
    }
}
