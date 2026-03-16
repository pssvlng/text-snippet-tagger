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
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AddTagDialog extends JDialog {
    private static AddTagDialog instance;

    private final SnippetService service;
    private JTextField tagField;
    private DefaultListModel<Tag> tagsModel;
    private JList<Tag> tagsList;

    private AddTagDialog(Frame parent, SnippetService service) {
        super(parent, "Manage Tags", true);
        this.service = service;
        initialize();
    }

    private AddTagDialog(Dialog parent, SnippetService service) {
        super(parent, "Manage Tags", true);
        this.service = service;
        initialize();
    }

    private void initialize() {

        setIconImage(AppAssets.appImage());
        setLayout(new BorderLayout(10, 10));
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(true);
        UiStyle.addPanelPadding((JPanel) getContentPane(), 12, 12, 12, 12);

        tagField = new JTextField(28);
        UiStyle.styleTextField(tagField);

        JButton addButton = new JButton("Add Tag");
        UiStyle.styleButton(addButton);
        addButton.addActionListener(e -> addTag());
        tagField.addActionListener(e -> addTag());

        JPanel addPanel = new JPanel(new BorderLayout(8, 8));
        addPanel.add(new JLabel("New tag name:"), BorderLayout.NORTH);
        addPanel.add(tagField, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.EAST);

        tagsModel = new DefaultListModel<>();
        tagsList = new JList<>(tagsModel);
        tagsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        UiStyle.styleList(tagsList);

        JScrollPane listScroll = new JScrollPane(tagsList);
        UiStyle.styleScrollPane(listScroll);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(new JLabel("Existing tags:"), BorderLayout.NORTH);
        centerPanel.add(listScroll, BorderLayout.CENTER);

        JButton deleteButton = new JButton("Delete Selected");
        UiStyle.styleButton(deleteButton);
        deleteButton.addActionListener(e -> deleteSelectedTags());

        JButton closeButton = new JButton("Close");
        UiStyle.styleButton(closeButton);
        closeButton.addActionListener(e -> setVisible(false));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(deleteButton);
        actions.add(closeButton);

        add(addPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        installKeyboardSupport(addButton, deleteButton, closeButton);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(500, 440);
        setLocationRelativeTo(getOwner());
        refreshTags();
    }

    public static void showDialog(Window parent, SnippetService service) {
        Window currentOwner = instance == null ? null : instance.getOwner();
        if (instance == null || currentOwner != parent) {
            if (instance != null) {
                instance.dispose();
            }

            if (parent instanceof Dialog dialogParent) {
                instance = new AddTagDialog(dialogParent, service);
            } else {
                instance = new AddTagDialog((Frame) parent, service);
            }
        }

        instance.refreshTags();
        instance.tagField.setText("");
        instance.setLocationRelativeTo(parent);
        instance.toFront();
        instance.setVisible(true);
        instance.tagField.requestFocusInWindow();
    }

    private void installKeyboardSupport(JButton addButton, JButton deleteButton, JButton closeButton) {
        getRootPane().setDefaultButton(addButton);

        List<Component> order = Arrays.asList(
                tagField,
                addButton,
                tagsList,
                deleteButton,
                closeButton
        );

        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new OrderedFocusTraversalPolicy(order));
    }

    private void addTag() {
        try {
            String tagName = tagField.getText();
            service.addTag(tagName);
            tagField.setText("");
            refreshTags();
            JOptionPane.showMessageDialog(this, "Tag saved.", "Success", JOptionPane.INFORMATION_MESSAGE, AppAssets.appImageIcon());
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to save tag", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
    }

    private void deleteSelectedTags() {
        List<Tag> selectedTags = tagsList.getSelectedValuesList();
        if (selectedTags.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one tag to delete.", "No Selection", JOptionPane.WARNING_MESSAGE, AppAssets.appImageIcon());
            return;
        }

        int usedBySnippets = 0;
        List<String> usedTagNames = new ArrayList<>();

        try {
            for (Tag selectedTag : selectedTags) {
                int usageCount = service.countSnippetsUsingTag(selectedTag.id());
                if (usageCount > 0) {
                    usedBySnippets += usageCount;
                    usedTagNames.add(selectedTag.name());
                }
            }

            String confirmationMessage;
            if (usedTagNames.isEmpty()) {
                confirmationMessage = "Delete the selected tags?";
            } else {
                confirmationMessage = "Some selected tags are used by existing text snippets:\n\n"
                        + String.join(", ", usedTagNames)
                        + "\n\nLinked snippets will remain, but these tags and their links will be removed.\nContinue?";
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    confirmationMessage,
                    "Confirm Delete",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    AppAssets.appImageIcon()
            );

            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }

            for (Tag selectedTag : selectedTags) {
                service.deleteTag(selectedTag.id());
            }

            refreshTags();

            String resultMessage = usedBySnippets > 0
                    ? "Tags deleted. Existing snippets were kept."
                    : "Tags deleted.";

            JOptionPane.showMessageDialog(this, resultMessage, "Done", JOptionPane.INFORMATION_MESSAGE, AppAssets.appImageIcon());
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to delete tags", JOptionPane.ERROR_MESSAGE, AppAssets.appImageIcon());
        }
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
