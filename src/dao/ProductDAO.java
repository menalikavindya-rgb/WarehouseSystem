package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
}