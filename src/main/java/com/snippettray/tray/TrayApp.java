package com.snippettray.tray;

import com.snippettray.service.SnippetService;
import com.snippettray.ui.AddSnippetDialog;
import com.snippettray.ui.AddTagDialog;
import com.snippettray.ui.AppAssets;
import com.snippettray.ui.SearchWindow;
import com.snippettray.ui.UiStyle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class TrayApp {
    private final SnippetService service;
    private SearchWindow searchWindow;
    private TrayIcon trayIcon;
    private JFrame fallbackFrame;
    private JDialog databaseLocationDialog;
    private JDialog aboutDialog;

    public TrayApp(SnippetService service) {
        this.service = service;
    }

    public void start() throws AWTException {
        if (!SystemTray.isSupported()) {
            startFallbackMode();
            return;
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
            AddTagDialog.showDialog(resolveDialogOwner(), service);
        }));
        addSnippet.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            requestForegroundIfNeeded();
            AddSnippetDialog.showDialog(resolveDialogOwner(), service);
        }));
        search.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            requestForegroundIfNeeded();
            showSearchWindow(resolveDialogOwner());
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
            showSearchWindow(resolveDialogOwner());
        }));

        tray.add(trayIcon);
        trayIcon.displayMessage("Snippet Tray Manager", "Running in system tray.", TrayIcon.MessageType.INFO);
    }

    private void startFallbackMode() {
        if (fallbackFrame != null && fallbackFrame.isDisplayable()) {
            fallbackFrame.setVisible(true);
            fallbackFrame.toFront();
            return;
        }

        fallbackFrame = new JFrame("Snippet Tray Manager");
        fallbackFrame.setIconImage(AppAssets.appImage());
        fallbackFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        fallbackFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        UiStyle.addPanelPadding(root, 12, 12, 12, 12);

        JLabel infoLabel = new JLabel("System tray is unavailable. Running in window mode.");
        root.add(infoLabel, BorderLayout.NORTH);

        JPanel actionsPanel = new JPanel(new GridLayout(0, 2, 8, 8));

        JButton addTagButton = new JButton("Add Tag");
        UiStyle.styleButton(addTagButton);
        addTagButton.addActionListener(event -> AddTagDialog.showDialog(fallbackFrame, service));

        JButton addSnippetButton = new JButton("Add Text Snippet");
        UiStyle.styleButton(addSnippetButton);
        addSnippetButton.addActionListener(event -> AddSnippetDialog.showDialog(fallbackFrame, service));

        JButton searchButton = new JButton("Search Snippets");
        UiStyle.styleButton(searchButton);
        searchButton.addActionListener(event -> showSearchWindow(resolveDialogOwner()));

        JButton dbLocationButton = new JButton("Show Database Location");
        UiStyle.styleButton(dbLocationButton);
        dbLocationButton.addActionListener(event -> showDatabaseLocationDialog());

        JButton aboutButton = new JButton("About");
        UiStyle.styleButton(aboutButton);
        aboutButton.addActionListener(event -> showAboutDialog());

        JButton exitButton = new JButton("Exit");
        UiStyle.styleButton(exitButton);
        exitButton.addActionListener(event -> shutdown());

        actionsPanel.add(addTagButton);
        actionsPanel.add(addSnippetButton);
        actionsPanel.add(searchButton);
        actionsPanel.add(dbLocationButton);
        actionsPanel.add(aboutButton);
        actionsPanel.add(exitButton);

        root.add(actionsPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(new JLabel("Tip: Enable AppIndicator extension for tray support on GNOME if desired."));
        root.add(footer, BorderLayout.SOUTH);

        fallbackFrame.setContentPane(root);
        fallbackFrame.pack();
        fallbackFrame.setResizable(false);
        fallbackFrame.setLocationRelativeTo(null);
        fallbackFrame.setVisible(true);
    }

    private void showSearchWindow(Window owner) {
        Window currentOwner = searchWindow == null ? null : searchWindow.getOwner();
        if (searchWindow == null || currentOwner != owner) {
            if (searchWindow != null) {
                searchWindow.dispose();
            }
            searchWindow = SearchWindow.create(owner, service);
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

        Window owner = resolveDialogOwner();
        databaseLocationDialog = optionPane.createDialog(owner, "Database Location");
        databaseLocationDialog.setModal(true);
        databaseLocationDialog.setAlwaysOnTop(true);
        databaseLocationDialog.setIconImage(AppAssets.appImage());
        databaseLocationDialog.setLocationRelativeTo(owner);
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
            + "Search by tag or text, and manage snippets and tags quickly.\n\n"
            + "All iconography and visual elements are handcrafted or AI-generated.";

        JOptionPane optionPane = new JOptionPane(
                message,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                AppAssets.appImageIcon()
        );

        Window owner = resolveDialogOwner();
        aboutDialog = optionPane.createDialog(owner, "About");
        aboutDialog.setModal(true);
        aboutDialog.setAlwaysOnTop(true);
        aboutDialog.setIconImage(AppAssets.appImage());
        aboutDialog.setLocationRelativeTo(owner);
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

    private Window resolveDialogOwner() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (isAppWindow(activeWindow)) {
            return activeWindow;
        }

        if (searchWindow != null && searchWindow.isShowing()) {
            return searchWindow;
        }
        if (fallbackFrame != null && fallbackFrame.isShowing()) {
            return fallbackFrame;
        }

        for (Window window : Window.getWindows()) {
            if (isAppWindow(window)) {
                return window;
            }
        }

        return null;
    }

    private boolean isAppWindow(Window window) {
        if (window == null || !window.isShowing()) {
            return false;
        }
        return window == fallbackFrame
                || window == searchWindow
                || window == aboutDialog
                || window == databaseLocationDialog
                || window instanceof AddTagDialog
                || window instanceof AddSnippetDialog
                || window instanceof SearchWindow;
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
        if (fallbackFrame != null) {
            fallbackFrame.dispose();
        }
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        service.shutdown();
        System.exit(0);
    }
}
