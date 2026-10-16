package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import dao.ProductDAO;
import model.Product;
import model.User;

// The manager's product rules: how many days each product must cure, and adding new products.
public class ProductsPanel extends ScreenPanel {

    private User user;
    private ProductDAO productDAO = new ProductDAO();
    private JTable table = createTable();

    private JLabel selectedLabel = new JLabel("Select a product in the table.");
    private JTextField curingField = new JTextField();

    private JTextField nameField = new JTextField();
    private JTextField categoryField = new JTextField();
    private JTextField priceField = new JTextField();
    private JTextField newCuringField = new JTextField("7");

    public ProductsPanel(User user) {
        super("Products");
        this.user = user;

        JLabel note = new JLabel("<html>Curing days apply to NEW batches only; existing batches keep their curing end date. "
                + "Prices cannot be edited here, because order totals are calculated from the current price.</html>");

        // tab 1: change the curing days of the selected product
        JButton saveButton = new JButton("Save curing days");
        JPanel editForm = formPanel(new String[] { "Curing days for the selected product" },
                new JComponent[] { curingField });
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        saveRow.add(saveButton);
        JPanel editTab = new JPanel(new BorderLayout(0, 8));
        editTab.add(selectedLabel, BorderLayout.NORTH);
        editTab.add(editForm, BorderLayout.CENTER);
        editTab.add(saveRow, BorderLayout.SOUTH);

        // tab 2: add a new product
        JButton addButton = new JButton("Add product");
        JPanel addForm = formPanel(
                new String[] { "Name *", "Category", "Unit price (Rs.) *", "Curing days *" },
                new JComponent[] { nameField, categoryField, priceField, newCuringField });
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addRow.add(addButton);
        JPanel addTab = new JPanel(new BorderLayout(0, 8));
        addTab.add(addForm, BorderLayout.CENTER);
        addTab.add(addRow, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Change curing days", editTab);
        tabs.addTab("Add a new product", addTab);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(note, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(tabs, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showSelected();
                }
            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCuringDays();
            }
        });
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addProduct();
            }
        });
    }

    private void showSelected() {
        Object id = selectedValue(table, 0);
        if (id == null) {
            selectedLabel.setText("Select a product in the table.");
            curingField.setText("");
            return;
        }
        selectedLabel.setText("Selected: " + id + " - " + selectedValue(table, 1));
        curingField.setText(String.valueOf(selectedValue(table, 4)));
    }

    private void saveCuringDays() {
        Object id = selectedValue(table, 0);
        if (id == null) {
            showWarning("Please select a product in the table first.");
            return;
        }
        try {
            int days = readInt(curingField, "Curing days");
            productDAO.updateCuringDays(user.getUserId(), ((Number) id).intValue(), days);
            showInfo("Curing days saved. New batches of this product will cure for " + days + " days.");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void addProduct() {
        try {
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            double price = readDouble(priceField, "Unit price");
            int days = readInt(newCuringField, "Curing days");

            int newId = productDAO.addProduct(user.getUserId(), name, category, price, days);
            showInfo("Product " + newId + " added.");

            nameField.setText("");
            categoryField.setText("");
            priceField.setText("");
            newCuringField.setText("7");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        try {
            ArrayList<Product> products = productDAO.getAllProducts();
            ArrayList<Object[]> rows = new ArrayList<Object[]>();
            for (Product p : products) {
                rows.add(new Object[] { p.getProductId(), p.getName(), p.getCategory(),
                        p.getUnitPrice(), p.getDefaultCuringDurationDays() });
            }
            formatMoney(rows, 3);
            String[] columns = { "ID", "Name", "Category", "Unit price (Rs.)", "Curing days" };
            fillTable(table, columns, rows);
        } catch (Exception e) {
            handleError(e);
        }
    }
}
