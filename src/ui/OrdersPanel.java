package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import dao.CustomerDAO;
import dao.OrderDAO;
import dao.ReservationDAO;
import dao.ViewDAO;
import model.Customer;
import model.User;

// Create orders, allocate stock to them, confirm or cancel them, and see their reservations.
public class OrdersPanel extends ScreenPanel {

    private User user;
    private ViewDAO viewDAO = new ViewDAO();
    private OrderDAO orderDAO = new OrderDAO();
    private ReservationDAO reservationDAO = new ReservationDAO();
    private CustomerDAO customerDAO = new CustomerDAO();

    private JComboBox<String> statusBox = new JComboBox<String>(
            new String[] { "All", "Pending", "Confirmed", "PartiallyShipped", "Shipped", "Cancelled" });
    private JTable ordersTable = createTable();
    private JTable reservationsTable = createTable();
    private JLabel summaryLabel = new JLabel("Select an order to see its stock and balance.");

    public OrdersPanel(User user) {
        super("Orders");
        this.user = user;

        // top: filter and the action buttons
        JButton newButton = new JButton("New order");
        JButton reserveButton = new JButton("Reserve stock");
        JButton confirmButton = new JButton("Confirm order");
        JButton cancelButton = new JButton("Cancel order");
        JButton refreshButton = new JButton("Refresh");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(new JLabel("Status:"));
        bar.add(statusBox);
        bar.add(newButton);
        bar.add(reserveButton);
        bar.add(confirmButton);
        bar.add(cancelButton);
        bar.add(refreshButton);

        // middle: orders on top, the selected order's reservations below
        JPanel lower = new JPanel(new BorderLayout(0, 5));
        lower.add(summaryLabel, BorderLayout.NORTH);
        lower.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(ordersTable), lower);
        split.setResizeWeight(0.6);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(bar, BorderLayout.NORTH);
        body.add(split, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // when another order is selected, show its details (ListSelectionListener is an interface)
        ordersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showSelectedOrder();
                }
            }
        });

        statusBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newOrder();
            }
        });
        reserveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reserveStock();
            }
        });
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmOrder();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelOrder();
            }
        });
    }

    // The orderId of the selected row, or 0 if nothing is selected.
    private int selectedOrderId() {
        Object id = selectedValue(ordersTable, 0);
        if (id == null) {
            return 0;
        }
        return ((Number) id).intValue();
    }

    // Shows the reservations and the money summary of the selected order.
    private void showSelectedOrder() {
        int orderId = selectedOrderId();
        try {
            String[] columns = { "Reservation", "Item", "Product", "Units", "Returned", "Unit price (Rs.)", "Status" };
            if (orderId == 0) {
                fillTable(reservationsTable, columns, new ArrayList<Object[]>());
                summaryLabel.setText("Select an order to see its stock and balance.");
                return;
            }
            ArrayList<Object[]> rows = viewDAO.getReservationRows(orderId);
            formatMoney(rows, 5);
            fillTable(reservationsTable, columns, rows);

            // the money columns of the orders table are already text such as 12,500.00
            summaryLabel.setText("Order " + orderId + " (" + selectedValue(ordersTable, 3) + ")   |   total Rs. "
                    + selectedValue(ordersTable, 4) + "   |   paid Rs. " + selectedValue(ordersTable, 5)
                    + "   |   balance Rs. " + selectedValue(ordersTable, 6));
        } catch (Exception e) {
            handleError(e);
        }
    }

    // Reloads the orders and selects the given order again (0 = select nothing).
    private void reloadOrders(int keepOrderId) {
        try {
            String choice = (String) statusBox.getSelectedItem();
            String status = choice.equals("All") ? null : choice;

            String[] columns = { "Order", "Customer", "Date", "Status", "Total (Rs.)", "Paid (Rs.)", "Balance (Rs.)" };
            ArrayList<Object[]> rows = viewDAO.getOrderRows(status);
            formatMoney(rows, 4, 5, 6);
            fillTable(ordersTable, columns, rows);

            for (int i = 0; i < ordersTable.getRowCount(); i++) {
                int id = ((Number) ordersTable.getValueAt(i, 0)).intValue();
                if (id == keepOrderId) {
                    ordersTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void newOrder() {
        try {
            // Step 1: let the user pick a customer from the list
            ArrayList<Customer> customers = customerDAO.searchByName("");
            if (customers.isEmpty()) {
                showWarning("There are no customers yet. Add one on the Customers screen first.");
                return;
            }
            final ArrayList<Customer> customerList = customers;
            final JComboBox<String> customerBox = new JComboBox<String>();
            for (Customer c : customerList) {
                customerBox.addItem(c.getCustomerId() + " - " + c.getName());
            }
            final JTextField addressField = new JTextField(customerList.get(0).getAddress(), 30);

            // choosing a customer fills in their address as the delivery address
            customerBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addressField.setText(customerList.get(customerBox.getSelectedIndex()).getAddress());
                }
            });

            Object[] fields = { "Customer", customerBox, "Delivery address", addressField };
            int answer = JOptionPane.showConfirmDialog(this, fields, "New order",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }

            // Step 2: create the order
            Customer chosen = customerList.get(customerBox.getSelectedIndex());
            int orderId = orderDAO.createOrder(chosen.getCustomerId(), addressField.getText().trim());
            statusBox.setSelectedItem("All");
            reloadOrders(orderId);
            showInfo("Order " + orderId + " created for " + chosen.getName()
                    + ". Next: choose \"Reserve stock\" to add products to it.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void reserveStock() {
        int orderId = selectedOrderId();
        if (orderId == 0) {
            showWarning("Please select an order first.");
            return;
        }
        try {
            // a small table of the stock that can be reserved, shown inside the dialog
            JTable stockTable = createTable();
            String[] columns = { "Item ID", "Product", "Category", "Location", "Units", "Unit price (Rs.)" };
            fillTable(stockTable, columns, viewDAO.getAvailableRows());
            JScrollPane scroll = new JScrollPane(stockTable);
            scroll.setPreferredSize(new Dimension(750, 260));
            JTextField quantityField = new JTextField(8);

            Object[] fields = { "Order " + orderId + ": select the stock to reserve", scroll,
                    "Units to reserve", quantityField };
            int answer = JOptionPane.showConfirmDialog(this, fields, "Reserve stock",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }

            Object itemId = selectedValue(stockTable, 0);
            if (itemId == null) {
                showWarning("You did not select a stock row.");
                return;
            }
            int quantity = readInt(quantityField, "Units to reserve");

            int reservationId = reservationDAO.reserveStock(orderId, ((Number) itemId).intValue(),
                    quantity, user.getUserId());
            reloadOrders(orderId);
            showInfo("Reserved " + quantity + " units (reservation " + reservationId + ").");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void confirmOrder() {
        int orderId = selectedOrderId();
        if (orderId == 0) {
            showWarning("Please select an order first.");
            return;
        }
        try {
            orderDAO.confirmOrder(orderId);
            reloadOrders(orderId);
            showInfo("Order " + orderId + " is now Confirmed.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void cancelOrder() {
        int orderId = selectedOrderId();
        if (orderId == 0) {
            showWarning("Please select an order first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Cancel order " + orderId + "? Its reserved stock will be released.",
                "Cancel order", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            orderDAO.cancelOrder(orderId, user.getUserId());
            reloadOrders(orderId);
            showInfo("Order " + orderId + " has been cancelled.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        reloadOrders(selectedOrderId());
    }
}
