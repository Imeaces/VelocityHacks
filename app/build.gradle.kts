plugins {
    // Apply the Application plugin to add support for building an executable JVM application.
    application
    id("com.ryandens.javaagent-application") version "0.12.2"
}

dependencies {
    runtimeOnly(project(":LevelLogin"))
    javaagent("org.imeaces:KeitaLoader-launcher:0.1.0") {
        attributes {
            attribute(
                Bundling.BUNDLING_ATTRIBUTE,
                objects.named<Bundling>(Bundling.SHADOWED)
            )
        }
    }
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.imeaces.keitaload.KeitaLaunchJarWithMain"

    applicationName = rootProject.name

    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
    )
}
