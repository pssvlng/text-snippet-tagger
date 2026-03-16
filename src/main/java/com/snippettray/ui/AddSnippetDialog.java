package com.snippettray.ui;

import com.snippettray.model.Tag;
import com.snippettray.service.SnippetService;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AddSnippetDialog extends JDialog {
    private static AddSnippetDialog instance;

    private final SnippetService service;
    private JTextField titleField;
    private JTextArea snippetArea;
    private JList<Tag> tagsList;
    private DefaultListModel<Tag> tagsModel;

    private AddSnippetDialog(Frame parent, SnippetService service) {
        super(parent, "Add Text Snippet", true);
        this.service = service;
        initialize();
    }

    private AddSnippetDialog(Dialog parent, SnippetService service) {
        super(parent, "Add Text Snippet", true);
        this.service = service;
        initialize();
    }

    private void initialize() {

        setLayout(new BorderLayout(10, 10));
        setIconImage(AppAssets.appImage());
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(true);
        UiStyle.addPanelPadding((JPanel) getContentPane(), 12, 12, 12, 12);

        titleField = new JTextField(50);
        UiStyle.styleTextField(titleField);

        snippetArea = new JTextArea(10, 50);
        UiStyle.styleTextArea(snippetArea);
        JScrollPane snippetScroll = new JScrollPane(snippetArea);
        UiStyle.styleScrollPane(snippetScroll);

        tagsModel = new DefaultListModel<>();
        tagsList = new JList<>(tagsModel);
        tagsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        UiStyle.styleList(tagsList);

        JScrollPane tagsScroll = new JScrollPane(tagsList);
        UiStyle.styleScrollPane(tagsScroll);
        tagsScroll.setPreferredSize(new Dimension(220, 200));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel titlePanel = new JPanel(new BorderLayout(6, 6));
        titlePanel.add(new JLabel("Snippet title (optional):"), BorderLayout.NORTH);
        titlePanel.add(titleField, BorderLayout.CENTER);

        JPanel centerTop = new JPanel(new BorderLayout(8, 8));
        centerTop.add(titlePanel, BorderLayout.NORTH);
        centerTop.add(new JLabel("Snippet text:"), BorderLayout.SOUTH);

        center.add(centerTop, BorderLayout.NORTH);
        center.add(snippetScroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(new JLabel("Attach tags:"), BorderLayout.NORTH);
        right.add(tagsScroll, BorderLayout.CENTER);

        JButton newTagButton = new JButton("New Tag");
        UiStyle.styleButton(newTagButton);
        newTagButton.addActionListener(e -> createTagInline());
        JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightFooter.add(newTagButton);
        right.add(rightFooter, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(center, BorderLayout.CENTER);
        content.add(right, BorderLayout.EAST);

        JButton saveButton = new JButton("Save Snippet");
        JButton cancelButton = new JButton("Close");
        UiStyle.styleButton(saveButton);
        UiStyle.styleButton(cancelButton);
        saveButton.addActionListener(e -> saveSnippet());
        cancelButton.addActionListener(e -> setVisible(false));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(cancelButton);
        actions.add(saveButton);

        add(content, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        installKeyboardSupport(newTagButton, saveButton, cancelButton);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        refreshTags();
        pack();
        setLocationRelativeTo(getOwner());
    }

    public static void showDialog(Window parent, SnippetService service) {
        Window currentOwner = instance == null ? null : instance.getOwner();
        if (instance == null || currentOwner != parent) {
            if (instance != null) {
                instance.dispose();
            }

            if (parent instanceof Dialog dialogParent) {
                instance = new AddSnippetDialog(dialogParent, service);
            } else {
                instance = new AddSnippetDialog((Frame) parent, service);
            }
        }

        instance.refreshTags();
        instance.titleField.setText("");
        instance.snippetArea.setText("");
        instance.tagsList.clearSelection();
        instance.titleField.requestFocusInWindow();
        instance.setLocationRelativeTo(parent);
        instance.toFront();
        instance.setVisible(true);
    }

    private void installKeyboardSupport(JButton newTagButton, JButton saveButton, JButton closeButton) {
        getRootPane().setDefaultButton(saveButton);
        titleField.addActionListener(e -> saveSnippet());
        snippetArea.setFocusTraversalKeysEnabled(true);

        List<Component> order = Arrays.asList(
                titleField,
                snippetArea,
                tagsList,
                newTagButton,
                saveButton,
                closeButton
        );

        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new OrderedFocusTraversalPolicy(order));
    }

    private void refreshTags() {
        try {
            List<Tag> tags = service.getTags();
            tagsModel.clear();
            for (Tag tag : tags) {
                tagsModel.addElement(tag);
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Failed to load tags", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private void createTagInline() {
        String value = (String) JOptionPane.showInputDialog(
                this,
                "Tag name:",
                "Create Tag",
                JOptionPane.PLAIN_MESSAGE,
                AppAssets.appImageIcon(),
                null,
                ""
        );
        if (value == null) {
            return;
        }

        try {
            int createdTagId = service.addTag(value);
            refreshTags();
            for (int i = 0; i < tagsModel.size(); i++) {
                if (tagsModel.get(i).id() == createdTagId) {
                    tagsList.addSelectionInterval(i, i);
                    break;
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to create tag", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private void saveSnippet() {
        String title = titleField.getText();
        String content = snippetArea.getText();

        List<Integer> selectedTagIds = new ArrayList<>();
        for (Tag tag : tagsList.getSelectedValuesList()) {
            selectedTagIds.add(tag.id());
        }

        try {
            service.addSnippet(title, content, selectedTagIds);
            JOptionPane.showMessageDialog(this, "Text snippet saved.", "Success", JOptionPane.INFORMATION_MESSAGE, AppAssets.appImageIcon());
            titleField.setText("");
            snippetArea.setText("");
            tagsList.clearSelection();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to save snippet", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
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
