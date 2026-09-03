package org.imeaces.fabricload;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class Entrypoint {
    static String entrypoint;
    static Path launcherDirectory = Path.of(System.getProperty("user.dir"));
    static Set<Path> jarFiles = new HashSet<>();
    static String[] programArgs;

    static void main(String[] args) throws IOException, URISyntaxException {
        var programJarFile = Path.of(args[0]);
        var programArgsLen = args.length - 1;

        programArgs = programArgsLen > 0
                ? Arrays.copyOfRange(args, 1, programArgsLen)
                : new String[0];

        try (var jar = new JarFile(programJarFile.toFile())){
            entrypoint = jar.getManifest()
                .getMainAttributes()
                .getValue("Main-Class");
        }

        jarFiles.add(programJarFile);
        jarFiles.add(Paths.get(Entrypoint.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

        net.fabricmc.loader.impl.launch.knot.KnotServer.main(programArgs);
    }
}
