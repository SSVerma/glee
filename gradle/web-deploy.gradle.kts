tasks.register("deployWeb") {
    group = "deployment"
    dependsOn("wasmJsBrowserDistribution")
    
    doLast {
        exec {
            commandLine("firebase", "deploy", "--only", "hosting", "--project", "glee")
        }
    }
}
