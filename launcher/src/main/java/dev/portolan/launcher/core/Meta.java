package dev.portolan.launcher.core;

import com.google.gson.*;
import java.nio.file.*;
import java.time.Instant;

public final class Meta {
    private static final String MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public static JsonObject manifest(Path cacheDir) throws Exception {
        Path cache = cacheDir.resolve("version_manifest_v2.json");
        if (Files.exists(cache) &&
            Files.getLastModifiedTime(cache).toInstant().isAfter(Instant.now().minusSeconds(3600))) {
            return JsonParser.parseString(Files.readString(cache)).getAsJsonObject();
        }
        String body = Http.getString(MANIFEST_URL);
        Files.createDirectories(cacheDir);
        Files.writeString(cache, body);
        return JsonParser.parseString(body).getAsJsonObject();
    }

    public static String urlFor(JsonObject manifest, String id) {
        for (JsonElement el : manifest.getAsJsonArray("versions")) {
            JsonObject v = el.getAsJsonObject();
            if (v.get("id").getAsString().equals(id)) return v.get("url").getAsString();
        }
        return null;
    }

    public static JsonObject fetchVersion(String url) throws Exception {
        return JsonParser.parseString(Http.getString(url)).getAsJsonObject();
    }

    public static JsonObject resolve(Path versionsDir, JsonObject version,
                                     Path cacheDir) throws Exception {
        JsonObject current = version;
        int guard = 0;
        while (current.has("inheritsFrom") && guard++ < 16) {
            String parentId = current.get("inheritsFrom").getAsString();
            Path local = versionsDir.resolve(parentId).resolve(parentId + ".json");
            JsonObject parent;
            if (Files.exists(local)) {
                parent = JsonParser.parseString(Files.readString(local)).getAsJsonObject();
            } else {
                JsonObject manifest = manifest(cacheDir);
                String url = urlFor(manifest, parentId);
                if (url == null) throw new IllegalStateException("parent version not found: " + parentId);
                parent = fetchVersion(url);
            }
            current = Merger.merge(parent, current);
        }
        return current;
    }

    private Meta() {}
}
