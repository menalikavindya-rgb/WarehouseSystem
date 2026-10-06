package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import exception.CustomerNotFoundException;
import exception.DuplicateCustomerException;
import model.Customer;

public class CustomerDAO {

    public int addCustomer(Customer c) throws SQLException, DuplicateCustomerException {
        String sql = "INSERT INTO Customer (name, contactNumber, email, nicNumber, address) "
                + "VALUES (?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getName());
            ps.setString(2, c.getContactNumber());
            ps.setString(3, c.getEmail());       // null is saved as NULL
            ps.setString(4, c.getNicNumber());   // null is saved as NULL
            ps.setString(5, c.getAddress());
            ps.executeUpdate();

            keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);

        } catch (SQLIntegrityConstraintViolationException e) {
            // the database refused a duplicate email or NIC
            throw new DuplicateCustomerException(
                    "A customer with this email or NIC already exists.");

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

    public ArrayList<Customer> searchByName(String text) throws SQLException {
        String sql = "SELECT customerId, name, contactNumber, email, nicNumber, address "
                + "FROM Customer WHERE name LIKE ? ORDER BY name";

        ArrayList<Customer> customers = new ArrayList<Customer>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, "%" + text + "%");
            rs = ps.executeQuery();

            while (rs.next()) {
                Customer c = new Customer(
                        rs.getInt("customerId"),
                        rs.getString("name"),
                        rs.getString("contactNumber"),
                        rs.getString("email"),
                        rs.getString("nicNumber"),
                        rs.getString("address"));
                customers.add(c);
            }
            return customers;

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

    public Customer getCustomerById(int customerId) throws SQLException, CustomerNotFoundException {
        String sql = "SELECT customerId, name, contactNumber, email, nicNumber, address "
                + "FROM Customer WHERE customerId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, customerId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new CustomerNotFoundException("No customer found with ID " + customerId);
            }

            return new Customer(
                    rs.getInt("customerId"),
                    rs.getString("name"),
                    rs.getString("contactNumber"),
                    rs.getString("email"),
                    rs.getString("nicNumber"),
                    rs.getString("address"));

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