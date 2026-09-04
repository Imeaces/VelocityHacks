package org.imeaces.igniteload;

import org.jetbrains.annotations.NotNull;
import space.vectrix.ignite.game.GameProvider;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class DynamicGameProvider implements GameProvider {
    private Path gamePath;
    private final Set<Path> libraries = new HashSet<>();

    @Override
    public @NotNull Stream<Path> gameLibraries() {
        return libraries.stream();
    }

    @Override
    public @NotNull Path gamePath() {
        return gamePath;
    }

    public void setGamePath(Path gamePath) {
        this.gamePath = gamePath;
    }

    public void addLibrary(Path lib){
        libraries.add(lib);
    }
}
