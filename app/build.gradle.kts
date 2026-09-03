plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":LevelLogin"))
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.imeaces.fabricload.Entrypoint"
}

dependencies {
    implementation("net.fabricmc:fabric-loader:0.19.5")
    implementation("io.github.llamalad7:mixinextras-fabric:0.5.5")
    implementation("net.fabricmc:class-tweaker:0.3.0")
    implementation("net.fabricmc:mapping-io:0.9.1")
    implementation("net.fabricmc:sponge-mixin:0.17.4+mixin.0.8.7")
    implementation("net.fabricmc:tiny-remapper:0.14.1")
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-analysis:9.10.1")
    implementation("org.ow2.asm:asm-commons:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    implementation("org.ow2.asm:asm-util:9.10.1")
    implementation("org.ow2.sat4j:org.ow2.sat4j.core:2.3.6")
    implementation("org.ow2.sat4j:org.ow2.sat4j.pb:2.3.6")
}
