package com.snippettray.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public final class UiStyle {
    private static final Color BORDER_COLOR = new Color(196, 204, 214);
    private static final Color INPUT_BACKGROUND = new Color(255, 255, 255);
    private static final Color BUTTON_BACKGROUND = new Color(246, 248, 251);
    private static final Color TABLE_GRID = new Color(230, 234, 240);
    private static final Color TEXT_COLOR = new Color(33, 37, 41);
    private static final Color MUTED_TEXT_COLOR = new Color(64, 72, 84);
    private static final Color SELECTION_BACKGROUND = new Color(44, 108, 194);
    private static final Color SELECTION_TEXT = new Color(255, 255, 255);
    private static final int BUTTON_HEIGHT = 34;

    private UiStyle() {
    }

    public static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
        }
        applyGlobalFont();
    }

    private static void applyGlobalFont() {
        FontUIResource fontResource = new FontUIResource(resolvePreferredUiFont());
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontResource);
            }
        }
    }

    private static Font resolvePreferredUiFont() {
        String[] preferredFamilies = {
                "Inter",
                "SF Pro Text",
                "Segoe UI",
                "Helvetica Neue",
                "Noto Sans",
                "Roboto",
                "Arial",
                "SansSerif"
        };

        Set<String> availableFamilies = new HashSet<>();
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            availableFamilies.add(family);
        }

        for (String preferredFamily : preferredFamilies) {
            if (availableFamilies.contains(preferredFamily)) {
                return new Font(preferredFamily, Font.PLAIN, 13);
            }
        }

        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    public static Border paddedLineBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)
        );
    }

    public static void styleTextField(JTextField textField) {
        textField.setBorder(paddedLineBorder());
        textField.setBackground(INPUT_BACKGROUND);
        textField.setForeground(TEXT_COLOR);
        textField.setCaretColor(TEXT_COLOR);
        textField.setSelectionColor(SELECTION_BACKGROUND);
        textField.setSelectedTextColor(SELECTION_TEXT);
    }

    public static void styleTextArea(JTextArea textArea) {
        textArea.setBorder(paddedLineBorder());
        textArea.setBackground(INPUT_BACKGROUND);
        textArea.setForeground(TEXT_COLOR);
        textArea.setCaretColor(TEXT_COLOR);
        textArea.setSelectionColor(SELECTION_BACKGROUND);
        textArea.setSelectedTextColor(SELECTION_TEXT);
        textArea.setMargin(new Insets(4, 4, 4, 4));
    }

    public static void styleButton(AbstractButton button) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(MUTED_TEXT_COLOR);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setMargin(new Insets(7, 12, 7, 12));

        Dimension preferred = button.getPreferredSize();
        button.setPreferredSize(new Dimension(preferred.width, BUTTON_HEIGHT));
    }

    public static void styleList(JList<?> list) {
        list.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        list.setBackground(INPUT_BACKGROUND);
        list.setForeground(TEXT_COLOR);
        list.setSelectionBackground(SELECTION_BACKGROUND);
        list.setSelectionForeground(SELECTION_TEXT);
        list.setFixedCellHeight(24);
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(TABLE_GRID);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setBackground(INPUT_BACKGROUND);
        table.setForeground(TEXT_COLOR);
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(SELECTION_TEXT);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
    }

    public static void addPanelPadding(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }
}
