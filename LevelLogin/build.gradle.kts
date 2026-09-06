plugins {
	id("buildsrc.convention.multable-jvm")
}

dependencies {
	compileOnly("org.imeaces:KeitaLoader-launcher:0.1.0")

	compileOnly("com.velocitypowered:velocity-proxy:4.1.2-SNAPSHOT")
	compileOnly("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")
	annotationProcessor("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")

	compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
	compileOnly("org.geysermc.floodgate:core:2.2.5-SNAPSHOT")
	compileOnly("org.geysermc.floodgate:velocity:2.2.5-SNAPSHOT")
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("description", project.description)

	val props = mapOf(
		"version" to version,
		"description" to project.description
	)

	filesMatching("ignite.mod.json") {
		expand(props)
	}

	filesMatching("velocity-plugin.json") {
		expand(props)
	}
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}
