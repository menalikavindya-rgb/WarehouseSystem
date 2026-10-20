package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

// Read-only figures for the dashboard. Nothing here changes the database.
public class DashboardDAO {

    // Runs a query and returns every row as an Object[] (same idea as ViewDAO).
    private ArrayList<Object[]> runQuery(String sql, int columnCount) throws SQLException {
        ArrayList<Object[]> rows = new ArrayList<Object[]>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int c = 0; c < columnCount; c++) {
                    row[c] = rs.getObject(c + 1);
                }
                rows.add(row);
            }
            return rows;

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

    // Columns: Status, Number of orders
    public ArrayList<Object[]> getOrderCountsByStatus() throws SQLException {
        String sql = "SELECT status, COUNT(*) FROM CustomerOrder GROUP BY status "
                + "ORDER BY FIELD(status, 'Pending', 'Confirmed', 'PartiallyShipped', 'Shipped', 'Cancelled')";
        return runQuery(sql, 2);
    }

    // Every product that has sellable (Available) stock. Columns: Product, Category, Units
    public ArrayList<Object[]> getSellableByProduct() throws SQLException {
        String sql = "SELECT p.name, COALESCE(p.category, ''), SUM(s.quantity) "
                + "FROM StockItem s JOIN Batch b ON s.batchId = b.batchId "
                + "JOIN Product p ON b.productId = p.productId "
                + "WHERE s.availabilityStatus = 'Available' AND s.quantity > 0 "
                + "GROUP BY p.productId, p.name, p.category ORDER BY p.name";
        return runQuery(sql, 3);
    }

    // Batches that are still curing, the earliest finish first.
    // Columns: Product, Units, Ready on
    public ArrayList<Object[]> getCuringSoon(int limit) throws SQLException {
        String sql = "SELECT p.name, SUM(s.quantity), b.curingEndDate "
                + "FROM Batch b JOIN Product p ON b.productId = p.productId "
                + "JOIN StockItem s ON s.batchId = b.batchId "
                + "WHERE b.curingStatus = 'Curing' AND s.availabilityStatus = 'Curing' "
                + "GROUP BY b.batchId, p.name, b.curingEndDate "
                + "ORDER BY b.curingEndDate, b.batchId LIMIT " + limit;
        ArrayList<Object[]> rows = runQuery(sql, 3);
        for (Object[] r : rows) {
            r[2] = String.valueOf(r[2]);
        }
        return rows;
    }

    // The newest ledger entries. Columns: When, Who, Action, Units, Product
    public ArrayList<Object[]> getRecentActivity(int limit) throws SQLException {
        String sql = "SELECT l.timestamp, u.name, l.actionType, l.quantityChange, p.name "
                + "FROM LedgerEntry l JOIN User u ON l.userId = u.userId "
                + "JOIN StockItem s ON l.itemId = s.itemId "
                + "JOIN Batch b ON s.batchId = b.batchId "
                + "JOIN Product p ON b.productId = p.productId "
                + "ORDER BY l.entryId DESC LIMIT " + limit;
        ArrayList<Object[]> rows = runQuery(sql, 5);
        for (Object[] r : rows) {
            r[0] = String.valueOf(r[0]).substring(0, 16);   // 2026-10-18 10:15
        }
        return rows;
    }

    // Money received from customers since the first day of this month.
    public double getPaymentsThisMonth() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM Payment "
                + "WHERE paymentDate >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')";
        ArrayList<Object[]> rows = runQuery(sql, 1);
        return ((Number) rows.get(0)[0]).doubleValue();
    }
}
