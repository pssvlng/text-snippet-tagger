package com.snippettray.ui;

import com.snippettray.model.SnippetResult;
import com.snippettray.model.Tag;
import com.snippettray.service.SnippetService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SearchWindow extends JDialog {
    private static final String ALL_TAGS = "All tags";
    private static final int ACTIONS_COLUMN_INDEX = 4;
    private static final int ACTION_ICON_SIZE = 12;
    private static final int ACTION_ICON_GAP = 8;
    private static final int ACTION_ICON_PADDING = 4;
        private static final int ACTIONS_ICON_COUNT = 3;
        private static final int ACTIONS_ICON_WIDTH = ACTION_ICON_SIZE * ACTIONS_ICON_COUNT
            + ACTION_ICON_GAP * (ACTIONS_ICON_COUNT - 1)
            + ACTION_ICON_PADDING * 2;
    private static final int ACTIONS_ICON_HEIGHT = ACTION_ICON_SIZE + ACTION_ICON_PADDING * 2;
    private static final int ACTIONS_COLUMN_WIDTH = ACTIONS_ICON_WIDTH + 20;
    private static final Icon ACTIONS_ICON = createActionsIcon();

    private final SnippetService service;
    private JComboBox<String> tagFilter;
    private final List<Tag> tags = new ArrayList<>();
    private JTextField searchField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JTextArea detailsArea;
    private List<SnippetResult> currentResults = new ArrayList<>();

    public SearchWindow(SnippetService service) {
        this((Frame) null, service);
    }

    public SearchWindow(Frame parent, SnippetService service) {
        super(parent, "Snippet Search", true);
        this.service = service;
        initialize();
    }

    public SearchWindow(Dialog parent, SnippetService service) {
        super(parent, "Snippet Search", true);
        this.service = service;
        initialize();
    }

    public static SearchWindow create(Window parent, SnippetService service) {
        if (parent instanceof Dialog dialogParent) {
            return new SearchWindow(dialogParent, service);
        }
        return new SearchWindow((Frame) parent, service);
    }

    private void initialize() {
        setIconImage(AppAssets.appImage());
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(true);

        setLayout(new BorderLayout(10, 10));
        setSize(920, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        UiStyle.addPanelPadding((JPanel) getContentPane(), 12, 12, 12, 12);

        tagFilter = new JComboBox<>();
        searchField = new JTextField(28);
        UiStyle.styleTextField(searchField);

        JButton manageTagsButton = new JButton("Manage Tags");
        UiStyle.styleButton(manageTagsButton);
        manageTagsButton.addActionListener(event -> {
            AddTagDialog.showDialog(this, service);
            loadTags();
            refreshResults();
        });

        JButton addSnippetButton = new JButton("Add Text Snippet");
        UiStyle.styleButton(addSnippetButton);
        addSnippetButton.addActionListener(event -> {
            AddSnippetDialog.showDialog(this, service);
            loadTags();
            refreshResults();
        });

        JButton clearButton = new JButton("Clear");
        UiStyle.styleButton(clearButton);
        clearButton.addActionListener(event -> {
            tagFilter.setSelectedIndex(0);
            searchField.setText("");
            refreshResults();
        });

        JButton closeButton = new JButton("Close");
        UiStyle.styleButton(closeButton);
        closeButton.addActionListener(event -> setVisible(false));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("Tag:"));
        toolbar.add(tagFilter);
        toolbar.add(new JLabel("Title or text:"));
        toolbar.add(searchField);
        toolbar.add(clearButton);
        toolbar.add(manageTagsButton);
        toolbar.add(addSnippetButton);
        toolbar.add(closeButton);

        tableModel = new DefaultTableModel(new Object[]{"Title", "Snippet", "Tags", "Created", "Actions"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                if (row < 0) {
                    return null;
                }
                if (column == ACTIONS_COLUMN_INDEX) {
                    return switch (resolveActionAt(event)) {
                        case DELETE -> "Delete snippet";
                        case COPY -> "Copy snippet";
                        default -> "Edit snippet";
                    };
                }
                return null;
            }
        };
        UiStyle.styleTable(resultsTable);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.getColumnModel().getColumn(ACTIONS_COLUMN_INDEX).setMinWidth(ACTIONS_COLUMN_WIDTH);
        resultsTable.getColumnModel().getColumn(ACTIONS_COLUMN_INDEX).setMaxWidth(ACTIONS_COLUMN_WIDTH);
        resultsTable.getColumnModel().getColumn(ACTIONS_COLUMN_INDEX).setPreferredWidth(ACTIONS_COLUMN_WIDTH);
        DefaultTableCellRenderer actionsRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setIcon(ACTIONS_ICON);
                return label;
            }
        };
        resultsTable.getColumnModel().getColumn(ACTIONS_COLUMN_INDEX).setCellRenderer(actionsRenderer);

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        UiStyle.styleTextArea(detailsArea);

        JScrollPane tableScroll = new JScrollPane(resultsTable);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        UiStyle.styleScrollPane(tableScroll);
        UiStyle.styleScrollPane(detailsScroll);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailsScroll);
        splitPane.setDividerLocation(320);

        add(toolbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        installKeyboardSupport(manageTagsButton, addSnippetButton, clearButton, closeButton);

        tagFilter.addActionListener(event -> refreshResults());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshResults();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshResults();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshResults();
            }
        });

        resultsTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateDetails();
            }
        });

        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = resultsTable.rowAtPoint(event.getPoint());
                int column = resultsTable.columnAtPoint(event.getPoint());
                if (row < 0) {
                    return;
                }
                if (column == ACTIONS_COLUMN_INDEX) {
                    switch (resolveActionAt(event)) {
                        case DELETE -> deleteSnippetAt(row);
                        case COPY -> copySnippetAt(row);
                        default -> editSnippetAt(row);
                    }
                }
            }
        });

        loadTags();
        refreshResults();
    }

    public void open() {
        loadTags();
        refreshResults();
        setLocationRelativeTo(null);
        toFront();
        setVisible(true);
    }

    private void installKeyboardSupport(
            JButton manageTagsButton,
            JButton addSnippetButton,
            JButton clearButton,
            JButton closeButton
    ) {
        searchField.addActionListener(e -> refreshResults());

        List<Component> order = Arrays.asList(
                tagFilter,
                searchField,
                clearButton,
                manageTagsButton,
                addSnippetButton,
                closeButton,
                resultsTable,
                detailsArea
        );

        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new OrderedFocusTraversalPolicy(order));
    }

    private void loadTags() {
        try {
            tags.clear();
            tags.addAll(service.getTags());

            tagFilter.removeAllItems();
            tagFilter.addItem(ALL_TAGS);
            for (Tag tag : tags) {
                tagFilter.addItem(tag.name());
            }
            tagFilter.setSelectedIndex(0);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Failed to load tags", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private void refreshResults() {
        try {
            Integer selectedTagId = getSelectedTagId();
            String query = searchField.getText();
            currentResults = service.search(selectedTagId, query);

            tableModel.setRowCount(0);
            for (SnippetResult result : currentResults) {
                tableModel.addRow(new Object[]{
                    result.title() == null || result.title().isBlank() ? "(Untitled)" : result.title(),
                        preview(result.content()),
                        result.tagsCsv(),
                        result.createdAt(),
                        ""
                });
            }

            if (!currentResults.isEmpty()) {
                resultsTable.setRowSelectionInterval(0, 0);
            } else {
                detailsArea.setText("");
            }
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Search failed", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private Integer getSelectedTagId() {
        int index = tagFilter.getSelectedIndex();
        if (index <= 0) {
            return null;
        }
        return tags.get(index - 1).id();
    }

    private void updateDetails() {
        int row = resultsTable.getSelectedRow();
        if (row < 0 || row >= currentResults.size()) {
            detailsArea.setText("");
            return;
        }

        SnippetResult snippetResult = currentResults.get(row);
        detailsArea.setText(
            "Title: " + (snippetResult.title() == null || snippetResult.title().isBlank() ? "(Untitled)" : snippetResult.title()) + "\n"
                +
                "Tags: " + snippetResult.tagsCsv() + "\n"
                        + "Created: " + snippetResult.createdAt() + "\n\n"
                        + snippetResult.content()
        );
        detailsArea.setCaretPosition(0);
    }

    private void deleteSnippetAt(int row) {
        if (row < 0 || row >= currentResults.size()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this text snippet?",
                "Confirm Delete",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                AppAssets.appImageIcon()
        );

        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int snippetId = currentResults.get(row).id();
            service.deleteSnippet(snippetId);
            refreshResults();
            JOptionPane.showMessageDialog(this, "Text snippet deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE, AppAssets.appImageIcon());
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to delete snippet", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private void editSnippetAt(int row) {
        if (row < 0 || row >= currentResults.size()) {
            return;
        }

        try {
            SnippetResult snippetResult = currentResults.get(row);
            List<Integer> tagIds = service.getSnippetTagIds(snippetResult.id());
            AddSnippetDialog.showEditDialog(this, service, snippetResult, tagIds);
            loadTags();
            refreshResults();
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to edit snippet", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private static String preview(String content) {
        String singleLine = content.replace('\n', ' ').replace('\r', ' ').trim();
        if (singleLine.length() <= 90) {
            return singleLine;
        }
        return singleLine.substring(0, 90) + "…";
    }

    private ActionType resolveActionAt(MouseEvent event) {
        int row = resultsTable.rowAtPoint(event.getPoint());
        int column = resultsTable.columnAtPoint(event.getPoint());
        if (row < 0 || column != ACTIONS_COLUMN_INDEX) {
            return ActionType.NONE;
        }

        java.awt.Rectangle cell = resultsTable.getCellRect(row, column, false);
        int xInCell = event.getX() - cell.x;
        int iconsStartX = Math.max((cell.width - ACTIONS_ICON_WIDTH) / 2, 0);
        int editStartX = iconsStartX + ACTION_ICON_PADDING;
        int editEndX = editStartX + ACTION_ICON_SIZE;
        if (xInCell >= editStartX && xInCell <= editEndX) {
            return ActionType.EDIT;
        }

        int deleteStartX = editEndX + ACTION_ICON_GAP;
        int deleteEndX = deleteStartX + ACTION_ICON_SIZE;
        if (xInCell >= deleteStartX && xInCell <= deleteEndX) {
            return ActionType.DELETE;
        }

        int copyStartX = deleteEndX + ACTION_ICON_GAP;
        int copyEndX = copyStartX + ACTION_ICON_SIZE;
        if (xInCell >= copyStartX && xInCell <= copyEndX) {
            return ActionType.COPY;
        }

        return ActionType.NONE;
    }

    private void copySnippetAt(int row) {
        if (row < 0 || row >= currentResults.size()) {
            return;
        }

        try {
            String content = currentResults.get(row).content();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
            JOptionPane.showMessageDialog(
                    this,
                    "Text snippet copied to clipboard.",
                    "Copied",
                    JOptionPane.INFORMATION_MESSAGE,
                    AppAssets.appImageIcon()
            );
        } catch (IllegalStateException | SecurityException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to copy text snippet to clipboard.",
                    "Copy Failed",
                    JOptionPane.ERROR_MESSAGE,
                    AppAssets.appImageIcon()
            );
        }
    }

    private static Icon createActionsIcon() {
        BufferedImage image = new BufferedImage(ACTIONS_ICON_WIDTH, ACTIONS_ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int editX = ACTION_ICON_PADDING;
        int iconTop = ACTION_ICON_PADDING;
        drawEditIcon(graphics, editX, iconTop);

        int deleteX = ACTION_ICON_PADDING + ACTION_ICON_SIZE + ACTION_ICON_GAP;
        drawDeleteIcon(graphics, deleteX, iconTop);

        int copyX = deleteX + ACTION_ICON_SIZE + ACTION_ICON_GAP;
        drawCopyIcon(graphics, copyX, iconTop);

        graphics.dispose();
        return new ImageIcon(image);
    }

    private static void drawEditIcon(Graphics2D graphics, int x, int y) {
        graphics.setColor(new Color(76, 90, 106));
        graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x + 2, y + ACTION_ICON_SIZE - 3, x + ACTION_ICON_SIZE - 4, y + 3);

        int[] tipX = {x + ACTION_ICON_SIZE - 5, x + ACTION_ICON_SIZE - 2, x + ACTION_ICON_SIZE - 4};
        int[] tipY = {y + 2, y + 4, y + 6};
        graphics.fillPolygon(tipX, tipY, 3);

        graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x + 2, y + ACTION_ICON_SIZE - 2, x + 5, y + ACTION_ICON_SIZE - 2);
    }

    private static void drawDeleteIcon(Graphics2D graphics, int x, int y) {
        graphics.setColor(new Color(138, 83, 83));
        graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawRoundRect(x + 3, y + 4, ACTION_ICON_SIZE - 6, ACTION_ICON_SIZE - 4, 2, 2);
        graphics.drawLine(x + 2, y + 4, x + ACTION_ICON_SIZE - 2, y + 4);
        graphics.drawLine(x + 5, y + 2, x + ACTION_ICON_SIZE - 5, y + 2);
        graphics.drawLine(x + 6, y + 6, x + 6, y + ACTION_ICON_SIZE - 2);
        graphics.drawLine(x + ACTION_ICON_SIZE - 6, y + 6, x + ACTION_ICON_SIZE - 6, y + ACTION_ICON_SIZE - 2);
    }

    private static void drawCopyIcon(Graphics2D graphics, int x, int y) {
        graphics.setColor(new Color(74, 98, 140));
        graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawRoundRect(x + 2, y + 4, ACTION_ICON_SIZE - 6, ACTION_ICON_SIZE - 6, 2, 2);
        graphics.drawRoundRect(x + 4, y + 2, ACTION_ICON_SIZE - 6, ACTION_ICON_SIZE - 6, 2, 2);
    }

    private enum ActionType {
        NONE,
        EDIT,
        DELETE,
        COPY
    }

    private static final class OrderedFocusTraversalPolicy extends FocusTraversalPolicy {
        private final List<Component> order;

        private OrderedFocusTraversalPolicy(List<Component> order) {
            this.order = new ArrayList<>(order);
        }

        @Override
        public Component getComponentAfter(Container aContainer, Component aComponent) {
            int index = order.indexOf(aComponent);
            if (index < 0) {
                return getDefaultComponent(aContainer);
            }
            return order.get((index + 1) % order.size());
        }

        @Override
        public Component getComponentBefore(Container aContainer, Component aComponent) {
            int index = order.indexOf(aComponent);
            if (index < 0) {
                return getDefaultComponent(aContainer);
            }
            return order.get((index - 1 + order.size()) % order.size());
        }

        @Override
        public Component getFirstComponent(Container aContainer) {
            return order.get(0);
        }

        @Override
        public Component getLastComponent(Container aContainer) {
            return order.get(order.size() - 1);
        }

        @Override
        public Component getDefaultComponent(Container aContainer) {
            return order.get(0);
        }
    }
}
