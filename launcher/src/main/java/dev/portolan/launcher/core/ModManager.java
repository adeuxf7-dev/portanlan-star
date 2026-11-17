package dev.portolan.launcher.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Auto-installe Sodium + Fabric API + Mod Menu + Meteor Client dans une instance.
 * Sodium / Fabric API / Mod Menu viennent de Modrinth (API publique).
 * Meteor vient de son archive communautaire ou GitHub (pas sur Modrinth).
 * Idempotent : skip un mod si un jar avec son préfixe existe déjà dans mods/.
 */
public final class ModManager {

    public record ModSpec(String slug, String displayName) {}

    public static final List<ModSpec> REQUIRED = List.of(
            // --- Base & UI ---
            new ModSpec("sodium",                   "Sodium"),
            new ModSpec("fabric-api",               "Fabric API"),
            new ModSpec("modmenu",                  "Mod Menu"),
            new ModSpec("cloth-config",             "Cloth Config API"),

            // --- Rendu & Culling ---
            new ModSpec("entityculling",            "Entity Culling"),
            new ModSpec("immediatelyfast",          "ImmediatelyFast"),
            new ModSpec("moreculling",              "More Culling"),

            // --- Logique du jeu (Tick) ---
            new ModSpec("lithium",                  "Lithium"),

            // --- Mémoire ---
            new ModSpec("ferrite-core",             "FerriteCore"),

            // --- Réseau & Multiplayer ---
            new ModSpec("krypton",                  "Krypton"),

            // --- Gestion des ressources (hors focus) ---
            new ModSpec("dynamic-fps",              "Dynamic FPS")
    );

    public void ensureMods(String mcVersion, Path instanceDir,
                           java.util.function.Consumer<String> log)
            throws IOException, InterruptedException {
        Path mods = instanceDir.resolve("mods");
        Files.createDirectories(mods);

        for (ModSpec spec : REQUIRED) {
            if (present(mods, spec)) {
                log.accept("  " + spec.displayName() + " : déjà là");
                continue;
            }
            try {
                installOne(spec, mcVersion, mods, log);
            } catch (Exception ex) {
                log.accept("  " + spec.displayName() + " : échec — " + ex.getMessage());
            }
        }

        new MeteorManager().ensureMeteor(mcVersion, instanceDir, log);
        new OpSecManager().ensureOpSec(mcVersion, instanceDir, log);
    }

    private boolean present(Path mods, ModSpec spec) throws IOException {
        String prefix = spec.slug().toLowerCase() + "-";
        try (var stream = Files.list(mods)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (p.getFileName().toString().toLowerCase().startsWith(prefix)) return true;
            }
        }
        return false;
    }

    private void installOne(ModSpec spec, String mcVersion, Path modsDir,
                            java.util.function.Consumer<String> log)
            throws IOException, InterruptedException {
        String url = "https://api.modrinth.com/v2/project/" + spec.slug() + "/version"
                + "?loaders=%5B%22fabric%22%5D"
                + "&game_versions=%5B%22" + mcVersion + "%22%5D";
        JsonArray versions = JsonParser.parseString(Http.getString(url)).getAsJsonArray();
        if (versions.isEmpty()) {
            log.accept("  " + spec.displayName() + " : pas de build Fabric pour " + mcVersion);
            return;
        }
        JsonObject pick = null;
        for (JsonElement el : versions) {
            JsonObject v = el.getAsJsonObject();
            if ("release".equals(v.get("version_type").getAsString())) { pick = v; break; }
        }
        if (pick == null) pick = versions.get(0).getAsJsonObject();

        JsonArray files = pick.getAsJsonArray("files");
        JsonObject primary = null;
        for (JsonElement f : files) {
            JsonObject o = f.getAsJsonObject();
            if (o.has("primary") && o.get("primary").getAsBoolean()) { primary = o; break; }
        }
        if (primary == null) primary = files.get(0).getAsJsonObject();

        String dl  = primary.get("url").getAsString();
        String fn  = primary.get("filename").getAsString();
        String sha = primary.has("hashes") && primary.getAsJsonObject("hashes").has("sha1")
                ? primary.getAsJsonObject("hashes").get("sha1").getAsString() : null;

        Path dest = modsDir.resolve(fn);
        log.accept("  " + spec.displayName() + " : " + fn);
        Http.download(dl, dest, sha);
    }

    public ModManager() {}
}