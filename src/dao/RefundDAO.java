package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import exception.OrderNotFoundException;
import exception.RefundNotAllowedException;

public class RefundDAO {

    // Records a refund and returns the new refundId.
    // reason must be "OrderCancelled" or "GoodsReturned".
    public int issueRefund(int orderId, double amount, String reason, int userId)
            throws SQLException, OrderNotFoundException, RefundNotAllowedException {

        if (amount <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than 0.");
        }
        if (!reason.equals("OrderCancelled") && !reason.equals("GoodsReturned")) {
            throw new IllegalArgumentException("Reason must be OrderCancelled or GoodsReturned.");
        }

        String sqlStatus = "SELECT status FROM CustomerOrder WHERE orderId = ?";
        String sqlReturned = "SELECT COALESCE(SUM(returnedQuantity), 0) AS returned "
                + "FROM Reservation WHERE orderId = ?";
        String sqlInsert = "INSERT INTO Refund (orderId, amount, reason, processedBy) "
                + "VALUES (?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            // Step 1: the order must exist
            ps = con.prepareStatement(sqlStatus);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }
            String orderStatus = rs.getString("status");
            rs.close();
            ps.close();

            // Step 2: never refund more than the customer has actually paid
            PaymentDAO paymentDAO = new PaymentDAO();
            double netPaid = paymentDAO.getAmountPaid(orderId);
            if (amount > netPaid + 0.004) {
                throw new RefundNotAllowedException("Cannot refund " + amount
                        + ": the customer has only paid "
                        + Math.round(netPaid * 100.0) / 100.0 + " (after earlier refunds).");
            }

            // Step 3: the rules for each reason
            if (reason.equals("OrderCancelled")) {
                if (!orderStatus.equals("Cancelled")) {
                    throw new RefundNotAllowedException("Order " + orderId + " is "
                            + orderStatus + ", not Cancelled.");
                }
            } else {
                ps = con.prepareStatement(sqlReturned);
                ps.setInt(1, orderId);
                rs = ps.executeQuery();
                rs.next();
                int returnedUnits = rs.getInt("returned");
                rs.close();
                ps.close();

                if (returnedUnits == 0) {
                    throw new RefundNotAllowedException("No goods have been returned for order "
                            + orderId + ".");
                }

                double surplus = netPaid - paymentDAO.getOrderTotal(orderId);
                if (amount > surplus + 0.004) {
                    throw new RefundNotAllowedException("Cannot refund " + amount
                            + ": after the returns, the customer has overpaid by only "
                            + Math.round(surplus * 100.0) / 100.0 + ".");
                }
            }

            // Step 4: save the refund
            ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, orderId);
            ps.setDouble(2, amount);
            ps.setString(3, reason);
            ps.setInt(4, userId);
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