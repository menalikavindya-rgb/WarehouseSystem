package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import exception.CustomerNotFoundException;
import exception.OrderNotFoundException;

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
}