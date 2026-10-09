package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import exception.CustomerNotFoundException;
import exception.OrderNotFoundException;
import java.util.ArrayList;
import exception.OrderNotCancellableException;
import exception.OrderNotConfirmableException;

public class OrderDAO {

    // Creates a new Pending order and returns its orderId
    public int createOrder(int customerId, String deliveryAddress)
            throws SQLException, CustomerNotFoundException {
        String sql = "INSERT INTO CustomerOrder (customerId, status, deliveryAddress) "
                + "VALUES (?, 'Pending', ?)";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, customerId);
            ps.setString(2, deliveryAddress);
            ps.executeUpdate();

            keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);

        } catch (SQLIntegrityConstraintViolationException e) {
            // the foreign key refused a customerId that does not exist
            throw new CustomerNotFoundException("No customer found with ID " + customerId);

        } finally {
            try {
                if (keys != null) {
                    keys.close();
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

    public void updateStatus(int orderId, String newStatus)
            throws SQLException, OrderNotFoundException {
        String sql = "UPDATE CustomerOrder SET status = ? WHERE orderId = ?";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }

        } finally {
            try {
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
        public String getOrderStatus(int orderId) throws SQLException, OrderNotFoundException {
        String sql = "SELECT status FROM CustomerOrder WHERE orderId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }
            return rs.getString("status");

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
        // Cancels an order, cancels its Active reservations and releases the reserved stock
    public void cancelOrder(int orderId, int userId)
            throws SQLException, OrderNotFoundException, OrderNotCancellableException {

        String sqlOrder = "SELECT status FROM CustomerOrder WHERE orderId = ? FOR UPDATE";
        String sqlFindReservations = "SELECT reservationId, itemId FROM Reservation "
                + "WHERE orderId = ? AND status = 'Active'";
        String sqlCancelReservation = "UPDATE Reservation SET status = 'Cancelled' "
                + "WHERE reservationId = ?";
        String sqlCountOthers = "SELECT COUNT(*) AS others FROM Reservation "
                + "WHERE itemId = ? AND status = 'Active'";
        String sqlRelease = "UPDATE StockItem SET availabilityStatus = 'Available' "
                + "WHERE itemId = ? AND availabilityStatus = 'Reserved'";
        String sqlCancelOrder = "UPDATE CustomerOrder SET status = 'Cancelled' WHERE orderId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean success = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start the transaction

            // Step 1: read and lock the order, and check it can be cancelled
            ps = con.prepareStatement(sqlOrder);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }
            String orderStatus = rs.getString("status");
            rs.close();
            ps.close();

            if (!orderStatus.equals("Pending") && !orderStatus.equals("Confirmed")) {
                throw new OrderNotCancellableException("Order " + orderId + " is "
                        + orderStatus + " and cannot be cancelled.");
            }

            // Step 2: find the Active reservations of this order
            ArrayList<Integer> reservationIds = new ArrayList<Integer>();
            ArrayList<Integer> itemIds = new ArrayList<Integer>();

            ps = con.prepareStatement(sqlFindReservations);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            while (rs.next()) {
                reservationIds.add(rs.getInt("reservationId"));
                itemIds.add(rs.getInt("itemId"));
            }
            rs.close();
            ps.close();

            // Step 3: cancel each reservation and release its stock if nobody else needs it
            LedgerDAO ledgerDAO = new LedgerDAO();

            for (int i = 0; i < reservationIds.size(); i++) {
                int reservationId = reservationIds.get(i);
                int itemId = itemIds.get(i);

                ps = con.prepareStatement(sqlCancelReservation);
                ps.setInt(1, reservationId);
                ps.executeUpdate();
                ps.close();

                ps = con.prepareStatement(sqlCountOthers);
                ps.setInt(1, itemId);
                rs = ps.executeQuery();
                rs.next();
                int others = rs.getInt("others");
                rs.close();
                ps.close();

                if (others == 0) {
                    ps = con.prepareStatement(sqlRelease);
                    ps.setInt(1, itemId);
                    int released = ps.executeUpdate();
                    ps.close();

                    if (released == 1) {
                        ledgerDAO.addEntry(con, itemId, userId, "Adjusted", 0,
                                "Order " + orderId + " cancelled: stock released to Available");
                    }
                }
            }

            // Step 4: mark the order itself as Cancelled
            ps = con.prepareStatement(sqlCancelOrder);
            ps.setInt(1, orderId);
            ps.executeUpdate();

            con.commit();
            success = true;

        } finally {
            try {
                if (con != null && !success) {
                    con.rollback();
                }
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
                System.out.println("Error during cleanup: " + e.getMessage());
            }
        }
    }
        // Moves an order from Pending to Confirmed (done by the sales officer)
    public void confirmOrder(int orderId)
            throws SQLException, OrderNotFoundException, OrderNotConfirmableException {

        String sqlStatus = "SELECT status FROM CustomerOrder WHERE orderId = ?";
        String sqlCount = "SELECT COUNT(*) AS active FROM Reservation "
                + "WHERE orderId = ? AND status = 'Active'";
        String sqlConfirm = "UPDATE CustomerOrder SET status = 'Confirmed' WHERE orderId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            // Step 1: the order must exist and be Pending
            ps = con.prepareStatement(sqlStatus);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }
            String orderStatus = rs.getString("status");
            rs.close();
            ps.close();

            if (!orderStatus.equals("Pending")) {
                throw new OrderNotConfirmableException("Order " + orderId + " is "
                        + orderStatus + ", so it cannot be confirmed.");
            }

            // Step 2: the order must have reserved stock
            ps = con.prepareStatement(sqlCount);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            rs.next();
            int activeReservations = rs.getInt("active");
            rs.close();
            ps.close();

            if (activeReservations == 0) {
                throw new OrderNotConfirmableException("Order " + orderId
                        + " has no reserved stock, so it cannot be confirmed.");
            }

            // Step 3: confirm it
            ps = con.prepareStatement(sqlConfirm);
            ps.setInt(1, orderId);
            ps.executeUpdate();

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