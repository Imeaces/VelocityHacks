plugins {
	id("buildsrc.convention.multable-jvm")
}

dependencies {
	compileOnly(project(":LevelLoginMod"))
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("description", project.description)

	val props = mapOf(
		"version" to version,
		"description" to project.description
	)

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
