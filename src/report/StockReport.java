package report;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import dao.DBConnection;
import dao.StockItemDAO;

public class StockReport extends Report {

    public StockReport(LocalDate startDate, LocalDate endDate) {
        super("Stock Report", startDate, endDate);
    }

    @Override
    public String generate() throws SQLException {
        StringBuilder sb = new StringBuilder(header());
        StockItemDAO stockDAO = new StockItemDAO();

        sb.append("\nCurrent stock figures\n");
        sb.append("  Total physical stock: ").append(stockDAO.getTotalPhysicalStock()).append(" units\n");
        sb.append("  Stock in curing:      ").append(stockDAO.getCuringStock()).append(" units\n");
        sb.append("  True sellable stock:  ").append(stockDAO.getSellableStock()).append(" units\n");

        String sqlStatus = "SELECT availabilityStatus, COALESCE(SUM(quantity), 0) AS units "
                + "FROM StockItem GROUP BY availabilityStatus ORDER BY availabilityStatus";
        String sqlLedger = "SELECT actionType, COUNT(*) AS entries, COALESCE(SUM(quantityChange), 0) AS units "
                + "FROM LedgerEntry WHERE DATE(timestamp) BETWEEN ? AND ? "
                + "GROUP BY actionType ORDER BY actionType";
        String sqlBatches = "SELECT curingStatus, COUNT(*) AS batches FROM Batch "
                + "GROUP BY curingStatus ORDER BY curingStatus";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            sb.append("\nUnits by status\n");
            ps = con.prepareStatement(sqlStatus);
            rs = ps.executeQuery();
            while (rs.next()) {
                sb.append("  ").append(String.format("%-12s", rs.getString("availabilityStatus")))
                        .append(rs.getInt("units")).append(" units\n");
            }
            rs.close();
            ps.close();

            sb.append("\nLedger activity in the period\n");
            ps = con.prepareStatement(sqlLedger);
            ps.setDate(1, Date.valueOf(getStartDate()));
            ps.setDate(2, Date.valueOf(getEndDate()));
            rs = ps.executeQuery();
            while (rs.next()) {
                sb.append("  ").append(String.format("%-15s", rs.getString("actionType")))
                        .append(String.format("%6d entries", rs.getInt("entries")))
                        .append(String.format("%10d units (net)", rs.getInt("units"))).append("\n");
            }
            rs.close();
            ps.close();

            sb.append("\nBatches by curing status\n");
            ps = con.prepareStatement(sqlBatches);
            rs = ps.executeQuery();
            while (rs.next()) {
                sb.append("  ").append(String.format("%-12s", rs.getString("curingStatus")))
                        .append(rs.getInt("batches")).append(" batches\n");
            }

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