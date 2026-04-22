package `in`.ssverma.glee.core.common

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()
