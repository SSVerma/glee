abstract class IncrementVersionTask : DefaultTask() {
    @get:InputFile
    abstract val versionFile: RegularFileProperty

    @TaskAction
    fun increment() {
        val file = versionFile.get().asFile
        val props = java.util.Properties()
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }

        val currentCode = props.getProperty("versionCode", "0").toInt()
        val currentName = props.getProperty("versionName", "1.0.0")

        val nextCode = currentCode + 1
        val parts = currentName.split(".").map { it.toIntOrNull() ?: 0 }.toMutableList()
        if (parts.size >= 3) {
            parts[2] = parts[2] + 1
        } else {
            while (parts.size < 3) parts.add(0)
            parts[2] = parts[2] + 1
        }
        val nextName = parts.joinToString(".")

        props.setProperty("versionCode", nextCode.toString())
        props.setProperty("versionName", nextName)

        file.outputStream().use { props.store(it, "Auto-incremented by generateAndroidRelease task") }
        
        println("Incremented version to $nextName ($nextCode)")
    }
}

val generateAndroidRelease = tasks.register<IncrementVersionTask>("generateAndroidRelease") {
    group = "publishing"
    description = "Increments version code, updates version name, and builds Android release artifacts (APK and AAB)."
    versionFile.set(layout.projectDirectory.file("../version.properties"))

    finalizedBy(":shared:bundleRelease", ":shared:assembleRelease")

    doLast {
        val apkDir = file("${project(":shared").layout.buildDirectory.get()}/outputs/apk/release")
        val signedApk = apkDir.listFiles()?.find { it.name.endsWith(".apk") && !it.name.contains("unsigned") }

        if (signedApk == null) {
            println("\n" + "!".repeat(50))
            println("WARNING: No signed APK found in $apkDir")
            println("Your release.properties might be missing or incorrect.")
            println("The build is 'unsigned' and cannot be installed on devices.")
            println("!".repeat(50) + "\n")
        } else {
            println("\nSUCCESS: Signed APK generated at: ${signedApk.absolutePath}\n")
        }
    }
}
