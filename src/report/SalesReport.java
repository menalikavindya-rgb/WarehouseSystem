package report;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import dao.DBConnection;

public class SalesReport extends Report {

    public SalesReport(LocalDate startDate, LocalDate endDate) {
        super("Sales Report", startDate, endDate);
    }

    // Runs a query that has two date parameters and returns one decimal number
    // (a small helper used for the three money totals below)
    private double sumBetweenDates(Connection con, String sql) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(getStartDate()));
            ps.setDate(2, Date.valueOf(getEndDate()));
            rs = ps.executeQuery();
            rs.next();
            return rs.getDouble("total");
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
    }

    @Override
    public String generate() throws SQLException {
        StringBuilder sb = new StringBuilder(header());

        String sqlOrders = "SELECT status, COUNT(*) AS orders FROM CustomerOrder "
                + "WHERE orderDate BETWEEN ? AND ? GROUP BY status ORDER BY status";
        String sqlValue = "SELECT COALESCE(SUM((r.reservedQuantity - r.returnedQuantity) * p.unitPrice), 0) AS total "
                + "FROM CustomerOrder o "
                + "JOIN Reservation r ON r.orderId = o.orderId AND r.status <> 'Cancelled' "
                + "JOIN StockItem s ON s.itemId = r.itemId "
                + "JOIN Batch b ON b.batchId = s.batchId "
                + "JOIN Product p ON p.productId = b.productId "
                + "WHERE o.orderDate BETWEEN ? AND ? AND o.status <> 'Cancelled'";
        String sqlPayments = "SELECT COALESCE(SUM(amount), 0) AS total FROM Payment "
                + "WHERE paymentDate BETWEEN ? AND ?";
        String sqlRefunds = "SELECT COALESCE(SUM(amount), 0) AS total FROM Refund "
                + "WHERE refundDate BETWEEN ? AND ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            sb.append("\nOrders placed in the period, by status\n");
            ps = con.prepareStatement(sqlOrders);
            ps.setDate(1, Date.valueOf(getStartDate()));
            ps.setDate(2, Date.valueOf(getEndDate()));
            rs = ps.executeQuery();
            int totalOrders = 0;
            while (rs.next()) {
                sb.append("  ").append(String.format("%-18s", rs.getString("status")))
                        .append(rs.getInt("orders")).append("\n");
                totalOrders = totalOrders + rs.getInt("orders");
            }
            rs.close();
            ps.close();
            sb.append("  Total orders:      ").append(totalOrders).append("\n");

            double orderValue = sumBetweenDates(con, sqlValue);
            double payments = sumBetweenDates(con, sqlPayments);
            double refunds = sumBetweenDates(con, sqlRefunds);

            sb.append("\nMoney\n");
            sb.append(String.format("  Value of non-cancelled orders: %,.2f%n", orderValue));
            sb.append(String.format("  Payments received:             %,.2f%n", payments));
            sb.append(String.format("  Refunds paid:                  %,.2f%n", refunds));
            sb.append(String.format("  Net received:                  %,.2f%n", payments - refunds));

            return sb.toString();

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