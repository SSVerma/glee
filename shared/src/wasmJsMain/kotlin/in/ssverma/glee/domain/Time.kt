package `in`.ssverma.glee.core.common

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
external fun jsNow(): Double

actual fun currentTimeMillis(): Long = jsNow().toLong()
