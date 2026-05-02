package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

actual fun getPlatformType(): PlatformType = PlatformType.Ios

actual fun PlatformFile.toCoilPath(): String? = path

actual fun PlatformFile.getAbsolutePath(): String? = path

actual fun PlatformFile.toJsFile(): Any? = null

actual fun getOsType(): OsType = OsType.Ios
