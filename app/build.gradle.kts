plugins {
    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    runtimeOnly(project(":LevelLogin"))
    runtimeOnly("space.vectrix.ignite:ignite-launcher:1.2.2-SNAPSHOT")
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.imeaces.igniteload.Entrypoint"

    applicationName = rootProject.name

    applicationDefaultJvmArgs = listOf(
        "-Dignite.locator=dynamic",
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
    )
}
