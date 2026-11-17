package dev.portolan.launcher.core;

import com.google.gson.*;
import java.nio.file.*;

public final class Profiles {
    public String username = "Player";
    public String versionId = "";
    public int memoryMb = 2048;

    public static Profiles load(Path file) {
        try {
            if (Files.exists(file)) {
                return new Gson().fromJson(Files.readString(file), Profiles.class);
            }
        } catch (Exception ignored) {}
        return new Profiles();
    }

    public void save(Path file) {
        try {
            Path p = file.toAbsolutePath().getParent();
            if (p != null) Files.createDirectories(p);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception ignored) {}
    }
}
