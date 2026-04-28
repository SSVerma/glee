package `in`.ssverma.glee.core.common.platform

class JsSystemMetrics : SystemMetrics {
    override fun getUsedRamGb(): Float = 0f
    override fun getTotalRamGb(): Float = 0f
}

actual fun getSystemMetrics(): SystemMetrics = JsSystemMetrics()
