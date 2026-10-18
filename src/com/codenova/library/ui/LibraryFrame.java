package com.codenova.library.ui;

import com.codenova.library.exception.LibraryException;
import com.codenova.library.exception.StorageException;
import com.codenova.library.model.*;
import com.codenova.library.service.LibraryService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/** UI layer: Swing desktop window with tabs. It holds no business rules, it only calls LibraryService. */
public class LibraryFrame extends JFrame {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Set<String> NUMERIC = Set.of("ID", "Loan ID", "Total", "Available", "Borrowed now",
            "Days overdue", "Fine so far (Rs.)", "Fine (Rs.)");

    private final LibraryService service;

    // ---- table models ----
    private final DefaultTableModel itemModel = model("ID", "Type", "Title", "Author / Creator", "Details", "Total", "Available");
    private final DefaultTableModel memberModel = model("ID", "Card No", "Name", "Email", "Phone", "Registered", "Borrowed now");
    private final DefaultTableModel activeModel = model("Loan ID", "Member", "Item", "Borrowed", "Due", "Status");
    private final DefaultTableModel overdueModel = model("Loan ID", "Member", "Card No", "Item", "Due", "Days overdue", "Fine so far (Rs.)");
    private final DefaultTableModel historyModel = model("Loan ID", "Member", "Item", "Borrowed", "Due", "Returned", "Fine (Rs.)", "Status");

    private final JTable itemTable = table(itemModel);
    private final JTable memberTable = table(memberModel);
    private final JTable activeTable = table(activeModel);
    private final JTable overdueTable = table(overdueModel);
    private final JTable historyTable = table(historyModel);

    // ---- catalog form ----
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"Physical Book", "Digital Media"});
    private final JTextField titleField = new JTextField(14);
    private final JTextField creatorField = new JTextField(14);
    private final JTextField copiesField = new JTextField("1", 4);
    private final JTextField extra1Field = new JTextField(12);
    private final JTextField extra2Field = new JTextField(10);
    private final JLabel extra1Label = new JLabel();
    private final JLabel extra2Label = new JLabel();

    // ---- member form ----
    private final JTextField mNameField = new JTextField(14);
    private final JTextField mEmailField = new JTextField(18);
    private final JTextField mPhoneField = new JTextField(12);

    // ---- circulation ----
    private final JComboBox<Option> memberBox = new JComboBox<>();
    private final JComboBox<Option> itemBox = new JComboBox<>();

    private final JLabel finesLabel = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();

    public LibraryFrame(LibraryService service) {
        super("Library Management System");
        this.service = service;
        buildUi();
        refreshAll();
        setStatus("Ready.");
    }

    // ===================================================== UI construction =====================================================

    private void buildUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 660);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 6));

        widths(itemTable, 40, 100, 240, 170, 230, 50, 70);
        widths(memberTable, 40, 80, 140, 230, 100, 100, 90);
        widths(activeTable, 60, 140, 250, 100, 100, 160);
        widths(overdueTable, 60, 140, 80, 250, 100, 90, 130);
        widths(historyTable, 60, 130, 230, 100, 100, 100, 80, 110);

        tabs.addTab("Catalog", catalogPanel());
        tabs.addTab("Members", membersPanel());
        tabs.addTab("Borrow / Return", circulationPanel());
        tabs.addTab("Overdue & Fines", overduePanel());
        tabs.addTab("Loan History", historyPanel());
        tabs.addChangeListener(e -> refreshAll());
        add(tabs, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(2, 10, 6, 10));
        add(status, BorderLayout.SOUTH);
    }

    private JPanel catalogPanel() {
        JPanel p = pad(new JPanel(new BorderLayout(8, 8)));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(new JLabel("Search (title, author, ISBN):"), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> searchField.setText(""));
        top.add(clear, BorderLayout.EAST);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshItems(); }
            public void removeUpdate(DocumentEvent e) { refreshItems(); }
            public void changedUpdate(DocumentEvent e) { refreshItems(); }
        });
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(itemTable), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add or remove a catalog item"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        place(form, g, 0, 0, new JLabel("Type"), 0);
        place(form, g, 1, 0, typeBox, 0);
        place(form, g, 2, 0, new JLabel("Title"), 0);
        place(form, g, 3, 0, titleField, 1);
        place(form, g, 4, 0, new JLabel("Author / Creator"), 0);
        place(form, g, 5, 0, creatorField, 1);
        place(form, g, 0, 1, new JLabel("Copies"), 0);
        place(form, g, 1, 1, copiesField, 0);
        place(form, g, 2, 1, extra1Label, 0);
        place(form, g, 3, 1, extra1Field, 1);
        place(form, g, 4, 1, extra2Label, 0);
        place(form, g, 5, 1, extra2Field, 1);
        JButton add = new JButton("Add item");
        JButton remove = new JButton("Remove selected");
        add.addActionListener(e -> onAddItem());
        remove.addActionListener(e -> onRemoveItem());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(add);
        btns.add(remove);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 6; g.weightx = 0;
        form.add(btns, g);
        typeBox.addActionListener(e -> updateExtraLabels());
        updateExtraLabels();
        p.add(form, BorderLayout.SOUTH);
        return p;
    }

    private JPanel membersPanel() {
        JPanel p = pad(new JPanel(new BorderLayout(8, 8)));
        p.add(new JScrollPane(memberTable), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Register a member (a library card is issued automatically)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        place(form, g, 0, 0, new JLabel("Name"), 0);
        place(form, g, 1, 0, mNameField, 1);
        place(form, g, 2, 0, new JLabel("Email"), 0);
        place(form, g, 3, 0, mEmailField, 1);
        place(form, g, 4, 0, new JLabel("Phone (10 digits)"), 0);
        place(form, g, 5, 0, mPhoneField, 1);
        JButton reg = new JButton("Register & issue card");
        JButton remove = new JButton("Remove selected");
        reg.addActionListener(e -> onRegisterMember());
        remove.addActionListener(e -> onRemoveMember());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(reg);
        btns.add(remove);
        g.gridx = 0; g.gridy = 1; g.gridwidth = 6; g.weightx = 0;
        form.add(btns, g);
        p.add(form, BorderLayout.SOUTH);
        return p;
    }

    private JPanel circulationPanel() {
        JPanel p = pad(new JPanel(new BorderLayout(8, 8)));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Issue an item"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        place(form, g, 0, 0, new JLabel("Member"), 0);
        place(form, g, 1, 0, memberBox, 1);
        place(form, g, 2, 0, new JLabel("Item"), 0);
        place(form, g, 3, 0, itemBox, 1);
        JButton borrow = new JButton("Borrow");
        borrow.addActionListener(e -> onBorrow());
        place(form, g, 4, 0, borrow, 0);
        p.add(form, BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(0, 6));
        centre.setBorder(BorderFactory.createTitledBorder("Items currently borrowed"));
        centre.add(new JScrollPane(activeTable), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton ret = new JButton("Return selected");
        ret.addActionListener(e -> onReturn());
        bottom.add(ret);
        bottom.add(new JLabel("Rules: max " + LibraryService.MAX_LOANS + " items per member | Book "
                + PhysicalBook.LOAN_DAYS + " days, Rs." + PhysicalBook.FINE_PER_DAY + "/day late | Digital "
                + DigitalMedia.LOAN_DAYS + " days, Rs." + DigitalMedia.FINE_PER_DAY + "/day late"));
        centre.add(bottom, BorderLayout.SOUTH);
        p.add(centre, BorderLayout.CENTER);
        return p;
    }

    private JPanel overduePanel() {
        JPanel p = pad(new JPanel(new BorderLayout(8, 8)));
        finesLabel.setFont(finesLabel.getFont().deriveFont(Font.BOLD));
        p.add(finesLabel, BorderLayout.NORTH);
        p.add(new JScrollPane(overdueTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel historyPanel() {
        JPanel p = pad(new JPanel(new BorderLayout(8, 8)));
        p.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return p;
    }

    // ===================================================== actions =====================================================

    private void onAddItem() {
        int copies;
        try {
            copies = Integer.parseInt(copiesField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Copies must be a whole number.");
            return;
        }
        try {
            if (typeBox.getSelectedIndex() == 0) {
                service.addPhysicalBook(titleField.getText(), creatorField.getText(), copies,
                        extra1Field.getText(), extra2Field.getText());
            } else {
                int size;
                try {
                    size = Integer.parseInt(extra2Field.getText().trim());
                } catch (NumberFormatException e) {
                    showError("Size (MB) must be a whole number.");
                    return;
                }
                service.addDigitalMedia(titleField.getText(), creatorField.getText(), copies,
                        extra1Field.getText(), size);
            }
            titleField.setText(""); creatorField.setText(""); copiesField.setText("1");
            extra1Field.setText(""); extra2Field.setText("");
            refreshAll();
            setStatus("Item added to the catalog.");
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        } catch (StorageException ex) {
            showError("Storage problem: " + ex.getMessage());
        }
    }

    private void onRemoveItem() {
        Integer id = selectedId(itemTable, itemModel);
        if (id == null) { showError("Select an item in the table first."); return; }
        if (!confirm("Remove this item from the catalog?")) return;
        try {
            service.removeItem(id);
            refreshAll();
            setStatus("Item removed.");
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        } catch (StorageException ex) {
            showError("Storage problem: " + ex.getMessage());
        }
    }

    private void onRegisterMember() {
        try {
            Member m = service.registerMember(mNameField.getText(), mEmailField.getText(), mPhoneField.getText());
            mNameField.setText(""); mEmailField.setText(""); mPhoneField.setText("");
            refreshAll();
            setStatus("Member registered.");
            JOptionPane.showMessageDialog(this,
                    "Library card issued\n\nName:  " + m.getName() + "\nCard no:  " + m.getCardNumber()
                            + "\nValid from:  " + m.getRegisteredOn().format(DF),
                    "Library card", JOptionPane.INFORMATION_MESSAGE);
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        } catch (StorageException ex) {
            showError("Storage problem: " + ex.getMessage());
        }
    }

    private void onRemoveMember() {
        Integer id = selectedId(memberTable, memberModel);
        if (id == null) { showError("Select a member in the table first."); return; }
        if (!confirm("Remove this member?")) return;
        try {
            service.removeMember(id);
            refreshAll();
            setStatus("Member removed.");
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        } catch (StorageException ex) {
            showError("Storage problem: " + ex.getMessage());
        }
    }

    private void onBorrow() {
        Option m = (Option) memberBox.getSelectedItem();
        Option i = (Option) itemBox.getSelectedItem();
        if (m == null || i == null) { showError("Pick a member and an item first."); return; }
        try {
            Loan loan = service.borrow(m.id, i.id);
            refreshAll();
            setStatus("Item issued.");
            JOptionPane.showMessageDialog(this, "Issued successfully.\nDue date: " + loan.getDueDate().format(DF),
                    "Borrowed", JOptionPane.INFORMATION_MESSAGE);
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        } catch (StorageException ex) {
            showError("Storage problem: " + ex.getMessage());
        }
    }

    private void onReturn() {
        Integer id = selectedId(activeTable, activeModel);
        if (id == null) { showError("Select a borrowed item in the table first."); return; }
        try {
            Loan loan = service.returnItem(id);
            refreshAll();
            setStatus("Item returned.");
            int late = loan.overdueDays(service.today());
            String msg = late == 0 ? "Returned on time. No fine."
                    : "Returned " + late + " day(s) late.\nFine to collect: Rs." + loan.getFine();
            JOptionPane.showMessageDialog(this, msg, "Returned", JOptionPane.INFORMATION_MESSAGE);
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        } catch (StorageException ex) {
            showError("Storage problem: " + ex.getMessage());
        }
    }

    // ===================================================== refreshing =====================================================

    private void refreshAll() {
        refreshItems();
        refreshMembers();
        refreshLoans();
        refreshCombos();
    }

    private void refreshItems() {
        itemModel.setRowCount(0);
        for (LibraryItem i : service.searchItems(searchField.getText())) {
            itemModel.addRow(new Object[]{i.getId(), i.getTypeLabel(), i.getTitle(), i.getCreator(), i.getDetails(),
                    i.getTotalCopies(), service.availableCopies(i.getId())});
        }
    }

    private void refreshMembers() {
        memberModel.setRowCount(0);
        for (Member m : service.getMembers()) {
            memberModel.addRow(new Object[]{m.getId(), m.getCardNumber(), m.getName(), m.getEmail(), m.getPhone(),
                    m.getRegisteredOn().format(DF), service.activeLoanCount(m.getId())});
        }
    }

    private void refreshLoans() {
        var today = service.today();

        activeModel.setRowCount(0);
        for (Loan l : service.getActiveLoans()) {
            String st = l.isOverdue(today) ? "OVERDUE by " + l.overdueDays(today) + " day(s)" : "On time";
            activeModel.addRow(new Object[]{l.getId(), memberName(l.getMemberId()), itemTitle(l.getItemId()),
                    l.getBorrowDate().format(DF), l.getDueDate().format(DF), st});
        }

        overdueModel.setRowCount(0);
        int totalFine = 0;
        for (Loan l : service.getOverdueLoans()) {
            Member m = service.memberOrNull(l.getMemberId());
            int fine = service.currentFine(l);
            totalFine += fine;
            overdueModel.addRow(new Object[]{l.getId(), memberName(l.getMemberId()), m == null ? "-" : m.getCardNumber(),
                    itemTitle(l.getItemId()), l.getDueDate().format(DF), l.overdueDays(today), fine});
        }
        finesLabel.setText(overdueModel.getRowCount() + " overdue item(s). Total fines accruing: Rs." + totalFine);

        historyModel.setRowCount(0);
        for (Loan l : service.getAllLoans()) {
            String st = l.isReturned() ? (l.getFine() > 0 ? "Returned late" : "Returned")
                    : (l.isOverdue(today) ? "OVERDUE" : "Borrowed");
            historyModel.addRow(new Object[]{l.getId(), memberName(l.getMemberId()), itemTitle(l.getItemId()),
                    l.getBorrowDate().format(DF), l.getDueDate().format(DF),
                    l.isReturned() ? l.getReturnDate().format(DF) : "-", service.currentFine(l), st});
        }
    }

    private void refreshCombos() {
        Option selM = (Option) memberBox.getSelectedItem();
        Option selI = (Option) itemBox.getSelectedItem();
        memberBox.removeAllItems();
        for (Member m : service.getMembers()) memberBox.addItem(new Option(m.getId(), m.toString()));
        itemBox.removeAllItems();
        for (LibraryItem i : service.searchItems("")) {
            itemBox.addItem(new Option(i.getId(), i.getTitle() + "  [" + service.availableCopies(i.getId()) + " available]"));
        }
        restore(memberBox, selM);
        restore(itemBox, selI);
    }

    private static void restore(JComboBox<Option> box, Option previous) {
        if (previous == null) return;
        for (int k = 0; k < box.getItemCount(); k++) {
            if (box.getItemAt(k).id == previous.id) { box.setSelectedIndex(k); return; }
        }
    }

    // ===================================================== small helpers =====================================================

    private String memberName(int id) {
        Member m = service.memberOrNull(id);
        return m == null ? "Member #" + id + " (removed)" : m.getName();
    }

    private String itemTitle(int id) {
        LibraryItem i = service.itemOrNull(id);
        return i == null ? "Item #" + id + " (removed)" : i.getTitle();
    }

    private void updateExtraLabels() {
        boolean book = typeBox.getSelectedIndex() == 0;
        extra1Label.setText(book ? "ISBN (10/13 digits)" : "Format (EBOOK/AUDIOBOOK/VIDEO)");
        extra2Label.setText(book ? "Shelf" : "Size (MB)");
    }

    private Integer selectedId(JTable table, DefaultTableModel model) {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return (Integer) model.getValueAt(table.convertRowIndexToModel(row), 0);
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cannot do that", JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String msg) { status.setText(msg); }

    /** Switch tab (used for taking screenshots). */
    public void showTab(int index) { tabs.setSelectedIndex(index); }

    private static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    private static JPanel pad(JPanel p) {
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return p;
    }

    private static void place(JPanel p, GridBagConstraints g, int x, int y, JComponent c, double weightx) {
        g.gridx = x; g.gridy = y; g.gridwidth = 1; g.weightx = weightx;
        g.fill = weightx > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        p.add(c, g);
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return NUMERIC.contains(getColumnName(c)) ? Integer.class : String.class;
            }
        };
    }

    private static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(24);
        return t;
    }

    /** Combo-box entry: shows a label, remembers the id. */
    private static final class Option {
        final int id;
        final String label;
        Option(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    /** Opens the window on the Swing event thread. */
    public static void launch(LibraryService service) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            new LibraryFrame(service).setVisible(true);
        });
    }
}
