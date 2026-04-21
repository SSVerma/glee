package `in`.ssverma.glee.utils

interface SystemMetrics {
    fun getUsedRamGb(): Float
    fun getTotalRamGb(): Float
}

expect fun getSystemMetrics(): SystemMetrics
