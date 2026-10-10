package ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import dao.BatchDAO;
import dao.ProductDAO;
import model.Batch;
import model.Product;
import model.User;
import model.WarehouseOperator;

// The operator registers a new production batch. It starts as Curing stock.
public class RegisterBatchPanel extends ScreenPanel {

    private User user;
    private ProductDAO productDAO = new ProductDAO();
    private BatchDAO batchDAO = new BatchDAO();

    private JComboBox<Product> productBox = new JComboBox<Product>();
    private JTextField dateField = new JTextField(LocalDate.now().toString());
    private JTextField curingDaysField = new JTextField();
    private JTextField quantityField = new JTextField();
    private JTextField locationField = new JTextField();
    private JLabel readyLabel = new JLabel(" ");

    public RegisterBatchPanel(User user) {
        super("Register Batch");
        this.user = user;

        // an operator's own yard zone is a sensible starting location
        if (user instanceof WarehouseOperator) {
            locationField.setText(((WarehouseOperator) user).getAssignedYardZone());
        }

        JButton registerButton = new JButton("Register batch");

        JPanel form = formPanel(
                new String[] { "Product", "Production date (yyyy-mm-dd)", "Curing days",
                        "Quantity (units)", "Yard location" },
                new JComponent[] { productBox, dateField, curingDaysField, quantityField, locationField });

        JPanel buttonRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        buttonRow.add(registerButton);

        JPanel body = new JPanel(new BorderLayout(0, 15));
        body.add(form, BorderLayout.NORTH);
        JPanel lower = new JPanel(new BorderLayout(0, 10));
        lower.add(buttonRow, BorderLayout.NORTH);
        lower.add(readyLabel, BorderLayout.CENTER);
        body.add(lower, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // choosing a product fills in its normal curing time
        productBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Product p = (Product) productBox.getSelectedItem();
                if (p != null) {
                    curingDaysField.setText(String.valueOf(p.getDefaultCuringDurationDays()));
                }
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registerBatch();
            }
        });
    }

    private void registerBatch() {
        try {
            // Step 1: read and check what was typed
            Product product = (Product) productBox.getSelectedItem();
            if (product == null) {
                throw new IllegalArgumentException("Please choose a product.");
            }

            LocalDate productionDate;
            try {
                productionDate = LocalDate.parse(dateField.getText().trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Production date must look like 2026-10-09.");
            }

            int curingDays = readInt(curingDaysField, "Curing days");
            if (curingDays < 0) {
                throw new IllegalArgumentException("Curing days cannot be negative.");
            }
            int quantity = readInt(quantityField, "Quantity");
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0.");
            }
            String location = locationField.getText().trim();
            if (location.isEmpty()) {
                throw new IllegalArgumentException("Please enter the yard location.");
            }

            // Step 2: build the Batch object and let the DAO save it
            LocalDate curingEnd = productionDate.plusDays(curingDays);
            Batch batch = new Batch(0, product.getProductId(), productionDate, curingEnd, quantity, "Curing");
            int batchId = batchDAO.registerBatch(batch, location, user.getUserId());

            // Step 3: tell the user and clear the quantity for the next batch
            readyLabel.setText("Batch " + batchId + " registered: " + quantity + " units of "
                    + product.getName() + " at " + location + ". Ready for sale on " + curingEnd + ".");
            quantityField.setText("");

        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        // load the products once; keep the user's choice on later refreshes
        if (productBox.getItemCount() > 0) {
            return;
        }
        try {
            ArrayList<Product> products = productDAO.getAllProducts();
            for (Product p : products) {
                productBox.addItem(p);
            }
        } catch (Exception e) {
            handleError(e);
        }
    }
}
