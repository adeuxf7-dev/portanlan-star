package dev.portolan.launcher.core;

import java.io.IOException;
import java.nio.file.*;

public final class MeteorManager {

    private static final String ARCHIVE_BASE =
            "https://github.com/ManInMyVan/meteor-archive/raw/main/files/meteor-client/";

    private static final String OFFICIAL_DL =
            "https://meteorclient.com/api/download?version=";

    public void ensureMeteor(String mcVersion, Path instanceDir,
                             java.util.function.Consumer<String> log)
            throws IOException, InterruptedException {
        Path mods = instanceDir.resolve("mods");
        Files.createDirectories(mods);

        // skip si déjà présent
        try (var stream = Files.list(mods)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (p.getFileName().toString().toLowerCase().startsWith("meteor-client-")) {
                    log.accept("  Meteor : déjà là");
                    return;
                }
            }
        }

        // 1) archive ManInMyVan pour les versions <= 1.21.3
        String archiveFile = archiveFilename(mcVersion);
        if (archiveFile != null) {
            Path dest = mods.resolve(archiveFile);
            log.accept("  Meteor : " + archiveFile + " (archive) pour " + mcVersion);
            Http.download(ARCHIVE_BASE + archiveFile, dest, null);
            return;
        }

        // 2) API officielle pour les versions >= 1.21.4 (dont 26.3)
        String filename = "meteor-client-" + mcVersion + ".jar";
        Path dest = mods.resolve(filename);
        log.accept("  Meteor : " + filename + " (API officielle) pour " + mcVersion);
        try {
            Http.download(OFFICIAL_DL + mcVersion, dest, null);
        } catch (Exception ex) {
            log.accept("  Meteor : échec API officielle — " + ex.getMessage());
            log.accept("  → télécharge manuellement depuis https://meteorclient.com/");
        }
    }

    private String archiveFilename(String mc) {
        return switch (mc) {
            case "1.20.1", "1.20" -> "meteor-client-0.5.4.jar";
            case "1.20.2" -> "meteor-client-0.5.5.jar";
            case "1.20.4" -> "meteor-client-0.5.6.jar";
            case "1.20.6" -> "meteor-client-0.5.7.jar";
            case "1.21.1" -> "meteor-client-0.5.8.jar";
            case "1.21.2", "1.21.3" -> "meteor-client-0.5.9.jar";
            default -> null;
        };
    }
}