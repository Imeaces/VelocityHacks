plugins {
    // Apply the Application plugin to add support for building an executable JVM application.
    `java-library`
}

dependencies {
    compileOnly("space.vectrix.ignite:ignite-launcher:1.2.2-SNAPSHOT")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 8
    }
}

