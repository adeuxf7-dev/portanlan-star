package dev.portolan.launcher.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameLauncher {
    private static final Pattern PH = Pattern.compile("\\$\\{([^}]+)}");

    public static Process launch(Plan plan, Auth.Session session, Path gameDir,
                                 Path assetsDir, int memMb, Path javaExe) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe.toAbsolutePath().toString());
        cmd.add("-Xmx" + memMb + "M");
        cmd.addAll(substitute(plan.jvmArgs(), plan, session, gameDir, assetsDir));
        cmd.add(plan.mainClass());
        cmd.addAll(substitute(plan.gameArgs(), plan, session, gameDir, assetsDir));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(gameDir.toFile());
        pb.redirectErrorStream(true);
        Path log = gameDir.resolve("logs").resolve("portolan-launch.log");
        Files.createDirectories(log.getParent());
        pb.redirectOutput(log.toFile());
        return pb.start();
    }

    private static List<String> substitute(List<String> in, Plan plan,
                                           Auth.Session s, Path gameDir, Path assetsDir) {
        StringBuilder cp = new StringBuilder();
        for (Path p : plan.classpath()) {
            if (cp.length() > 0) cp.append(java.io.File.pathSeparatorChar);
            cp.append(p.toAbsolutePath());
        }
        Map<String, String> ph = new HashMap<>();
        ph.put("auth_player_name", s.username());
        ph.put("version_name", plan.id());
        ph.put("game_directory", gameDir.toAbsolutePath().toString());
        ph.put("assets_root", assetsDir.toAbsolutePath().toString());
        ph.put("assets_index_name", plan.assetIndexId());
        ph.put("auth_uuid", s.uuid());
        ph.put("auth_access_token", s.accessToken());
        ph.put("auth_session", "token:" + s.accessToken() + ":" + s.uuid());
        ph.put("user_type", s.userType());
        ph.put("version_type", plan.type());
        ph.put("natives_directory", plan.nativesDir().toAbsolutePath().toString());
        ph.put("launcher_name", "portolan");
        ph.put("launcher_version", "1.0");
        ph.put("classpath", cp.toString());
        ph.put("user_properties", "{}");
        ph.put("clientid", "");
        ph.put("auth_xuid", "");
        ph.put("game_assets", assetsDir.resolve("virtual").resolve("legacy").toString());

        List<String> out = new ArrayList<>(in.size());
        for (String arg : in) {
            Matcher m = PH.matcher(arg);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String v = ph.get(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(v != null ? v : ""));
            }
            m.appendTail(sb);
            out.add(sb.toString());
        }
        return out;
    }
    private GameLauncher() {}
}