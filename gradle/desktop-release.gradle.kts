tasks.register("generateDesktopRelease") {
    group = "publishing"
    description = "Builds desktop release installers (DMG, MSI, or DEB) based on the current OS."

    val osName = System.getProperty("os.name").lowercase()
    val packageTaskPath = when {
        osName.contains("mac") -> ":shared:packageReleaseDmg"
        osName.contains("win") -> ":shared:packageReleaseMsi"
        else -> ":shared:packageReleaseDeb"
    }

    dependsOn(packageTaskPath)

    // Capture the output directory into a local variable to avoid serializing 'project' or 'file()' 
    // which breaks configuration cache.
    val sharedBuildDir = project(":shared").layout.buildDirectory

    doLast {
        val binarySubDir = when {
            osName.contains("mac") -> "dmg"
            osName.contains("win") -> "msi"
            else -> "deb"
        }
        val outputDir =
            sharedBuildDir.dir("compose/binaries/main-release/$binarySubDir").get().asFile
        val installer = outputDir.listFiles()?.firstOrNull()

        if (installer != null) {
            println("\nSUCCESS: Desktop installer generated at: ${installer.absolutePath}\n")
        } else {
            println("\nERROR: Desktop installer not found in $outputDir\n")
        }
    }
}
