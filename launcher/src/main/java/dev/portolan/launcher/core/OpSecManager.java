package dev.portolan.launcher.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

public final class OpSecManager {

    private static final long MOD_ID = 1702991L;

    private record FileInfo(long id, String name) {}

    // Mapping version MC -> (fileId, nom du fichier), utilise quand l'API echoue.
    // Le fileId vient du log launcher (9352499) — verifie sur curseforge.com.
    private static final Map<String, FileInfo> KNOWN = Map.ofEntries(
            Map.entry("26.3", new FileInfo(9352499L, "opsec-26.3+v1.9.1-enhanced.jar"))
    );

    public void ensureOpSec(String mcVersion, Path instanceDir,
                            java.util.function.Consumer<String> log)
            throws IOException, InterruptedException {
        Path mods = instanceDir.resolve("mods");
        Files.createDirectories(mods);

        try (var stream = Files.list(mods)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (p.getFileName().toString().toLowerCase().contains("opsec")) {
                    log.accept("  OpSec Enhanced : deja la");
                    return;
                }
            }
        }

        long fileId;
        String fileName;

        FileInfo known = KNOWN.get(mcVersion);
        if (known != null) {
            fileId = known.id();
            fileName = known.name();
            log.accept("  OpSec : fallback hardcode -> " + fileName);
        } else {
            String listUrl = "https://api.curse.tools/v1/cf/mods/" + MOD_ID
                    + "/files?gameVersion=" + mcVersion + "&modLoaderType=4";
            Map<String,String> headers = Map.of("User-Agent", "PortolanLauncher/1.0");
            try {
                String body = Http.getStringWithHeaders(listUrl, headers);
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                if (!root.has("data")) {
                    log.accept("  OpSec : reponse API invalide pour " + mcVersion);
                    return;
                }
                JsonArray files = root.getAsJsonArray("data");
                JsonObject best = null;
                for (JsonElement el : files) {
                    JsonObject f = el.getAsJsonObject();
                    String fn = f.get("fileName").getAsString().toLowerCase();
                    if (!fn.endsWith(".jar")) continue;
                    if (fn.contains("-sources") || fn.contains("-javadoc")) continue;
                    int rt = f.has("releaseType") ? f.get("releaseType").getAsInt() : 3;
                    int bestRt = best != null && best.has("releaseType")
                            ? best.get("releaseType").getAsInt() : 99;
                    if (best == null || rt < bestRt) best = f;
                }
                if (best == null) {
                    log.accept("  OpSec : aucun build Fabric pour " + mcVersion);
                    return;
                }
                fileId = best.get("id").getAsLong();
                fileName = best.get("fileName").getAsString();
            } catch (Exception ex) {
                log.accept("  OpSec : API echouee pour " + mcVersion
                        + " (" + ex.getMessage() + ") — pas de fallback hardcode");
                return;
            }
        }

        String idStr = Long.toString(fileId);
        String part1 = idStr.substring(0, 4);
        String part2 = idStr.substring(4, 7);
        String cdn = "https://edge.forgecdn.net/files/" + part1 + "/" + part2 + "/"
                + java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");

        log.accept("  OpSec Enhanced : " + fileName);
        log.accept("  OpSec : URL -> " + cdn);
        Map<String,String> dlHeaders = Map.of("User-Agent", "PortolanLauncher/1.0");
        try {
            Http.download(cdn, mods.resolve(fileName), null, dlHeaders);
        } catch (Exception ex) {
            log.accept("  OpSec : telechargement CDN echoue — " + ex.getMessage());
        }
    }
}