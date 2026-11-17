package dev.portolan.launcher.ui;

import com.google.gson.*;
import dev.portolan.launcher.core.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.List;

public final class MainFrame extends JFrame {
    private static final Color BG     = new Color(24, 24, 28);
    private static final Color PANEL  = new Color(34, 34, 40);
    private static final Color INPUT  = new Color(45, 45, 52);
    private static final Color ACCENT = new Color(232, 232, 236);
    private static final Color TEXT   = new Color(228, 228, 232);
    private static final Color MUTED  = new Color(140, 140, 150);
    private static final Color LOG_BG = new Color(18, 18, 22);
    private static final Color LOG_FG = new Color(170, 200, 170);

    private final Path root = Paths.get(System.getProperty("user.home"), ".portolan");
    private final Path gameDir = root.resolve("game");
    private final Path assetsDir = root.resolve("assets");
    private final Path versionsDir = gameDir.resolve("versions");
    private final Path instancesDir = gameDir.resolve("instances");
    private final Path configFile = root.resolve("launcher.json");

    private record FabricVersion(String mcVersion, String profileId, Instant released) {}

    private final String sessionUser;
    private final JComboBox<String> versionCombo = new JComboBox<>();
    private final JSpinner memory = new JSpinner(new SpinnerNumberModel(2048, 512, 32768, 256));
    private final JTextArea logArea = new JTextArea();
    private final JButton playBtn = new JButton("Jouer");
    private final JLabel statusLabel = new JLabel("Pret");

    private Profiles profiles;
    private Map<String, String> mcToProfile = new HashMap<>();

    public MainFrame(String username) {
        super("Fares Client");
        this.sessionUser = username;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 620);
        setMinimumSize(new Dimension(700, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        profiles = Profiles.load(configFile);
        profiles.username = username;
        memory.setValue(profiles.memoryMb);

        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBackground(BG);
        rootPanel.add(buildHeader(), BorderLayout.NORTH);
        rootPanel.add(buildContent(), BorderLayout.CENTER);
        rootPanel.add(buildStatusBar(), BorderLayout.SOUTH);
        add(rootPanel);

        new Thread(this::bootstrap).start();
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(PANEL);
        h.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel brand = new JLabel("Fares Client");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setForeground(TEXT);

        JLabel user = new JLabel(sessionUser);
        user.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        user.setForeground(MUTED);

        JButton resetCfgBtn = new JButton("Reset configs");
        resetCfgBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resetCfgBtn.setForeground(MUTED);
        resetCfgBtn.setBackground(PANEL);
        resetCfgBtn.setFocusPainted(false);
        resetCfgBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        resetCfgBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetCfgBtn.putClientProperty("JButton.buttonType", "roundRect");
        resetCfgBtn.putClientProperty("JButton.arc", 6);
        resetCfgBtn.addActionListener(e -> onResetConfigs());

        JButton logout = new JButton("Deconnexion");
        logout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logout.setForeground(MUTED);
        logout.setBackground(PANEL);
        logout.setFocusPainted(false);
        logout.setBorder(new EmptyBorder(6, 12, 6, 12));
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.putClientProperty("JButton.buttonType", "roundRect");
        logout.putClientProperty("JButton.arc", 6);
        logout.addActionListener(e -> onLogout());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(resetCfgBtn);
        right.add(user);
        right.add(logout);

        h.add(brand, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private void onLogout() {
        try {
            Path authFile = root.resolve("auth.json");
            LocalAuth a = LocalAuth.load(authFile);
            if (a != null) a.clearToken(authFile);
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(PANEL);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 55, 65), 1),
                new EmptyBorder(20, 20, 20, 20)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 10, 12);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        JLabel vLbl = new JLabel("Version Fabric");
        vLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        vLbl.setForeground(MUTED);
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        controls.add(vLbl, g);

        versionCombo.setPreferredSize(new Dimension(280, 36));
        versionCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        versionCombo.setBackground(INPUT);
        versionCombo.setForeground(TEXT);
        versionCombo.putClientProperty("JComponent.roundRect", true);
        g.gridx = 1; g.gridy = 0; g.weightx = 1;
        controls.add(versionCombo, g);

        JLabel mLbl = new JLabel("Memoire (Mo)");
        mLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mLbl.setForeground(MUTED);
        g.gridx = 2; g.gridy = 0; g.weightx = 0;
        controls.add(mLbl, g);

        memory.setPreferredSize(new Dimension(100, 36));
        memory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g.gridx = 3; g.gridy = 0; g.weightx = 0;
        controls.add(memory, g);

        playBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        playBtn.setForeground(new Color(24, 24, 28));
        playBtn.setBackground(ACCENT);
        playBtn.setFocusPainted(false);
        playBtn.setBorder(new EmptyBorder(10, 32, 10, 32));
        playBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playBtn.putClientProperty("JButton.buttonType", "roundRect");
        playBtn.putClientProperty("JButton.arc", 8);
        playBtn.addActionListener(e -> onLaunch());

        g.gridx = 0; g.gridy = 1; g.gridwidth = 4; g.weightx = 1;
        g.insets = new Insets(8, 0, 0, 0);
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.EAST;
        controls.add(playBtn, g);

        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(LOG_BG);
        logArea.setForeground(LOG_FG);
        logArea.setCaretColor(TEXT);
        logArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 65), 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        content.add(controls, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL);
        bar.setBorder(new EmptyBorder(8, 24, 8, 24));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setStatus(String s) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(s));
    }

    private void bootstrap() {
        try {
            appendLog("=== Fares Client ===");
            Provisioner.provisionAll(versionsDir, this::appendLog);
            loadVersions();
        } catch (Exception ex) {
            appendLog("Erreur : " + ex);
        }
    }

    private void loadVersions() throws Exception {
        JsonObject manifest = Meta.manifest(root.resolve("cache"));
        Map<String, Instant> times = new HashMap<>();
        for (JsonElement el : manifest.getAsJsonArray("versions")) {
            JsonObject v = el.getAsJsonObject();
            String id = v.get("id").getAsString();
            Instant t = v.has("releaseTime")
                    ? Instant.parse(v.get("releaseTime").getAsString()) : Instant.EPOCH;
            times.put(id, t);
        }
        List<FabricVersion> raw = new ArrayList<>();
        if (Files.isDirectory(versionsDir)) {
            try (var stream = Files.list(versionsDir)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    String pid = p.getFileName().toString();
                    if (!pid.startsWith("fabric-loader-")) continue;
                    Path json = p.resolve(pid + ".json");
                    if (!Files.isRegularFile(json)) continue;
                    try {
                        JsonObject obj = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
                        if (!obj.has("inheritsFrom")) continue;
                        String mc = obj.get("inheritsFrom").getAsString();
                        raw.add(new FabricVersion(mc, pid, times.getOrDefault(mc, Instant.EPOCH)));
                    } catch (Exception ignored) {}
                }
            }
        }
        Map<String, FabricVersion> byMc = new LinkedHashMap<>();
        for (FabricVersion fv : raw) {
            FabricVersion prev = byMc.get(fv.mcVersion());
            if (prev == null || fv.profileId().compareTo(prev.profileId()) > 0)
                byMc.put(fv.mcVersion(), fv);
        }
        List<FabricVersion> sorted = new ArrayList<>(byMc.values());
        sorted.sort((a, b) -> b.released().compareTo(a.released()));

        mcToProfile.clear();
        for (FabricVersion fv : sorted) mcToProfile.put(fv.mcVersion(), fv.profileId());

        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (FabricVersion fv : sorted) model.addElement(fv.mcVersion());
            versionCombo.setModel(model);
            if (profiles.versionId != null && !profiles.versionId.isEmpty()
                    && mcToProfile.containsKey(profiles.versionId))
                versionCombo.setSelectedItem(profiles.versionId);
            else if (versionCombo.getItemCount() > 0)
                versionCombo.setSelectedIndex(0);
        });
        appendLog("Pret — " + sorted.size() + " versions Fabric disponibles");
        setStatus("Pret");
    }

    private void onResetConfigs() {
        Object sel = versionCombo.getSelectedItem();
        if (sel == null) { appendLog("Selectionne une version d'abord"); return; }
        String mc = sel.toString();
        String profileId = mcToProfile.get(mc);
        if (profileId == null) { appendLog("Profil introuvable pour " + mc); return; }

        Path instanceDir = instancesDir.resolve(profileId);
        Path config = instanceDir.resolve("config");
        try {
            Files.deleteIfExists(instanceDir.resolve("options.txt"));
            Files.deleteIfExists(config.resolve("entityculling.json"));
            Files.deleteIfExists(config.resolve("dynamicfps.toml"));
            Files.deleteIfExists(config.resolve("immediatelyfast.json"));
            Files.deleteIfExists(config.resolve("sodium-options.json"));
            appendLog("Configs supprimees, re-seed en cours pour " + mc + "...");
            new ConfigSeeder().seed(instanceDir, mc, this::appendLog);
            appendLog("Configs optimisees appliquees. Relance le jeu pour les voir.");
            setStatus("Configs reinitialisees");
        } catch (Exception ex) {
            appendLog("Erreur reset: " + ex.getMessage());
        }
    }

    private void onLaunch() {
        playBtn.setEnabled(false);
        setStatus("Lancement en cours...");
        new Thread(() -> {
            try {
                Object sel = versionCombo.getSelectedItem();
                if (sel == null) { appendLog("Aucune version selectionnee"); return; }
                String mcVersion = sel.toString();
                String profileId = mcToProfile.get(mcVersion);
                if (profileId == null) { appendLog("Profil introuvable : " + mcVersion); return; }
                int mem = (Integer) memory.getValue();

                profiles.username = sessionUser;
                profiles.versionId = mcVersion;
                profiles.memoryMb = mem;
                profiles.save(configFile);

                appendLog("=== Lancement " + mcVersion + " (" + sessionUser + ") ===");
                Path instanceDir = instancesDir.resolve(profileId);
                Files.createDirectories(instanceDir);

                appendLog("Preparation des mods...");
                new ModManager().ensureMods(mcVersion, instanceDir, this::appendLog);

                appendLog("Application des configs optimisees...");
                new ConfigSeeder().seed(instanceDir, mcVersion, this::appendLog);

                Path vDir = versionsDir.resolve(profileId);
                Path localJson = vDir.resolve(profileId + ".json");
                if (!Files.exists(localJson)) { appendLog("Profil Fabric manquant"); return; }
                JsonObject version = JsonParser.parseString(Files.readString(localJson)).getAsJsonObject();

                JsonObject resolved = Meta.resolve(versionsDir, version, root.resolve("cache"));
                Installer inst = new Installer(gameDir, assetsDir);
                Plan plan = inst.prepare(resolved, profileId);
                inst.shutdown();

                int jdkMajor = 17;
                if (resolved.has("javaVersion") && resolved.getAsJsonObject("javaVersion").has("majorVersion"))
                    jdkMajor = resolved.getAsJsonObject("javaVersion").get("majorVersion").getAsInt();
                Path javaExe = JdkFinder.find(jdkMajor);
                if (javaExe == null) {
                    appendLog("JDK " + jdkMajor + " introuvable — winget install EclipseAdoptium.Temurin." + jdkMajor + ".JDK");
                    setStatus("JDK manquant");
                    return;
                }
                appendLog("JDK " + jdkMajor + " : " + javaExe);

                Auth.Session session = Auth.offline(sessionUser);
                appendLog("Lancement du jeu...");
                Process p = GameLauncher.launch(plan, session, instanceDir, assetsDir, mem, javaExe);
                appendLog("PID " + p.pid());
                setStatus("En jeu — PID " + p.pid());
            } catch (Exception ex) {
                appendLog("Echec : " + ex);
                setStatus("Erreur");
                ex.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> playBtn.setEnabled(true));
            }
        }).start();
    }
}