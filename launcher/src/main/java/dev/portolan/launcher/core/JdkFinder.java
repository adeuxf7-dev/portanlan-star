package dev.portolan.launcher.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class JdkFinder {
    public static Path find(int major) {
        List<Path> roots = new ArrayList<>();
        String programFiles = System.getenv("ProgramFiles");
        String userHome = System.getProperty("user.home");
        String localAppData = System.getenv("LOCALAPPDATA");

        if (programFiles != null) {
            Path pf = Paths.get(programFiles);
            roots.add(pf.resolve("Eclipse Adoptium"));
            roots.add(pf.resolve("Microsoft"));
            roots.add(pf.resolve("Java"));
            roots.add(pf.resolve("Amazon Corretto"));
            roots.add(pf.resolve("Zulu"));
            roots.add(pf.resolve("BellSoft"));
        }
        if (localAppData != null) {
            roots.add(Paths.get(localAppData).resolve("Programs").resolve("Eclipse Adoptium"));
        }
        if (userHome != null) {
            roots.add(Paths.get(userHome, ".jdks"));
            roots.add(Paths.get(userHome, ".gradle", "jdks"));
            roots.add(Paths.get(userHome, ".sdkman", "candidates", "java"));
        }

        for (Path r : roots) {
            if (!Files.isDirectory(r)) continue;
            try (var stream = Files.list(r)) {
                for (Path c : (Iterable<Path>) stream::iterator) {
                    if (!Files.isDirectory(c)) continue;
                    String name = c.getFileName().toString().toLowerCase();
                    if (!name.contains(String.valueOf(major))) continue;
                    Path exe = c.resolve("bin").resolve(OS.NAME.equals("windows") ? "java.exe" : "java");
                    if (Files.isRegularFile(exe)) return exe;
                }
            } catch (IOException ignored) {}
        }
        return null;
    }
    private JdkFinder() {}
}