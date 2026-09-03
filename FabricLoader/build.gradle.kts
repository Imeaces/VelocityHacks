plugins {
    id("buildsrc.convention.java-jvm")
}

dependencies {
    api("net.fabricmc:fabric-loader:0.19.5")
    api("io.github.llamalad7:mixinextras-fabric:0.5.5")
    api("net.fabricmc:class-tweaker:0.3.0")
    api("net.fabricmc:mapping-io:0.9.1")
    api("net.fabricmc:sponge-mixin:0.17.4+mixin.0.8.7")
    api("net.fabricmc:tiny-remapper:0.14.1")
    api("org.ow2.asm:asm:9.10.1")
    api("org.ow2.asm:asm-analysis:9.10.1")
    api("org.ow2.asm:asm-commons:9.10.1")
    api("org.ow2.asm:asm-tree:9.10.1")
    api("org.ow2.asm:asm-util:9.10.1")
    api("org.ow2.sat4j:org.ow2.sat4j.core:2.3.6")
    api("org.ow2.sat4j:org.ow2.sat4j.pb:2.3.6")
}
