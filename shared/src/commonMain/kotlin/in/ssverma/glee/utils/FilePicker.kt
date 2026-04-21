package `in`.ssverma.glee.utils

import `in`.ssverma.glee.domain.model.AttachedFile
import io.github.vinceglb.filekit.core.PlatformFile

fun PlatformFile.toAttachedFile(): AttachedFile {
    return AttachedFile(
        name = name,
        path = path,
        size = 0L, // Placeholder
        platformFile = this
    )
}
