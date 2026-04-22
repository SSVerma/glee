package `in`.ssverma.glee.core.common.platform

interface SystemMetrics {
    fun getUsedRamGb(): Float
    fun getTotalRamGb(): Float
}

expect fun getSystemMetrics(): SystemMetrics
