package org.imeaces.fabricload;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class VirtualGameProvider implements GameProvider {

    private final GameTransformer transformer = new GameTransformer() {
        @Override
        public byte[] transform(String className) {
            return null;
        }
    };

    private Arguments arguments = new Arguments();

    @Override
    public String getGameId() {
        return "app";
    }

    @Override
    public String getGameName() {
        return "App";
    }

    @Override
    public String getRawGameVersion() {
        return "unknown";
    }

    @Override
    public String getNormalizedGameVersion() {
        return "unknown";
    }

    @Override
    public Collection<GameProvider.BuiltinMod> getBuiltinMods() {
        return Collections.emptyList();
    }

    @Override
    public String getEntrypoint() {
        return Entrypoint.entrypoint;
    }

    @Override
    public Path getLaunchDirectory() {
        return Entrypoint.launcherDirectory;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public Set<GameProvider.BuiltinTransform> getBuiltinTransforms(String className) {
        return Collections.emptySet();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(
            FabricLauncher launcher,
            String[] args
    ) {
        arguments.parse(args);
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        MixinBootstrap.init();
        MixinExtrasBootstrap.init();
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return transformer;
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        for (Path p : Entrypoint.jarFiles) {
            launcher.addToClassPath(p);
        }
    }

    @Override
    public void launch(ClassLoader loader) {
        /*
         * @see org.imeaces.fabricload.bootstrap.Bootstrap
         */
        try {
            Class<?> callHelper = loader.loadClass(
                    getClass().getPackageName() + ".bootstrap.Bootstrap"
            );

            Method method = callHelper.getMethod(
                    "callStaticMain",
                    Path.class,
                    String.class,
                    String[].class
            );

            method.invoke(
                    null,
                    Entrypoint.launcherDirectory,
                    getEntrypoint(),
                    Entrypoint.programArgs
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return arguments.toArray();
    }
}
