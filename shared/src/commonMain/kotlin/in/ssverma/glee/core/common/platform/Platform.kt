package `in`.ssverma.glee.core.common.platform

enum class PlatformType {
    Android,
    Ios,
    Jvm,
    Js,
    WasmJs
}

expect fun getPlatformType(): PlatformType
