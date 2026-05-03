# Keep all Navigation 3 and Navigation Event classes to avoid VerifyError
# These are newer libraries and need to be kept intact for correct type verification.
-keep class androidx.navigation3.** { *; }
-keep interface androidx.navigation3.** { *; }
-keep class androidx.navigationevent.** { *; }
-keep interface androidx.navigationevent.** { *; }

# SavedState and Compose Navigation
-keep class androidx.savedstate.** { *; }
-keep interface androidx.savedstate.** { *; }

# General Compose Desktop
# Instead of keeping everything, we keep the internal reflection points and UI components.
-keep class androidx.compose.ui.window.** { *; }
-keep class androidx.compose.runtime.CompositionLocal { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, *Annotation*

# Keep AppBuildConfig and GleeConfig
-keep class in.ssverma.glee.AppBuildConfig { *; }
-keep class in.ssverma.glee.GleeConfig { *; }

# LiteRT / MediaPipe (Critical for AI inference)
-keep class com.google.mediapipe.** { *; }
-keep class com.google.ai.edge.** { *; }
-keep class com.google.ai.edge.litertlm.** { *; }

# Ktor
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.serialization.** { *; }
-dontwarn io.ktor.util.debug.IntellijIdeaDebugDetector

# Okio
-keep class okio.** { *; }
-keep interface okio.** { *; }

# Kotlin Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class **$$serializer { *; }
-keep class kotlinx.serialization.json.** { *; }

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
# Some coroutine internals are used via reflection
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
