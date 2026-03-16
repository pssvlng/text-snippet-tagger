package com.snippettray.tray;

import com.snippettray.service.SnippetService;
import com.snippettray.ui.AddSnippetDialog;
import com.snippettray.ui.AddTagDialog;
import com.snippettray.ui.AppAssets;
import com.snippettray.ui.SearchWindow;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class TrayApp {
    private final SnippetService service;
    private SearchWindow searchWindow;
    private TrayIcon trayIcon;
    private JDialog databaseLocationDialog;
    private JDialog aboutDialog;

    public TrayApp(SnippetService service) {
        this.service = service;
    }

    public void start() throws AWTException {
        if (!SystemTray.isSupported()) {
            throw new IllegalStateException("System tray is not supported on this desktop environment.");
        }

        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu menu = new PopupMenu();

        MenuItem addTag = new MenuItem("Add Tag");
        MenuItem addSnippet = new MenuItem("Add Text Snippet");
        MenuItem search = new MenuItem("Search Snippets");
        MenuItem dbLocation = new MenuItem("Show Database Location");
        MenuItem about = new MenuItem("About");
        MenuItem exit = new MenuItem("Exit");

        addTag.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            requestForegroundIfNeeded();
            AddTagDialog.showDialog(null, service);
        }));
        addSnippet.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            requestForegroundIfNeeded();
            AddSnippetDialog.showDialog(null, service);
        }));
        search.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            requestForegroundIfNeeded();
            showSearchWindow();
        }));
        dbLocation.addActionListener(event -> SwingUtilities.invokeLater(this::showDatabaseLocationDialog));
        about.addActionListener(event -> SwingUtilities.invokeLater(this::showAboutDialog));
        exit.addActionListener(event -> shutdown());

        menu.add(addTag);
        menu.add(addSnippet);
        menu.add(search);
        menu.addSeparator();
        menu.add(dbLocation);
        menu.add(about);
        menu.addSeparator();
        menu.add(exit);

        trayIcon = new TrayIcon(TrayIconFactory.createIconImage(tray.getTrayIconSize().width), "Snippet Tray Manager", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            requestForegroundIfNeeded();
            showSearchWindow();
        }));

        tray.add(trayIcon);
        trayIcon.displayMessage("Snippet Tray Manager", "Running in system tray.", TrayIcon.MessageType.INFO);
    }

    private void showSearchWindow() {
        if (searchWindow == null) {
            searchWindow = new SearchWindow(service);
        }

        searchWindow.open();
    }

    private void showDatabaseLocationDialog() {
        if (databaseLocationDialog != null && databaseLocationDialog.isShowing()) {
            databaseLocationDialog.toFront();
            databaseLocationDialog.requestFocus();
            return;
        }

        String message = "Database file:\n" + service.getDatabasePath();
        JOptionPane optionPane = new JOptionPane(
                message,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                AppAssets.appImageIcon()
        );

        databaseLocationDialog = optionPane.createDialog(null, "Database Location");
        databaseLocationDialog.setModal(true);
        databaseLocationDialog.setAlwaysOnTop(true);
        databaseLocationDialog.setIconImage(AppAssets.appImage());
        databaseLocationDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                databaseLocationDialog = null;
            }

            @Override
            public void windowClosing(WindowEvent e) {
                databaseLocationDialog = null;
            }
        });
        databaseLocationDialog.setVisible(true);
    }

    private void showAboutDialog() {
        if (aboutDialog != null && aboutDialog.isShowing()) {
            aboutDialog.toFront();
            aboutDialog.requestFocus();
            return;
        }

        String message = "Snippet Tray Manager\n\n"
                + "Save and organize text snippets with tags from your system tray.\n"
                + "Search by tag or text, and manage snippets and tags quickly.";

        JOptionPane optionPane = new JOptionPane(
                message,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                AppAssets.appImageIcon()
        );

        aboutDialog = optionPane.createDialog(null, "About");
        aboutDialog.setModal(true);
        aboutDialog.setAlwaysOnTop(true);
        aboutDialog.setIconImage(AppAssets.appImage());
        aboutDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                aboutDialog = null;
            }

            @Override
            public void windowClosing(WindowEvent e) {
                aboutDialog = null;
            }
        });
        aboutDialog.setVisible(true);
    }

    private static void requestForegroundIfNeeded() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            return;
        }

        try {
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.getMethod("getApplication").invoke(null);
            appClass.getMethod("requestForeground", boolean.class).invoke(app, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void shutdown() {
        if (searchWindow != null) {
            searchWindow.dispose();
        }
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        service.shutdown();
        System.exit(0);
    }
}
