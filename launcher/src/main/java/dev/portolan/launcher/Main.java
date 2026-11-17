package dev.portolan.launcher;

import com.formdev.flatlaf.FlatDarculaLaf;
import dev.portolan.launcher.core.LocalAuth;
import dev.portolan.launcher.ui.LoginFrame;
import dev.portolan.launcher.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Paths;

public final class Main {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarculaLaf()); }
        catch (Exception ignored) {}
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.width", 10);

        LocalAuth auth = LocalAuth.load(Paths.get(System.getProperty("user.home"),
                ".portolan", "auth.json"));

        if (auth != null && auth.hasRemembered()) {
            SwingUtilities.invokeLater(() -> new MainFrame(auth.username).setVisible(true));
        } else {
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}