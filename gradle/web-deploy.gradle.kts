tasks.register<Exec>("deployWeb") {
    group = "deployment"
    dependsOn("wasmJsBrowserDistribution")
    
    commandLine("firebase", "deploy", "--only", "hosting", "--project", "glee-ai")
}
