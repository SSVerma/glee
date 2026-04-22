package `in`.ssverma.glee.core.common.platform

import android.app.ActivityManager
import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AndroidSystemMetrics : SystemMetrics, KoinComponent {
    private val context: Context by inject()

    override fun getUsedRamGb(): Float {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalMem - memoryInfo.availMem) / (1024f * 1024f * 1024f)
    }

    override fun getTotalRamGb(): Float {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024f * 1024f * 1024f)
    }
}

actual fun getSystemMetrics(): SystemMetrics = AndroidSystemMetrics()
