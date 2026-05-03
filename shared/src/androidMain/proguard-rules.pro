# Ktor
# Don't keep everything; Ktor is mostly used via reflection or service loading for engines/plugins.
# Keep the plugin/engine classes which are often loaded by name.
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.serialization.** { *; }

# Ktor uses java.lang.management which is not available on Android
-dontwarn java.lang.management.**
-dontwarn javax.naming.**
-dontwarn io.ktor.util.debug.IntellijIdeaDebugDetector

# Okio
-keep class okio.** { *; }
-keep interface okio.** { *; }
# Okio might reference java.nio.file classes not available on older Android
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Kotlin Coroutines
# Ktor heavily relies on coroutines; ensure volatile fields used for atomics are kept.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# LiteRT / MediaPipe
-keep class com.google.mediapipe.** { *; }
-keep class com.google.ai.edge.** { *; }

# Serializable data classes
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class **$$serializer { *; }

# Keep domain models
-keep class in.ssverma.glee.features.chat.domain.model.** { *; }
