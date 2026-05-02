package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

enum class PlatformType {
    Android, Ios, Jvm, WasmJs, Js
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

/**
 * Returns the browser File object if on Wasm/JS target.
 */
expect fun PlatformFile.toJsFile(): Any?

enum class OsType {
    Android,
    Ios,
    Windows,
    Mac,
    Linux,
    Unknown
}

expect fun getOsType(): OsType
