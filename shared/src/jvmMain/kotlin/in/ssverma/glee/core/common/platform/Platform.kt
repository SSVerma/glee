package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

actual fun getPlatformType(): PlatformType = PlatformType.Jvm

actual fun PlatformFile.toCoilPath(): String? = path

actual fun PlatformFile.getAbsolutePath(): String? = path

actual fun PlatformFile.toJsFile(): Any? = null

actual fun getOsType(): OsType {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> OsType.Windows
        os.contains("mac") -> OsType.Mac
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> OsType.Linux
        else -> OsType.Unknown
    }
}
