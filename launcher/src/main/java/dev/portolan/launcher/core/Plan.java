package dev.portolan.launcher.core;

import java.nio.file.Path;
import java.util.List;

public record Plan(
        String id,
        String type,
        String mainClass,
        List<String> jvmArgs,
        List<String> gameArgs,
        List<Path> classpath,
        Path nativesDir,
        String assetIndexId) {}
