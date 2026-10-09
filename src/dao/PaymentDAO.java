package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import exception.OrderNotFoundException;
import exception.OverpaymentException;

public class PaymentDAO {

    // Total value of the stock reserved for this order
    public double getOrderTotal(int orderId) throws SQLException {
                String sql = "SELECT COALESCE(SUM((r.reservedQuantity - r.returnedQuantity) * p.unitPrice), 0) AS total "
                + "FROM Reservation r "
                + "JOIN StockItem s ON r.itemId = s.itemId "
                + "JOIN Batch b ON s.batchId = b.batchId "
                + "JOIN Product p ON b.productId = p.productId "
                + "WHERE r.orderId = ? AND r.status <> 'Cancelled'";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            rs.next();
            return rs.getDouble("total");

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // Sum of all payments made for this order
    public double getAmountPaid(int orderId) throws SQLException {
                String sql = "SELECT COALESCE((SELECT SUM(amount) FROM Payment WHERE orderId = ?), 0) "
                + "- COALESCE((SELECT SUM(amount) FROM Refund WHERE orderId = ?), 0) AS paid";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            rs = ps.executeQuery();
            rs.next();
            return rs.getDouble("paid");

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    public double getOutstandingBalance(int orderId) throws SQLException {
        double balance = getOrderTotal(orderId) - getAmountPaid(orderId);
        return Math.round(balance * 100.0) / 100.0;   // round to 2 decimal places
    }

    // Records a payment and returns the new paymentId
    public int addPayment(int orderId, double amount)
            throws SQLException, OrderNotFoundException, OverpaymentException {

        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0.");
        }

        String sqlStatus = "SELECT status FROM CustomerOrder WHERE orderId = ?";
        String sqlInsert = "INSERT INTO Payment (orderId, amount) VALUES (?, ?)";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            // Step 1: the order must exist and must not be cancelled
            ps = con.prepareStatement(sqlStatus);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }
            String orderStatus = rs.getString("status");
            rs.close();
            ps.close();

            if (orderStatus.equals("Cancelled")) {
                throw new OrderNotFoundException("Order " + orderId
                        + " is Cancelled and cannot take payments.");
            }

            // Step 2: refuse overpayment (only once the order has reserved stock)
            double total = getOrderTotal(orderId);
            double paid = getAmountPaid(orderId);
            if (total > 0 && paid + amount > total + 0.004) {
                double balance = Math.round((total - paid) * 100.0) / 100.0;
                throw new OverpaymentException("Payment of " + amount
                        + " is more than the outstanding balance of " + balance + ".");
            }

            // Step 3: save the payment
            ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, orderId);
            ps.setDouble(2, amount);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}