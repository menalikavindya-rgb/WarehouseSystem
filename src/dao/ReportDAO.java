package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import exception.AccessDeniedException;
import report.Report;

public class ReportDAO {

    // Saves a record of a generated report. Only managers may do this.
    public int logReport(Report report, int userId) throws SQLException, AccessDeniedException {
        String sqlRole = "SELECT role FROM User WHERE userId = ?";
        String sqlInsert = "INSERT INTO Report (generatedBy, type, dateRangeStart, dateRangeEnd) "
                + "VALUES (?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            // Step 1: only a WarehouseManager may generate reports
            ps = con.prepareStatement(sqlRole);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (!rs.next() || !rs.getString("role").equals("WarehouseManager")) {
                throw new AccessDeniedException("Only a warehouse manager can generate reports.");
            }
            rs.close();
            ps.close();

            // Step 2: save the record
            ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, report.getType());
            ps.setDate(3, Date.valueOf(report.getStartDate()));
            ps.setDate(4, Date.valueOf(report.getEndDate()));
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);

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