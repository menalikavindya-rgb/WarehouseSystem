package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.StockItem;

public class StockItemDAO {

    public ArrayList<StockItem> getAllStock() throws SQLException {
        String sql = "SELECT itemId, batchId, location, quantity, availabilityStatus "
                + "FROM StockItem ORDER BY itemId";

        ArrayList<StockItem> items = new ArrayList<StockItem>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                StockItem s = new StockItem(
                        rs.getInt("itemId"),
                        rs.getInt("batchId"),
                        rs.getString("location"),
                        rs.getInt("quantity"),
                        rs.getString("availabilityStatus"));
                items.add(s);
            }
            return items;

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

    public ArrayList<StockItem> getItemsByStatus(String status) throws SQLException {
        String sql = "SELECT itemId, batchId, location, quantity, availabilityStatus "
                + "FROM StockItem WHERE availabilityStatus = ? ORDER BY itemId";

        ArrayList<StockItem> items = new ArrayList<StockItem>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();

            while (rs.next()) {
                StockItem s = new StockItem(
                        rs.getInt("itemId"),
                        rs.getInt("batchId"),
                        rs.getString("location"),
                        rs.getInt("quantity"),
                        rs.getString("availabilityStatus"));
                items.add(s);
            }
            return items;

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

    public int getTotalPhysicalStock() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) AS total FROM StockItem "
                + "WHERE availabilityStatus IN ('Curing', 'Available', 'Reserved')";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt("total");

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

    public int getCuringStock() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) AS total FROM StockItem "
                + "WHERE availabilityStatus = 'Curing'";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt("total");

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

    // True sellable stock = Available quantity minus active reservations
    public int getSellableStock() throws SQLException {
        String sql = "SELECT COALESCE(SUM(s.quantity - COALESCE(r.reservedQty, 0)), 0) AS total "
                + "FROM StockItem s "
                + "LEFT JOIN (SELECT itemId, SUM(reservedQuantity) AS reservedQty "
                + "           FROM Reservation WHERE status = 'Active' GROUP BY itemId) r "
                + "ON s.itemId = r.itemId "
                + "WHERE s.availabilityStatus = 'Available'";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt("total");

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