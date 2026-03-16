package com.snippettray.ui;

import com.snippettray.model.SnippetResult;
import com.snippettray.model.Tag;
import com.snippettray.service.SnippetService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SearchWindow extends JDialog {
    private static final String ALL_TAGS = "All tags";
    private static final int DELETE_COLUMN_INDEX = 4;

    private final SnippetService service;
    private final JComboBox<String> tagFilter;
    private final List<Tag> tags = new ArrayList<>();
    private final JTextField searchField;
    private final JTable resultsTable;
    private final DefaultTableModel tableModel;
    private final JTextArea detailsArea;
    private List<SnippetResult> currentResults = new ArrayList<>();

    public SearchWindow(SnippetService service) {
        super((java.awt.Frame) null, "Snippet Search", true);
        this.service = service;
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

        tableModel = new DefaultTableModel(new Object[]{"Title", "Snippet", "Tags", "Created", "Delete"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        UiStyle.styleTable(resultsTable);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.getColumnModel().getColumn(DELETE_COLUMN_INDEX).setMinWidth(70);
        resultsTable.getColumnModel().getColumn(DELETE_COLUMN_INDEX).setMaxWidth(70);
        resultsTable.getColumnModel().getColumn(DELETE_COLUMN_INDEX).setPreferredWidth(70);

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
                if (row < 0 || column != DELETE_COLUMN_INDEX) {
                    return;
                }
                deleteSnippetAt(row);
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
                        "🗑"
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

    private static String preview(String content) {
        String singleLine = content.replace('\n', ' ').replace('\r', ' ').trim();
        if (singleLine.length() <= 90) {
            return singleLine;
        }
        return singleLine.substring(0, 90) + "…";
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
