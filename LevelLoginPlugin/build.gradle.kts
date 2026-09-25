import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	id("buildsrc.convention.multable-jvm")
	id("com.gradleup.shadow") version "9.2.2"
}

dependencies {
	compileOnly(project(":LevelLoginMod"))

	implementation("org.xerial:sqlite-jdbc:3.53.4.0")
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

tasks.getByName<ShadowJar>("shadowJar") {
	mergeServiceFiles()
}

tasks.named("assemble") {
	dependsOn("shadowJar")
}
