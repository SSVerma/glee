package `in`.ssverma.glee.utils

import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean

class JvmSystemMetrics : SystemMetrics {
    override fun getUsedRamGb(): Float {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return (osBean.totalPhysicalMemorySize - osBean.freePhysicalMemorySize) / (1024f * 1024f * 1024f)
    }

    override fun getTotalRamGb(): Float {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return osBean.totalPhysicalMemorySize / (1024f * 1024f * 1024f)
    }
}

actual fun getSystemMetrics(): SystemMetrics = JvmSystemMetrics()
