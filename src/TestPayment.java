import java.sql.SQLException;
import dao.PaymentDAO;
import exception.OrderNotFoundException;
import exception.OverpaymentException;

public class TestPayment {

    public static void main(String[] args) {
        PaymentDAO paymentDAO = new PaymentDAO();
        int orderId = 251;

        try {
            double total = paymentDAO.getOrderTotal(orderId);
            System.out.println("Order total: " + total);
            System.out.println("Balance before: " + paymentDAO.getOutstandingBalance(orderId));

            // Test 1: pay half of the total as an advance payment
            double firstPayment = Math.round(total / 2 * 100) / 100.0;
            paymentDAO.addPayment(orderId, firstPayment);
            System.out.println("Test 1 OK: paid " + firstPayment + ", balance now "
                    + paymentDAO.getOutstandingBalance(orderId));

            // Test 2: pay the whole remaining balance
            double balance = paymentDAO.getOutstandingBalance(orderId);
            paymentDAO.addPayment(orderId, balance);
            System.out.println("Test 2 OK: paid " + balance + ", balance now "
                    + paymentDAO.getOutstandingBalance(orderId));

            // Test 3: one more payment, which must be refused as an overpayment
            try {
                paymentDAO.addPayment(orderId, 1.00);
                System.out.println("Test 3: this line should not print.");
            } catch (OverpaymentException e) {
                System.out.println("Test 3 refused as expected: " + e.getMessage());
            }

            // Test 4: an order that does not exist
            try {
                paymentDAO.addPayment(999999, 100.00);
                System.out.println("Test 4: this line should not print.");
            } catch (OrderNotFoundException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // Test 5: a negative amount
            try {
                paymentDAO.addPayment(orderId, -5.00);
                System.out.println("Test 5: this line should not print.");
            } catch (IllegalArgumentException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (OrderNotFoundException e) {
            System.out.println("Order problem: " + e.getMessage());
        } catch (OverpaymentException e) {
            System.out.println("Payment problem: " + e.getMessage());
        }
    }
}
