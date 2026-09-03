plugins {
    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":LevelLogin"))
    implementation(project(":FabricLoader"))
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.imeaces.fabricload.Entrypoint"

    applicationName = rootProject.name

    applicationDefaultJvmArgs = listOf(
        "-Dfabric.development=true",
        "--enable-native-access=ALL-UNNAMED"
    )
}
