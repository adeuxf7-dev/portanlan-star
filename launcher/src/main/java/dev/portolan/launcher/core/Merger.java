package dev.portolan.launcher.core;

import com.google.gson.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Merger {
    public static JsonObject merge(JsonObject parent, JsonObject child) {
        JsonObject out = parent.deepCopy();
        for (Map.Entry<String, JsonElement> e : child.entrySet()) {
            String k = e.getKey();
            switch (k) {
                case "libraries" -> {
                    JsonArray arr = new JsonArray();
                    if (parent.has("libraries")) arr.addAll(parent.getAsJsonArray("libraries"));
                    if (child.has("libraries"))  arr.addAll(child.getAsJsonArray("libraries"));
                    out.add("libraries", dedupe(arr));
                }
                case "arguments" -> out.add("arguments", mergeArgs(
                        parent.getAsJsonObject("arguments"),
                        child.getAsJsonObject("arguments")));
                case "minecraftArguments" -> out.add(k, e.getValue());
                case "inheritsFrom" -> {}
                default -> out.add(k, e.getValue());
            }
        }
        out.remove("inheritsFrom");
        return out;
    }

    private static JsonObject mergeArgs(JsonObject p, JsonObject c) {
        JsonObject out = new JsonObject();
        for (String key : new String[]{"game", "jvm"}) {
            JsonArray arr = new JsonArray();
            if (p != null && p.has(key)) arr.addAll(p.getAsJsonArray(key));
            if (c != null && c.has(key)) arr.addAll(c.getAsJsonArray(key));
            out.add(key, arr);
        }
        return out;
    }

    private static JsonArray dedupe(JsonArray libs) {
        Map<String, JsonObject> byName = new LinkedHashMap<>();
        for (JsonElement el : libs) {
            JsonObject o = el.getAsJsonObject();
            byName.put(o.get("name").getAsString(), o);
        }
        JsonArray out = new JsonArray();
        byName.values().forEach(out::add);
        return out;
    }
    private Merger() {}
}
