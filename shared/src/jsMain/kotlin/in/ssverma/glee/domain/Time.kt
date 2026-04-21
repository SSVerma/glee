package `in`.ssverma.glee.domain

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()
