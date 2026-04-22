package `in`.ssverma.glee.core.common.platform

import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean

class JvmSystemMetrics : SystemMetrics {
    override fun getUsedRamGb(): Float {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return (osBean.totalMemorySize - osBean.freeMemorySize) / (1024f * 1024f * 1024f)
    }

    override fun getTotalRamGb(): Float {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return osBean.totalMemorySize / (1024f * 1024f * 1024f)
    }
}

actual fun getSystemMetrics(): SystemMetrics = JvmSystemMetrics()
