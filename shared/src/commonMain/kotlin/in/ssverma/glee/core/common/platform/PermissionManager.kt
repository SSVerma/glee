package `in`.ssverma.glee.core.common.platform

interface PermissionManager {
    fun isPermissionGranted(type: PermissionType): Boolean
    fun shouldShowRationale(type: PermissionType): Boolean = false
}

class NoOpPermissionManager : PermissionManager {
    override fun isPermissionGranted(type: PermissionType): Boolean = true
    override fun shouldShowRationale(type: PermissionType): Boolean = false
}
