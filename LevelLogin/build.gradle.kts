plugins {
	id("buildsrc.convention.kotlin-jvm")
}

dependencies {
	implementation(project(":FabricLoader"))

	compileOnly("com.velocitypowered:velocity-proxy:4.1.2-SNAPSHOT")
	compileOnly("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}
