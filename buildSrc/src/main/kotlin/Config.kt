import java.io.File

data class AppBuildConfig(
    val buildType: BuildType,
    val meta: AppMetadata,

    // Platform Specifics
    val desktop: DesktopConfig,
    val android: AndroidConfig,
    val ios: IosConfig,
    val web: WebConfig
)

data class AppMetadata(
    val appName: String,
    val baseName: String,
    val version: String,     // "1.0.0"
    val buildNumber: Int     // 100
)

sealed class BuildType(val key: String) {
    object Debug : BuildType("debug")
    object Release : BuildType("release")

    companion object {
        fun fromKey(key: String?): BuildType = when (key?.lowercase()) {
            "release" -> Release
            else -> Debug
        }
    }
}

sealed interface PlatformConfig

// 1. Desktop Configs (REQUIRES File for native distributions)
data class DesktopConfig(
    val mainClass: String = "MainKt",
    val description: String,
    val copyright: String,
    val vendor: String,
    val isObfuscationEnabled: Boolean,
    val mac: DesktopMac,
    val win: DesktopWindows,
    val linux: DesktopLinux
) : PlatformConfig

data class DesktopMac(
    val bundleId: String,
    val dockName: String,
    val category: String = "public.app-category.business",
    val minimumSystemVersion: String = "12.0",
    val iconFile: File // .icns (Build-time asset)
)

data class DesktopWindows(
    val menuGroup: String,
    val upgradeUuid: String,
    val console: Boolean,
    val iconFile: File // .ico (Build-time asset)
)

data class DesktopLinux(
    val packageName: String,
    val maintainerEmail: String,
    val iconFile: File // .png (Build-time asset)
)

sealed interface MobileConfig : PlatformConfig {
    val applicationId: String
}

data class AndroidConfig(
    override val applicationId: String,
    val namespace: String,
    val compileSdk: Int = 34,
    val minSdk: Int = 24,
    val targetSdk: Int = 34,
    val versionCode: Int,
    val versionName: String,
) : MobileConfig

data class IosConfig(
    override val applicationId: String, // Bundle ID
    val deploymentTarget: String = "14.1",
    val teamId: String? = null,
    val iconAssetsPath: String?
) : MobileConfig

data class WebConfig(
    val pageTitle: String
) : PlatformConfig

object ProjectConfig {
    private const val BASE_APP_NAME = "Glee"
    private const val BASE_PACKAGE_NAME = "in.ssverma.glee"

    fun get(buildType: BuildType, projectDir: File): AppBuildConfig {
        var version = "1.0.0"
        var buildNumber = 1

        val versionFile = File(projectDir, "version.properties")
        if (versionFile.exists()) {
            val props = java.util.Properties()
            versionFile.inputStream().use { props.load(it) }
            version = props.getProperty("versionName", version)
            buildNumber = props.getProperty("versionCode", buildNumber.toString()).toInt()
        }

        val suffix = if (buildType is BuildType.Debug) ".debug" else ""
        val appNameSuffix = if (buildType is BuildType.Debug) "-Debug" else ""

        val appName = "$BASE_APP_NAME$appNameSuffix"
        val applicationId = "$BASE_PACKAGE_NAME$suffix"

        val compileSdk = findVersionFromToml(projectDir, "android-compileSdk", 36)
        val minSdk = findVersionFromToml(projectDir, "android-minSdk", 24)
        val targetSdk = findVersionFromToml(projectDir, "android-targetSdk", 36)

        return AppBuildConfig(
            buildType = buildType,
            meta = AppMetadata(
                appName = appName,
                baseName = BASE_APP_NAME.lowercase(),
                version = version,
                buildNumber = buildNumber
            ),
            desktop = DesktopConfig(
                mainClass = "$BASE_PACKAGE_NAME.MainKt",
                description = "Glee - An open source AI assistant",
                copyright = "© 2026 ssverma",
                vendor = "in.ssverma",
                isObfuscationEnabled = buildType is BuildType.Release,
                mac = DesktopMac(
                    bundleId = applicationId,
                    dockName = appName,
                    iconFile = File(projectDir, "shared/src/jvmMain/resources/icons/icon.icns")
                ),
                win = DesktopWindows(
                    menuGroup = appName,
                    upgradeUuid = "8e9c4d21-4f3b-4c5a-9d8e-7f6a5b4c3d2e",
                    console = false,
                    iconFile = File(projectDir, "shared/src/jvmMain/resources/icons/icon.ico")
                ),
                linux = DesktopLinux(
                    packageName = BASE_APP_NAME.lowercase() + suffix,
                    maintainerEmail = "support@ssverma.in",
                    iconFile = File(projectDir, "shared/src/jvmMain/resources/icons/icon.png")
                )
            ),
            android = AndroidConfig(
                applicationId = applicationId,
                namespace = BASE_PACKAGE_NAME,
                compileSdk = compileSdk,
                minSdk = minSdk,
                targetSdk = targetSdk,
                versionCode = buildNumber,
                versionName = version
            ),
            ios = IosConfig(
                applicationId = applicationId,
                iconAssetsPath = "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
            ),
            web = WebConfig(
                pageTitle = appName
            )
        )
    }

    private fun findVersionFromToml(projectDir: File, key: String, default: Int): Int {
        val tomlFile = File(projectDir, "gradle/libs.versions.toml")
        if (!tomlFile.exists()) return default

        val tomlContent = tomlFile.readText()
        val regex = Regex("""$key\s*=\s*"([^"]+)"""")
        return regex.find(tomlContent)?.groupValues?.get(1)?.toIntOrNull() ?: default
    }
}
