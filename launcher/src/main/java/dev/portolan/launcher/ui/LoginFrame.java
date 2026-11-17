package dev.portolan.launcher.ui;

import dev.portolan.launcher.core.LocalAuth;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LoginFrame extends JFrame {
    private static final Color BG     = new Color(24, 24, 28);
    private static final Color CARD   = new Color(34, 34, 40);
    private static final Color INPUT  = new Color(45, 45, 52);
    private static final Color ACCENT = new Color(232, 232, 236);
    private static final Color TEXT   = new Color(228, 228, 232);
    private static final Color MUTED  = new Color(140, 140, 150);
    private static final Color ERROR  = new Color(230, 100, 100);
    private static final Color LINK   = new Color(200, 200, 210);

    private final Path authFile = Paths.get(System.getProperty("user.home"), ".portolan", "auth.json");
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JPasswordField confirm  = new JPasswordField();
    private final JLabel title    = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JLabel status   = new JLabel(" ");
    private final JLabel confirmLbl = new JLabel("Confirmer le mot de passe");
    private final JButton primary = new JButton();
    private final JButton secondary = new JButton();
    private final JLabel forgotLink = new JLabel("Mot de passe oublie ?");

    private enum Mode { LOGIN, REGISTER }
    private Mode mode;

    public LoginFrame() {
        super("Fares Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG);

        LocalAuth existing = LocalAuth.load(authFile);
        mode = existing == null ? Mode.REGISTER : Mode.LOGIN;

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(36, 32, 36, 32));
        card.setPreferredSize(new Dimension(360, 460));

        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLbl = new JLabel("Pseudo");
        userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userLbl.setForeground(MUTED);
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = new JLabel("Mot de passe");
        passLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        passLbl.setForeground(MUTED);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        confirmLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        confirmLbl.setForeground(MUTED);
        confirmLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleField(username);
        styleField(password);
        styleField(confirm);

        if (existing != null) username.setText(existing.username);

        primary.setAlignmentX(Component.LEFT_ALIGNMENT);
        primary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        primary.setPreferredSize(new Dimension(Integer.MAX_VALUE, 42));
        primary.setFont(new Font("Segoe UI", Font.BOLD, 14));
        primary.setBackground(ACCENT);
        primary.setForeground(new Color(24, 24, 28));
        primary.setFocusPainted(false);
        primary.setBorder(new EmptyBorder(10, 20, 10, 20));
        primary.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        primary.putClientProperty("JButton.buttonType", "roundRect");
        primary.putClientProperty("JButton.arc", 8);
        primary.addActionListener(this::onSubmit);

        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        secondary.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        secondary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        secondary.setBackground(CARD);
        secondary.setForeground(LINK);
        secondary.setFocusPainted(false);
        secondary.setBorder(new EmptyBorder(8, 20, 8, 20));
        secondary.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        secondary.putClientProperty("JButton.buttonType", "roundRect");
        secondary.putClientProperty("JButton.arc", 8);
        secondary.addActionListener(e -> toggleMode());

        forgotLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotLink.setForeground(LINK);
        forgotLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        forgotLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onForgot(); }
        });

        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setForeground(ERROR);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));
        card.add(userLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(username);
        card.add(Box.createVerticalStrut(12));
        card.add(passLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(password);
        card.add(Box.createVerticalStrut(12));
        card.add(confirmLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(confirm);
        card.add(Box.createVerticalStrut(6));
        card.add(status);
        card.add(Box.createVerticalStrut(10));
        card.add(forgotLink);
        card.add(Box.createVerticalStrut(10));
        card.add(primary);
        card.add(Box.createVerticalStrut(6));
        card.add(secondary);

        root.add(card);
        add(root);
        getRootPane().setDefaultButton(primary);

        applyMode();
    }

    private void styleField(JTextField f) {
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
                new EmptyBorder(8, 12, 8, 12)));
        f.putClientProperty("JComponent.roundRect", true);
    }

    private void applyMode() {
        boolean reg = mode == Mode.REGISTER;
        title.setText(reg ? "Creation de compte" : "Connexion");
        LocalAuth ex = LocalAuth.load(authFile);
        subtitle.setText(reg ? "Nouveau profil local"
                             : (ex != null ? "Bon retour, " + ex.username : ""));
        primary.setText(reg ? "Creer" : "Connexion");
        secondary.setText(reg ? "J'ai deja un compte" : "Creer un autre compte");
        confirmLbl.setVisible(reg);
        confirm.setVisible(reg);
        forgotLink.setVisible(!reg);
        status.setText(" ");
        revalidate();
        repaint();
    }

    private void toggleMode() {
        mode = mode == Mode.LOGIN ? Mode.REGISTER : Mode.LOGIN;
        if (mode == Mode.REGISTER) password.setText("");
        applyMode();
    }

    private void onForgot() {
        int r = JOptionPane.showConfirmDialog(this,
                "Supprimer le profil local et creer un nouveau compte ?\n"
                + "Le mot de passe sera perdu (pas les sauvegardes).",
                "Mot de passe oublie",
                JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            Files.deleteIfExists(authFile);
            mode = Mode.REGISTER;
            password.setText("");
            confirm.setText("");
            applyMode();
            status.setForeground(MUTED);
            status.setText("Profil supprime, cree un nouveau compte.");
        } catch (Exception ex) {
            status.setForeground(ERROR);
            status.setText("Erreur : " + ex.getMessage());
        }
    }

    private void onSubmit(ActionEvent e) {
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String conf = new String(confirm.getPassword());
        status.setForeground(ERROR);

        if (user.isEmpty()) { status.setText("Pseudo requis."); return; }
        if (pass.length() < 4) { status.setText("4 caracteres minimum."); return; }

        try {
            LocalAuth existing = LocalAuth.load(authFile);

            if (mode == Mode.REGISTER) {
                if (existing != null && existing.username.equalsIgnoreCase(user)) {
                    status.setText("Ce compte existe deja. Entre ton mot de passe.");
                    mode = Mode.LOGIN;
                    applyMode();
                    status.setForeground(ERROR);
                    status.setText("Ce compte existe deja. Entre ton mot de passe.");
                    return;
                }
                if (!pass.equals(conf)) {
                    status.setText("Les mots de passe ne correspondent pas.");
                    return;
                }
                LocalAuth.register(authFile, user, pass);
                openMain(user);
            } else {
                LocalAuth a = LocalAuth.login(authFile, pass);
                if (a == null) { status.setText("Mot de passe incorrect."); return; }
                openMain(a.username);
            }
        } catch (Exception ex) {
            status.setText("Erreur : " + ex.getMessage());
        }
    }

    private void openMain(String user) {
        final String u = user;
        SwingUtilities.invokeLater(() -> {
            new MainFrame(u).setVisible(true);
            dispose();
        });
    }
}