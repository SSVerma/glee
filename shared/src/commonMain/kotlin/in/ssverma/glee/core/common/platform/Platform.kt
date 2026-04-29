package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

enum class PlatformType {
    Android,
    Ios,
    Jvm,
    Js,
    WasmJs
}

expect fun getPlatformType(): PlatformType

/**
 * Returns a path or URI that Coil can use to render the file.
 */
expect fun PlatformFile.toCoilPath(): String?

/**
 * Returns the absolute path to the file if available on the platform.
 */
expect fun PlatformFile.getAbsolutePath(): String?
