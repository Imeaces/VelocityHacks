plugins {
	id("buildsrc.convention.kotlin-jvm")
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
