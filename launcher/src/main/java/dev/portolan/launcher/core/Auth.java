package dev.portolan.launcher.core;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Auth {
    public record Session(String username, String uuid, String accessToken, String userType) {}

    // Offline UUID matches vanilla's OfflinePlayer name-based UUID. No authserver call.
    public static Session offline(String username) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        return new Session(username, id.toString().replace("-", ""), "0", "legacy");
    }
}
