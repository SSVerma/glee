package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

actual fun getPlatformType(): PlatformType = PlatformType.Js

actual fun PlatformFile.toCoilPath(): String? = null

actual fun PlatformFile.getAbsolutePath(): String? = null

actual fun PlatformFile.toJsFile(): Any? = null

actual fun getOsType(): OsType {
    val platform = kotlinx.browser.window.navigator.platform.lowercase()
    val userAgent = kotlinx.browser.window.navigator.userAgent.lowercase()
    return when {
        platform.contains("win") || userAgent.contains("win") -> OsType.Windows
        platform.contains("mac") || userAgent.contains("mac") -> OsType.Mac
        platform.contains("linux") || userAgent.contains("linux") -> OsType.Linux
        userAgent.contains("android") -> OsType.Android
        userAgent.contains("iphone") || userAgent.contains("ipad") || userAgent.contains("ipod") -> OsType.Ios
        else -> OsType.Unknown
    }
}
