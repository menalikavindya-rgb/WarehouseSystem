package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import exception.AccessDeniedException;
import exception.ProductNotFoundException;
import model.Product;

public class ProductDAO {

    public ArrayList<Product> getAllProducts() throws SQLException {
        String sql = "SELECT productId, name, category, unitPrice, defaultCuringDurationDays "
                + "FROM Product ORDER BY category, name";

        ArrayList<Product> products = new ArrayList<Product>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("productId"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("unitPrice"),
                        rs.getInt("defaultCuringDurationDays"));
                products.add(p);
            }
            return products;

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

    public Product getProductById(int productId) throws SQLException, ProductNotFoundException {
        String sql = "SELECT productId, name, category, unitPrice, defaultCuringDurationDays "
                + "FROM Product WHERE productId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, productId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new ProductNotFoundException("No product found with ID " + productId);
            }

            return new Product(
                    rs.getInt("productId"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("unitPrice"),
                    rs.getInt("defaultCuringDurationDays"));

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

    // Adds a new product. Only a manager may do this. Returns the new productId.
    public int addProduct(int managerId, String name, String category, double unitPrice, int curingDays)
            throws SQLException, AccessDeniedException {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("The product name cannot be empty.");
        }
        if (unitPrice <= 0) {
            throw new IllegalArgumentException("The unit price must be greater than 0.");
        }
        if (curingDays < 0 || curingDays > 90) {
            throw new IllegalArgumentException("Curing days must be between 0 and 90.");
        }

        String sql = "INSERT INTO Product (name, category, unitPrice, defaultCuringDurationDays) "
                + "VALUES (?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;

        try {
            con = DBConnection.getConnection();
            AccessChecker.requireManager(con, managerId);

            ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name.trim());
            ps.setString(2, category == null ? null : category.trim());
            ps.setDouble(3, unitPrice);
            ps.setInt(4, curingDays);
            ps.executeUpdate();

            keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);

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

    // Changes how many days a product must cure. It applies to NEW batches only.
    // Only a manager may do this.
    public void updateCuringDays(int managerId, int productId, int curingDays)
            throws SQLException, AccessDeniedException, ProductNotFoundException {

        if (curingDays < 0 || curingDays > 90) {
            throw new IllegalArgumentException("Curing days must be between 0 and 90.");
        }

        String sql = "UPDATE Product SET defaultCuringDurationDays = ? WHERE productId = ?";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            AccessChecker.requireManager(con, managerId);

            ps = con.prepareStatement(sql);
            ps.setInt(1, curingDays);
            ps.setInt(2, productId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new ProductNotFoundException("No product found with ID " + productId);
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
}
