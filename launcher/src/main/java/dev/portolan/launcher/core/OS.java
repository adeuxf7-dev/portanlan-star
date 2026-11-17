package dev.portolan.launcher.core;

public final class OS {
    public static final String NAME;
    public static final String ARCH;
    static {
        String n = System.getProperty("os.name").toLowerCase();
        NAME = n.contains("win") ? "windows"
             : (n.contains("mac") || n.contains("darwin")) ? "osx" : "linux";
        String a = System.getProperty("os.arch").toLowerCase();
        ARCH = a.contains("aarch64") ? "arm64" : a.contains("64") ? "x86_64" : "x86";
    }
    private OS() {}
}
