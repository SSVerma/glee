package `in`.ssverma.glee.core.common.platform

class IosSystemMetrics : SystemMetrics {
    override fun getUsedRamGb(): Float = 4.2f // Placeholder
    override fun getTotalRamGb(): Float = 16f // Placeholder
}

actual fun getSystemMetrics(): SystemMetrics = IosSystemMetrics()
