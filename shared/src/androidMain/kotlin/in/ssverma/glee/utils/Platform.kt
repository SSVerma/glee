package `in`.ssverma.glee.core.common.platform

import io.github.vinceglb.filekit.core.PlatformFile

actual fun getPlatformType(): PlatformType = PlatformType.Android

actual fun PlatformFile.toCoilPath(): String? = uri.toString()
