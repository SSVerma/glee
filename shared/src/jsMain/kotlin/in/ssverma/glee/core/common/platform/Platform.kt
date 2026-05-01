package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

actual fun getPlatformType(): PlatformType = PlatformType.Js

actual fun PlatformFile.toCoilPath(): String? = null

actual fun PlatformFile.getAbsolutePath(): String? = null

actual fun PlatformFile.toJsFile(): Any? = null
