@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package `in`.ssverma.glee.core.common.platform

@JsFun("() => window.performance.memory ? window.performance.memory.usedJSHeapSize / (1024 * 1024 * 1024) : 0")
private external fun getUsedHeapSizeGb(): Double

@JsFun("() => window.performance.memory ? window.performance.memory.jsHeapSizeLimit / (1024 * 1024 * 1024) : 0")
private external fun getHeapSizeLimitGb(): Double

class WasmSystemMetrics : SystemMetrics {
    override fun getUsedRamGb(): Float = getUsedHeapSizeGb().toFloat()
    override fun getTotalRamGb(): Float = getHeapSizeLimitGb().toFloat()
}

actual fun getSystemMetrics(): SystemMetrics = WasmSystemMetrics()
