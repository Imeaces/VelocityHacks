package org.imeaces.igniteload;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import space.vectrix.ignite.Blackboard;
import space.vectrix.ignite.IgniteBootstrap;
import space.vectrix.ignite.game.GameLocatorService;
import space.vectrix.ignite.game.GameProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class DynamicGameLocator implements GameLocatorService {
  static final DynamicGameProvider PROVIDER = new DynamicGameProvider();

  static String id = "dynamic";
  static String name = "Dynamic game";

  @Override
  public @NotNull String id() {
    return id;
  }

  @Override
  public @NotNull String name() {
    return name;
  }

  @Override
  public int priority() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean shouldApply() {
    return true;
  }

  @Override
  public void apply(final @NotNull IgniteBootstrap bootstrap) throws Throwable {
    Blackboard.compute(Blackboard.GAME_TARGET,
            () -> Blackboard.get(Blackboard.GAME_TARGET).orElse(Entrypoint.entrypoint));

    // Locate the game jar.
    if(Blackboard.get(Blackboard.GAME_JAR).isEmpty()) {
      Blackboard.put(Blackboard.GAME_JAR, PROVIDER.gamePath());
    }
  }

  @Override
  public @NotNull GameProvider locate() {
    return PROVIDER;
  }

  public static void addGameLibs(String... libNames){
    for (var libName : libNames){
      var libPath = Path.of(libName);
      if (!addGameLib(libPath)) {
        Logger.warn("gameLibraries contain invalid jar library: " + libPath);
      }
    }
  }

  public static boolean addGameLib(Path libPath) {
    if (isJarFile(libPath)) {
      DynamicGameLocator.PROVIDER.addLibrary(libPath);
      Logger.info("found game lib: " + libPath);
      return true;
    } else {
      return false;
    }
  }

  public static void loadSysPropGameLibs() {
    var sysProp_gameLibraries = System.getProperty("gameLibraries");
    var sysProp_gameLibraryDirs = System.getProperty("gameLibraryDirs");

    if (sysProp_gameLibraries != null) {
      addGameLibs(sysProp_gameLibraries.split(":"));
    }

    if (sysProp_gameLibraryDirs != null) {
      for (var libDirName : sysProp_gameLibraryDirs.split(":")) {
        var libDir = Path.of(libDirName);
        if (!libDir.toFile().isDirectory()) {
          Logger.warn("gameLibraries contain invalid directory: " + libDirName);
          continue;
        }

        try (final Stream<Path> libDirWalker = Files.list(libDir)) {
          for (var it = libDirWalker.iterator(); it.hasNext(); ) {
            var libPath = it.next();
            if (libPath.endsWith(".jar")) {
              addGameLib(libPath);
            }
          }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
      }
    }
  }

  static boolean isJarFile(Path libPath){
    try (var _ = new JarFile(libPath.toFile())){
      return true;
    } catch (Throwable ex){
      return false;
    }
  }
}
