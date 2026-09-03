package org.imeaces.fabricload

import net.fabricmc.loader.impl.game.GameProvider
import net.fabricmc.loader.impl.game.patch.GameTransformer
import net.fabricmc.loader.impl.launch.FabricLauncher
import net.fabricmc.loader.impl.util.Arguments
import java.nio.file.Path

class VirtualGameProvider : GameProvider {
    var transformer = object : GameTransformer() {
        override fun transform(className: String): ByteArray? {
            return null;
        }
    }
    private var arguments: Arguments = Arguments()

    override fun getGameId(): String {
        return "app"
    }

    override fun getGameName(): String {
        return "App"
    }

    override fun getRawGameVersion(): String {
        return "unknown"
    }

    override fun getNormalizedGameVersion(): String {
        return "unknown"
    }

    override fun getBuiltinMods(): Collection<GameProvider.BuiltinMod> {
        return listOf()
    }

    override fun getEntrypoint(): String {
        return Entrypoint.entrypoint
    }

    override fun getLaunchDirectory(): Path {
        return Entrypoint.launcherDirectory
    }

    override fun requiresUrlClassLoader(): Boolean {
        return false
    }

    override fun getBuiltinTransforms(className: String): Set<GameProvider.BuiltinTransform> {
        return setOf()
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun locateGame(
        launcher: FabricLauncher,
        args: Array<out String>
    ): Boolean {
        arguments.parse(args)
        return true
    }

    override fun initialize(launcher: FabricLauncher) {
    }

    override fun getEntrypointTransformer(): GameTransformer {
        return transformer
    }

    override fun unlockClassPath(launcher: FabricLauncher) {
        for (p in Entrypoint.jarFiles){
            launcher.addToClassPath(p)
        }
    }

    override fun launch(loader: ClassLoader) {
        /**
         * @see org.imeaces.fabricload.bootstrap.Bootstrap
         */
        val callHelper = loader.loadClass(this.javaClass.packageName + ".bootstrap.Bootstrap")
        val method = callHelper.getMethod(
            "callStaticMain",
            java.nio.file.Path::class.java,
            String::class.java,
            Array<String>::class.java
        )

        method.invoke(
            null,
            Entrypoint.launcherDirectory,
            entrypoint,
            Entrypoint.programArgs as Any
        )
    }

    override fun getArguments(): Arguments {
        return arguments
    }

    override fun getLaunchArguments(sanitize: Boolean): Array<out String> {
        return arguments.toArray()
    }
}