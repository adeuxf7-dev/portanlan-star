package dev.portolan.launcher.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;

public final class LocalAuth {
    public String username;
    public String salt;
    public String hash;
    public String rememberToken;

    public static LocalAuth load(Path file) {
        try {
            if (!Files.exists(file)) return null;
            return new Gson().fromJson(Files.readString(file), LocalAuth.class);
        } catch (Exception ex) { return null; }
    }

    public void save(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(this));
    }

    public static LocalAuth register(Path file, String username, String password) throws Exception {
        LocalAuth a = new LocalAuth();
        a.username = username;
        a.salt = randomHex(16);
        a.hash = sha256(a.salt + password);
        a.rememberToken = randomHex(32);
        a.save(file);
        return a;
    }

    public static LocalAuth login(Path file, String password) throws Exception {
        LocalAuth a = load(file);
        if (a == null || !a.verify(password)) return null;
        if (a.rememberToken == null || a.rememberToken.isEmpty()) {
            a.rememberToken = randomHex(32);
            a.save(file);
        }
        return a;
    }

    public boolean verify(String password) {
        return hash != null && hash.equals(sha256(salt + password));
    }

    public boolean hasRemembered() {
        return rememberToken != null && !rememberToken.isEmpty();
    }

    public void clearToken(Path file) throws IOException {
        this.rememberToken = "";
        save(file);
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}