package dev.portolan.launcher.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

public final class Rules {
    public static boolean allowed(JsonObject holder) {
        if (!holder.has("rules")) return true;
        boolean allow = false;
        for (JsonElement el : holder.getAsJsonArray("rules")) {
            JsonObject r = el.getAsJsonObject();
            if (matches(r)) allow = "allow".equals(r.get("action").getAsString());
        }
        return allow;
    }

    private static boolean matches(JsonObject r) {
        if (r.has("os")) {
            JsonObject os = r.getAsJsonObject("os");
            if (os.has("name") && !OS.NAME.equals(os.get("name").getAsString())) return false;
            if (os.has("arch") && !OS.ARCH.equals(os.get("arch").getAsString())) return false;
        }
        if (r.has("features")) {
            for (Map.Entry<String, JsonElement> e : r.getAsJsonObject("features").entrySet()) {
                if (e.getValue().getAsBoolean()) return false;
            }
        }
        return true;
    }
    private Rules() {}
}
