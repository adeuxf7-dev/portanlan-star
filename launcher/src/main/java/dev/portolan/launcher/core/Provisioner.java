package dev.portolan.launcher.core;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Provisionne localement un profil Fabric pour chaque version de jeu stable
 * que Fabric reconnaît. Idempotent : si un profil existe déjà pour une version,
 * on le saute sans requête réseau.
 */
public final class Provisioner {

    public static void provisionAll(Path versionsDir, java.util.function.Consumer<String> log)
            throws Exception {
        Files.createDirectories(versionsDir);

        log.accept("liste des versions Fabric…");
        JsonArray games = JsonParser.parseString(
                Http.getString("https://meta.fabricmc.net/v2/versions/game")).getAsJsonArray();
        List<String> stable = new ArrayList<>();
        for (JsonElement el : games) {
            JsonObject g = el.getAsJsonObject();
            if (g.get("stable").getAsBoolean()) stable.add(g.get("version").getAsString());
        }
        log.accept(stable.size() + " versions stables connues de Fabric");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<String>> futures = new ArrayList<>();
        for (String mc : stable) {
            futures.add(pool.submit(() -> provisionOne(versionsDir, mc)));
        }

        int created = 0, existing = 0, failed = 0;
        for (Future<String> f : futures) {
            try {
                String r = f.get();
                if (r == null) { existing++; }
                else if (r.startsWith("ERR:")) { failed++; log.accept("  " + r); }
                else { created++; log.accept("  + " + r); }
            } catch (Exception ex) { failed++; }
        }
        pool.shutdown();
        if (created > 0)
            log.accept("provisioning : " + created + " créés, " + existing + " déjà là, " + failed + " échecs");
        else
            log.accept("provisioning : tout est à jour (" + existing + " profils)");
    }

    private static String provisionOne(Path versionsDir, String mc) {
        try {
            // 1) skip si un profil valide existe déjà pour cette version
            if (Files.isDirectory(versionsDir)) {
                try (var stream = Files.list(versionsDir)) {
                    for (Path p : (Iterable<Path>) stream::iterator) {
                        String name = p.getFileName().toString();
                        if (!name.startsWith("fabric-loader-")) continue;
                        if (!name.endsWith("-" + mc)) continue;
                        Path json = p.resolve(name + ".json");
                        if (!Files.isRegularFile(json)) continue;
                        try {
                            JsonParser.parseString(Files.readString(json)).getAsJsonObject();
                            return null; // valide → skip
                        } catch (Exception corrupt) { /* re-provisionne */ }
                    }
                }
            }

            // 2) résolution du loader
            String meta = Http.getString("https://meta.fabricmc.net/v2/versions/loader/" + mc);
            JsonArray loaders = JsonParser.parseString(meta).getAsJsonArray();
            if (loaders.isEmpty()) return "ERR:" + mc + " : aucun loader";
            String loaderVersion = loaders.get(0).getAsJsonObject()
                    .getAsJsonObject("loader").get("version").getAsString();

            String profileId = "fabric-loader-" + loaderVersion + "-" + mc;
            Path dir = versionsDir.resolve(profileId);
            Path json = dir.resolve(profileId + ".json");
            if (Files.exists(json)) return null;

            // 3) profil complet
            String profileJson = Http.getString(
                    "https://meta.fabricmc.net/v2/versions/loader/" + mc + "/" + loaderVersion + "/profile/json");
            JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
            Files.createDirectories(dir);
            Files.writeString(json, new GsonBuilder().setPrettyPrinting().create().toJson(profile));
            return mc + " (loader " + loaderVersion + ")";
        } catch (Exception ex) {
            return "ERR:" + mc + " : " + ex.getMessage();
        }
    }
    private Provisioner() {}
}