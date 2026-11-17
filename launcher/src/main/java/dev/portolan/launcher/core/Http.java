package dev.portolan.launcher.core;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.util.Map;

public final class Http {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS).build();

    public static String getString(String url) throws IOException, InterruptedException {
        return getStringWithHeaders(url, Map.of());
    }

    public static String getStringWithHeaders(String url, Map<String,String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "PortolanLauncher/1.0")
                .GET();
        headers.forEach(b::header);
        HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " " + url);
        return r.body();
    }

    public static void download(String url, Path dest, String sha1)
            throws IOException, InterruptedException {
        download(url, dest, sha1, Map.of());
    }

    public static void download(String url, Path dest, String sha1, Map<String,String> headers)
            throws IOException, InterruptedException {
        if (sha1 != null && Files.exists(dest) && sha1.equalsIgnoreCase(sha1(dest))) return;
        try {
            downloadOnce(url, dest, sha1, headers);
        } catch (Http416 | RangeMismatch e) {
            Files.deleteIfExists(dest);
            downloadOnce(url, dest, sha1, headers);
        }
        if (dest.toString().toLowerCase().endsWith(".jar") && !isZip(dest)) {
            long size = Files.size(dest);
            Files.deleteIfExists(dest);
            throw new IOException("not a valid jar (" + size + " bytes from " + url + ")");
        }
    }

    private static void downloadOnce(String url, Path dest, String sha1, Map<String,String> headers)
            throws IOException, InterruptedException {
        Path parent = dest.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        long existing = Files.exists(dest) ? Files.size(dest) : 0;
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "PortolanLauncher/1.0")
                .GET();
        headers.forEach(b::header);
        if (existing > 0) b.header("Range", "bytes=" + existing + "-");
        HttpResponse<InputStream> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofInputStream());
        int code = r.statusCode();
        if (code == 416) throw new Http416(url);
        if (code != 200 && code != 206) throw new IOException("HTTP " + code + " " + url);
        boolean append = (code == 206 && existing > 0);
        if (code == 200 && existing > 0) {
            try (InputStream ignored = r.body()) {}
            Files.deleteIfExists(dest);
            throw new RangeMismatch(url);
        }
        try (InputStream in = r.body();
             OutputStream out = append
                     ? Files.newOutputStream(dest, StandardOpenOption.APPEND)
                     : Files.newOutputStream(dest, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            in.transferTo(out);
        }
        if (sha1 != null && !sha1.equalsIgnoreCase(sha1(dest))) {
            Files.deleteIfExists(dest);
            throw new IOException("sha1 mismatch " + url);
        }
    }

    private static boolean isZip(Path p) {
        try (InputStream in = Files.newInputStream(p)) {
            byte[] m = new byte[4];
            if (in.read(m) < 4) return false;
            return m[0] == 0x50 && m[1] == 0x4B && m[2] == 0x03 && m[3] == 0x04;
        } catch (IOException ex) { return false; }
    }

    public static String sha1(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte x : md.digest()) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    private static final class Http416 extends IOException {
        Http416(String url) { super("HTTP 416 " + url); }
    }
    private static final class RangeMismatch extends IOException {
        RangeMismatch(String url) { super("server ignored range; restarting " + url); }
    }
    private Http() {}
}