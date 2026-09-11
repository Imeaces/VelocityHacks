plugins {
	id("buildsrc.convention.multable-jvm")
	id("me.glicz.access-widen") version "3.0.0"
}

accessWiden {
	// load *.accesswidener files from resources
	accessWideners.from(fileTree(sourceSets.main.get().resources.srcDirs.first()) {
		include("*.accesswidener")
	})
}

dependencies {
	// mod loader etc.
	compileOnly("org.imeaces:KeitaLoader-launcher:0.1.0")
	compileOnly("org.projectlombok:lombok:1.18.42")
	annotationProcessor("org.projectlombok:lombok:1.18.42")

	// velocity plugins
	compileOnlyApi("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")
	annotationProcessor("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")
	compileOnlyApi("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")

	// for mixin usage
	compileOnlyApi(accessWiden("com.velocitypowered:velocity-proxy:4.1.2-SNAPSHOT"))
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
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}
