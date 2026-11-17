package dev.portolan.launcher.core;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Installer {
    private final Path gameDir;
    private final Path assetsDir;
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    public Installer(Path gameDir, Path assetsDir) {
        this.gameDir = gameDir;
        this.assetsDir = assetsDir;
    }

    public Plan prepare(JsonObject version, String versionId) throws Exception {
        String id = version.has("id") ? version.get("id").getAsString() : versionId;
        String type = version.has("type") ? version.get("type").getAsString() : "release";
        String mainClass = version.get("mainClass").getAsString();

        Path nativesDir = gameDir.resolve("natives").resolve(id);
        Files.createDirectories(nativesDir);

        List<Path> classpath = new ArrayList<>();
        if (version.has("libraries")) {
            for (JsonElement el : version.getAsJsonArray("libraries")) {
                JsonObject lib = el.getAsJsonObject();
                if (!Rules.allowed(lib)) continue;
                String name = lib.get("name").getAsString();
                String[] parts = name.split(":");
                String classifier = parts.length >= 4 ? parts[3] : null;

                if (lib.has("natives")) {
                    String key = lib.getAsJsonObject("natives").get(OS.NAME).getAsString()
                            .replace("${arch}", OS.ARCH.equals("x86_64") ? "64" : "32");
                    JsonObject art = lib.getAsJsonObject("downloads").getAsJsonObject("classifiers")
                            .getAsJsonObject(key);
                    Path jar = fetch(art, "libraries");
                    extract(jar, nativesDir, lib);
                } else if (classifier != null && classifier.startsWith("natives-")) {
                    Path jar = fetch(artifact(lib), "libraries");
                    extract(jar, nativesDir, lib);
                } else {
                    JsonObject art = artifact(lib);
                    if (art != null) classpath.add(fetch(art, "libraries"));
                }
            }
        }

        JsonObject client = version.getAsJsonObject("downloads").getAsJsonObject("client");
        Path clientJar = fetch(client, "versions/" + id);
        classpath.add(clientJar);

        String assetIndexId;
        JsonObject assetIndex;
        if (version.has("assetIndex")) assetIndex = version.getAsJsonObject("assetIndex");
        else assetIndex = version.getAsJsonObject("downloads").getAsJsonObject("assetIndex");
        assetIndexId = assetIndex.get("id").getAsString();
        Path indexFile = assetsDir.resolve("indexes").resolve(assetIndexId + ".json");
        Http.download(assetIndex.get("url").getAsString(), indexFile,
                assetIndex.has("sha1") ? assetIndex.get("sha1").getAsString() : null);
        downloadObjects(indexFile);

        List<String> jvm = new ArrayList<>();
        List<String> game = new ArrayList<>();
        if (version.has("arguments")) {
            JsonObject a = version.getAsJsonObject("arguments");
            if (a.has("jvm"))  jvm.addAll(resolveArgs(a.getAsJsonArray("jvm")));
            if (a.has("game")) game.addAll(resolveArgs(a.getAsJsonArray("game")));
        }
        if (jvm.isEmpty()) {
            jvm.add("-Djava.library.path=${natives_directory}");
            jvm.add("-cp");
            jvm.add("${classpath}");
        }
        if (game.isEmpty() && version.has("minecraftArguments")) {
            game.addAll(Arrays.asList(version.get("minecraftArguments").getAsString().trim().split("\\s+")));
        }

        if (mainClass.contains("launchwrapper") || id.toLowerCase().contains("forge")) {
            jvm.add(0, "-Dfml.ignoreInvalidMinecraftCertificates=true");
            jvm.add(1, "-Dfml.ignorePatchDiscrepancies=true");
        }
        jvm.add("-Dorg.lwjgl.librarypath=${natives_directory}");
        jvm.add("-Dminecraft.launcher.brand=portolan");
        jvm.add("-Dminecraft.launcher.version=1.0");

        return new Plan(id, type, mainClass, jvm, game, classpath, nativesDir, assetIndexId);
    }

    private JsonObject artifact(JsonObject lib) {
        if (lib.has("downloads") && lib.getAsJsonObject("downloads").has("artifact"))
            return lib.getAsJsonObject("downloads").getAsJsonObject("artifact");
        String name = lib.get("name").getAsString();
        String[] p = name.split(":");
        String base = lib.has("url") ? lib.get("url").getAsString()
                : "https://libraries.minecraft.net/";
        if (!base.endsWith("/")) base += "/";
        String groupPath = p[0].replace('.', '/');
        String file = p[1] + "-" + p[2] + (p.length >= 4 ? "-" + p[3] : "") + ".jar";
        JsonObject art = new JsonObject();
        art.addProperty("url", base + groupPath + "/" + p[1] + "/" + p[2] + "/" + file);
        return art;
    }

    private Path fetch(JsonObject art, String subdir) throws Exception {
        String url = art.get("url").getAsString();
        String sha1 = art.has("sha1") && !art.get("sha1").isJsonNull()
                ? art.get("sha1").getAsString() : null;
        String path = art.has("path") ? art.get("path").getAsString()
                : url.substring(url.lastIndexOf('/') + 1);
        Path dest = gameDir.resolve(subdir).resolve(path);
        Http.download(url, dest, sha1);
        return dest;
    }

    private void extract(Path jar, Path nativesDir, JsonObject lib) throws IOException {
        List<String> excludes = new ArrayList<>();
        if (lib.has("extract") && lib.getAsJsonObject("extract").has("exclude"))
            for (JsonElement e : lib.getAsJsonObject("extract").getAsJsonArray("exclude"))
                excludes.add(e.getAsString());
        Files.createDirectories(nativesDir);
        try (ZipFile zf = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                String n = ze.getName().replace('\\', '/');
                if (ze.isDirectory() || n.startsWith("META-INF/")) continue;

                // unified LWJGL jars carry windows/x64 + windows/arm64 + linux/x64 ...
                // keep only the subdir matching this JVM's OS/arch, then flatten to basename.
                if (n.startsWith("windows/arm64/") && !OS.ARCH.equals("arm64")) continue;
                if (n.startsWith("windows/x64/")   && !OS.ARCH.equals("x86_64")) continue;
                if (n.startsWith("windows/x86/")   && !OS.ARCH.equals("x86")) continue;
                if (n.startsWith("linux/")         && !OS.NAME.equals("linux")) continue;
                if (n.startsWith("macos/")         && !OS.NAME.equals("osx")) continue;

                String base = n.substring(n.lastIndexOf('/') + 1);
                boolean skip = false;
                for (String ex : excludes) if (base.startsWith(ex)) { skip = true; break; }
                if (skip) continue;

                Path out = nativesDir.resolve(base);
                try (InputStream in = zf.getInputStream(ze)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void downloadObjects(Path indexFile) throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        JsonObject objects = root.getAsJsonObject("objects");
        List<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : objects.entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            String hash = o.get("hash").getAsString();
            String sub = hash.substring(0, 2);
            Path dest = assetsDir.resolve("objects").resolve(sub).resolve(hash);
            String url = "https://resources.download.minecraft.net/" + sub + "/" + hash;
            futures.add(pool.submit(() -> {
                Http.download(url, dest, hash);
                return null;
            }));
        }
        for (Future<?> f : futures) f.get();
    }

    private List<String> resolveArgs(JsonArray arr) {
        List<String> out = new ArrayList<>();
        for (JsonElement el : arr) {
            if (el.isJsonPrimitive()) { out.add(el.getAsString()); continue; }
            JsonObject o = el.getAsJsonObject();
            if (!Rules.allowed(o)) continue;
            JsonElement v = o.get("value");
            if (v.isJsonArray()) for (JsonElement x : v.getAsJsonArray()) out.add(x.getAsString());
            else out.add(v.getAsString());
        }
        return out;
    }

    public void shutdown() { pool.shutdownNow(); }
}
