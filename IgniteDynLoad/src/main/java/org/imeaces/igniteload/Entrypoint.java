package org.imeaces.igniteload;

import org.tinylog.Logger;
import space.vectrix.ignite.Blackboard;
import space.vectrix.ignite.IgniteBootstrap;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.JarFile;

public class Entrypoint {
    static String entrypoint;

    public static void main(String[] args) throws IOException {
        Path programJarFile = Paths.get(args[0]);
        int programArgsLen = args.length - 1;
        String[] programArgs = programArgsLen > 0
                ? Arrays.copyOfRange(args, 1, programArgsLen)
                : new String[0];

        try (JarFile jar = new JarFile(programJarFile.toFile())){
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
