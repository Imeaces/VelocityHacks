package org.imeaces.fabricload.bootstrap;

import org.imeaces.fabricload.Entrypoint;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Bootstrap {
    private static final Logger logger = Logger.getLogger(Bootstrap.class.getName());

    public static void callStaticMain(
            Path launcherDirectory,
            String mainClassName,
            String[] args
    )
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {

        net.fabricmc.loader.impl.game.minecraft.Hooks.startServer(launcherDirectory.toFile(), null);

        logger.info("running main method under class " + mainClassName);

        var mainClass = Bootstrap.class.getClassLoader().loadClass(mainClassName);

        var method = mainClass.getMethod("main", String[].class);

        method.invoke(null, (Object)args);
    }
}
