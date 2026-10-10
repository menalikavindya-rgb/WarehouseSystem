package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import dao.ShippingDAO;
import dao.ViewDAO;
import model.User;

// Reservations that are ready to leave the yard. Shipping needs a Confirmed order and a payment.
public class ShipmentsPanel extends ScreenPanel {

    private User user;
    private ViewDAO viewDAO = new ViewDAO();
    private ShippingDAO shippingDAO = new ShippingDAO();
    private JTable table = createTable();

    public ShipmentsPanel(User user) {
        super("Shipments");
        this.user = user;

        JButton shipButton = new JButton("Ship selected reservation");
        JButton refreshButton = new JButton("Refresh");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.add(shipButton);
        buttons.add(refreshButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(buttons, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        shipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ship();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    private void ship() {
        Object id = selectedValue(table, 0);
        if (id == null) {
            showWarning("Please select a reservation in the table first.");
            return;
        }
        int reservationId = ((Number) id).intValue();

        int answer = JOptionPane.showConfirmDialog(this,
                "Ship reservation " + reservationId + " now?", "Confirm shipping",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            shippingDAO.shipReservation(reservationId, user.getUserId());
            showInfo("Reservation " + reservationId + " has been shipped.");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        try {
            String[] columns = { "Reservation", "Order", "Customer", "Product", "Units",
                    "Location", "Order status", "Payment received" };
            fillTable(table, columns, viewDAO.getShippableRows());
        } catch (Exception e) {
            handleError(e);
        }
    }
}
