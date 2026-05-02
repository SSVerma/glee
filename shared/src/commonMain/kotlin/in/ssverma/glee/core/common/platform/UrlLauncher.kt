package `in`.ssverma.glee.core.common.platform

interface UrlLauncher {
    fun launchUrl(url: String): Boolean
    fun openAppSettings() {}
}
