package org.imeaces.igniteload;

import org.tinylog.Logger;
import space.vectrix.ignite.Blackboard;
import space.vectrix.ignite.IgniteBootstrap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

public class Entrypoint {
    static String entrypoint;

    static void main(String[] args) throws IOException, URISyntaxException {
        var programJarFile = Path.of(args[0]);
        var programArgsLen = args.length - 1;
        var programArgs = programArgsLen > 0
                ? Arrays.copyOfRange(args, 1, programArgsLen)
                : new String[0];

        try (var jar = new JarFile(programJarFile.toFile())){
            entrypoint = jar.getManifest()
                .getMainAttributes()
                .getValue("Main-Class");
        }

        DynamicGameLocator.id = entrypoint;
        DynamicGameLocator.name = programJarFile.toFile().getName();

        DynamicGameLocator.PROVIDER.setGamePath(
                programJarFile
        );

        DynamicGameLocator.loadSysPropGameLibs();

        System.setProperty(Blackboard.GAME_LOCATOR.name(), DynamicGameLocator.id);

        Logger.info("Running IgniteBootstrap.main()");

        IgniteBootstrap.main(programArgs);
    }
}
