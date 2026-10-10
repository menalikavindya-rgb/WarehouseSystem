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
import javax.swing.JTable;
import javax.swing.JTextField;
import dao.CustomerDAO;
import model.Customer;

// Search the customers and add a new one.
public class CustomersPanel extends ScreenPanel {

    private CustomerDAO customerDAO = new CustomerDAO();

    private JTextField searchField = new JTextField(20);
    private JTable table = createTable();

    private JTextField nameField = new JTextField();
    private JTextField contactField = new JTextField();
    private JTextField emailField = new JTextField();
    private JTextField nicField = new JTextField();
    private JTextField addressField = new JTextField();

    public CustomersPanel() {
        super("Customers");

        // top: search bar
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show all");
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.add(new JLabel("Name contains:"));
        searchBar.add(searchField);
        searchBar.add(searchButton);
        searchBar.add(showAllButton);

        // bottom: the add-customer form
        JButton addButton = new JButton("Add customer");
        JPanel form = formPanel(
                new String[] { "Name *", "Contact number", "Email (optional)",
                        "NIC (optional, companies have none)", "Address" },
                new JComponent[] { nameField, contactField, emailField, nicField, addressField });
        JPanel addButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addButtonRow.add(addButton);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.add(new JLabel("Add a new customer"), BorderLayout.NORTH);
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(addButtonRow, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(searchBar, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        showAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                refresh();
            }
        });
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addCustomer();
            }
        });
    }

    private void addCustomer() {
        try {
            // the Customer constructor checks the name and the NIC, and throws if they are wrong
            Customer c = new Customer(0, nameField.getText().trim(), contactField.getText().trim(),
                    emailField.getText(), nicField.getText(), addressField.getText().trim());
            int newId = customerDAO.addCustomer(c);

            showInfo("Customer " + newId + " added.");
            nameField.setText("");
            contactField.setText("");
            emailField.setText("");
            nicField.setText("");
            addressField.setText("");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        try {
            ArrayList<Customer> customers = customerDAO.searchByName(searchField.getText().trim());
            ArrayList<Object[]> rows = new ArrayList<Object[]>();
            for (Customer c : customers) {
                rows.add(new Object[] { c.getCustomerId(), c.getName(), c.getContactNumber(),
                        c.getEmail(), c.getNicNumber(), c.getAddress() });
            }
            String[] columns = { "ID", "Name", "Contact", "Email", "NIC", "Address" };
            fillTable(table, columns, rows);
        } catch (Exception e) {
            handleError(e);
        }
    }
}
