package `in`.ssverma.glee.utils

class WasmSystemMetrics : SystemMetrics {
    override fun getUsedRamGb(): Float = 0f
    override fun getTotalRamGb(): Float = 0f
}

actual fun getSystemMetrics(): SystemMetrics = WasmSystemMetrics()
