package com.snippettray;

import com.snippettray.db.DatabaseManager;
import com.snippettray.repo.AppRepository;
import com.snippettray.service.SnippetService;
import com.snippettray.tray.TrayApp;
import com.snippettray.ui.UiStyle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            System.setProperty("apple.awt.UIElement", "true");
        }

        UiStyle.configureLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager databaseManager = DatabaseManager.initialize();
                AppRepository repository = new AppRepository(databaseManager.getConnection());
                SnippetService service = new SnippetService(repository, databaseManager);
                TrayApp trayApp = new TrayApp(service);
                trayApp.start();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to start Snippet Tray Manager:\n" + exception.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
    }
}
