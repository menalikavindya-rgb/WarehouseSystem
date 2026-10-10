package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.RefundDAO;
import dao.ViewDAO;
import model.User;
import model.WarehouseManager;

// Payments and refunds of one order. Only a manager sees the refund controls.
public class PaymentsPanel extends ScreenPanel {

    private User user;
    private ViewDAO viewDAO = new ViewDAO();
    private OrderDAO orderDAO = new OrderDAO();
    private PaymentDAO paymentDAO = new PaymentDAO();
    private RefundDAO refundDAO = new RefundDAO();

    private int currentOrderId = 0;   // the order that is loaded, 0 = none yet

    private JTextField orderField = new JTextField(8);
    private JLabel summaryLabel = new JLabel("Enter an order number and press Load.");
    private JTable paymentsTable = createTable();
    private JTable refundsTable = createTable();
    private JTextField paymentField = new JTextField(10);
    private JTextField refundField = new JTextField(10);
    private JComboBox<String> reasonBox = new JComboBox<String>(new String[] { "OrderCancelled", "GoodsReturned" });

    public PaymentsPanel(User user) {
        super("Payments");
        this.user = user;

        // top: choose the order
        JButton loadButton = new JButton("Load");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.add(new JLabel("Order number:"));
        top.add(orderField);
        top.add(loadButton);
        top.add(summaryLabel);

        // middle: payments on the left, refunds on the right
        JPanel paymentsBox = new JPanel(new BorderLayout(0, 5));
        paymentsBox.add(new JLabel("Payments"), BorderLayout.NORTH);
        paymentsBox.add(new JScrollPane(paymentsTable), BorderLayout.CENTER);
        JPanel refundsBox = new JPanel(new BorderLayout(0, 5));
        refundsBox.add(new JLabel("Refunds"), BorderLayout.NORTH);
        refundsBox.add(new JScrollPane(refundsTable), BorderLayout.CENTER);
        JPanel tables = new JPanel(new GridLayout(1, 2, 15, 0));
        tables.add(paymentsBox);
        tables.add(refundsBox);

        // bottom: record a payment (everyone), issue a refund (managers only)
        JButton payButton = new JButton("Record payment");
        JPanel payRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        payRow.add(new JLabel("Payment amount (Rs.):"));
        payRow.add(paymentField);
        payRow.add(payButton);

        JPanel bottom = new JPanel(new GridLayout(0, 1));
        bottom.add(payRow);

        if (user instanceof WarehouseManager) {
            JButton refundButton = new JButton("Issue refund");
            JPanel refundRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            refundRow.add(new JLabel("Refund amount (Rs.):"));
            refundRow.add(refundField);
            refundRow.add(new JLabel("Reason:"));
            refundRow.add(reasonBox);
            refundRow.add(refundButton);
            bottom.add(refundRow);

            refundButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    issueRefund();
                }
            });
        }

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(top, BorderLayout.NORTH);
        body.add(tables, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    currentOrderId = readInt(orderField, "Order number");
                    reloadOrder();
                } catch (Exception ex) {
                    handleError(ex);
                }
            }
        });
        payButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recordPayment();
            }
        });
    }

    private void recordPayment() {
        if (currentOrderId == 0) {
            showWarning("Please load an order first.");
            return;
        }
        try {
            double amount = readDouble(paymentField, "Payment amount");
            int paymentId = paymentDAO.addPayment(currentOrderId, amount);
            paymentField.setText("");
            reloadOrder();
            showInfo("Payment " + paymentId + " of " + money(amount) + " recorded.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void issueRefund() {
        if (currentOrderId == 0) {
            showWarning("Please load an order first.");
            return;
        }
        try {
            double amount = readDouble(refundField, "Refund amount");
            String reason = (String) reasonBox.getSelectedItem();
            int refundId = refundDAO.issueRefund(currentOrderId, amount, reason, user.getUserId());
            refundField.setText("");
            reloadOrder();
            showInfo("Refund " + refundId + " of " + money(amount) + " issued.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    // Reloads the summary and both tables for the current order.
    private void reloadOrder() {
        try {
            String status = orderDAO.getOrderStatus(currentOrderId);   // throws if the order does not exist
            double total = paymentDAO.getOrderTotal(currentOrderId);
            double paid = paymentDAO.getAmountPaid(currentOrderId);
            double balance = paymentDAO.getOutstandingBalance(currentOrderId);

            summaryLabel.setText("Order " + currentOrderId + " (" + status + ")   total " + money(total)
                    + "   paid " + money(paid) + "   balance " + money(balance));

            ArrayList<Object[]> payments = viewDAO.getPaymentRows(currentOrderId);
            formatMoney(payments, 2);
            fillTable(paymentsTable, new String[] { "Payment", "Date", "Amount (Rs.)" }, payments);

            ArrayList<Object[]> refunds = viewDAO.getRefundRows(currentOrderId);
            formatMoney(refunds, 2);
            fillTable(refundsTable, new String[] { "Refund", "Date", "Amount (Rs.)", "Reason", "Processed by" }, refunds);

        } catch (Exception e) {
            currentOrderId = 0;
            summaryLabel.setText("Enter an order number and press Load.");
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        if (currentOrderId > 0) {
            reloadOrder();
        }
    }
}
