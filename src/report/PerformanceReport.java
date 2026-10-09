package report;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import dao.DBConnection;

public class PerformanceReport extends Report {

    public PerformanceReport(LocalDate startDate, LocalDate endDate) {
        super("Performance Report", startDate, endDate);
    }

    @Override
    public String generate() throws SQLException {
        StringBuilder sb = new StringBuilder(header());

        String sql = "SELECT u.name, u.role, COUNT(*) AS actions, "
                + "COALESCE(SUM(CASE WHEN l.actionType = 'Produced' THEN l.quantityChange ELSE 0 END), 0) AS produced, "
                + "COALESCE(SUM(CASE WHEN l.actionType = 'Shipped' THEN -l.quantityChange ELSE 0 END), 0) AS shipped, "
                + "COALESCE(SUM(CASE WHEN l.actionType = 'Damaged' THEN -l.quantityChange ELSE 0 END), 0) AS damaged "
                + "FROM LedgerEntry l JOIN User u ON u.userId = l.userId "
                + "WHERE DATE(l.timestamp) BETWEEN ? AND ? "
                + "GROUP BY u.userId, u.name, u.role ORDER BY actions DESC LIMIT 10";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(getStartDate()));
            ps.setDate(2, Date.valueOf(getEndDate()));
            rs = ps.executeQuery();

            sb.append("\nTop 10 staff by recorded stock actions\n");
            sb.append(String.format("  %-24s %-18s %7s %9s %8s %8s%n",
                    "Name", "Role", "Actions", "Produced", "Shipped", "Damaged"));
            while (rs.next()) {
                sb.append(String.format("  %-24s %-18s %7d %9d %8d %8d%n",
                        rs.getString("name"), rs.getString("role"), rs.getInt("actions"),
                        rs.getInt("produced"), rs.getInt("shipped"), rs.getInt("damaged")));
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