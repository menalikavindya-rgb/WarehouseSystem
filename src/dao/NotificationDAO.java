package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import exception.AccessDeniedException;
import exception.NotificationNotFoundException;
import model.Notification;

public class NotificationDAO {

    private static boolean isValidRole(String role) {
        return role.equals("WarehouseOperator") || role.equals("SalesOfficer")
                || role.equals("WarehouseManager");
    }

    // Used by the SYSTEM (for example when curing finishes): sends a message to every
    // active user with the given role. Returns how many users were notified.
    public int notifyRole(String role, String message) throws SQLException {
        if (!isValidRole(role)) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("A notification message cannot be empty.");
        }

        String sql = "INSERT INTO Notification (userId, message) "
                + "SELECT userId, ? FROM User WHERE role = ? AND isActive = TRUE";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, message.trim());
            ps.setString(2, role);
            return ps.executeUpdate();

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

    // A person sends a message to everyone with a role. The rules follow the ERD document:
    // a sales officer may only notify warehouse operators, a manager may notify anyone,
    // and warehouse operators cannot send notifications. Returns how many users were notified.
    public int sendManual(int senderId, String targetRole, String message)
            throws SQLException, AccessDeniedException {

        if (!isValidRole(targetRole)) {
            throw new IllegalArgumentException("Unknown role: " + targetRole);
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Please type a message.");
        }

        String sqlSender = "SELECT name, role, isActive FROM User WHERE userId = ?";
        String sqlSend = "INSERT INTO Notification (userId, message) "
                + "SELECT userId, ? FROM User WHERE role = ? AND isActive = TRUE";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            // Step 1: who is sending?
            ps = con.prepareStatement(sqlSender);
            ps.setInt(1, senderId);
            rs = ps.executeQuery();
            if (!rs.next() || !rs.getBoolean("isActive")) {
                throw new AccessDeniedException("This account does not exist or is not active.");
            }
            String senderName = rs.getString("name");
            String senderRole = rs.getString("role");
            rs.close();
            ps.close();

            // Step 2: is this sender allowed to notify this group?
            if (senderRole.equals("WarehouseOperator")) {
                throw new AccessDeniedException("Warehouse operators cannot send notifications.");
            }
            if (senderRole.equals("SalesOfficer") && !targetRole.equals("WarehouseOperator")) {
                throw new AccessDeniedException("Sales officers can only notify warehouse operators.");
            }

            // Step 3: send it, with the sender's name at the front
            ps = con.prepareStatement(sqlSend);
            ps.setString(1, "From " + senderName + ": " + message.trim());
            ps.setString(2, targetRole);
            return ps.executeUpdate();

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

    // All notifications of one user, newest first
    public ArrayList<Notification> getNotificationsForUser(int userId) throws SQLException {
        String sql = "SELECT notificationId, userId, message, timestamp, isRead "
                + "FROM Notification WHERE userId = ? "
                + "ORDER BY timestamp DESC, notificationId DESC LIMIT 300";

        ArrayList<Notification> list = new ArrayList<Notification>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                Notification n = new Notification(
                        rs.getInt("notificationId"),
                        rs.getInt("userId"),
                        rs.getString("message"),
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getBoolean("isRead"));
                list.add(n);
            }
            return list;

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

    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS unread FROM Notification "
                + "WHERE userId = ? AND isRead = FALSE";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt("unread");

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

    // Marks one of the user's own notifications as read
    public void markAsRead(int notificationId, int userId)
            throws SQLException, NotificationNotFoundException {
        String sql = "UPDATE Notification SET isRead = TRUE WHERE notificationId = ? AND userId = ?";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new NotificationNotFoundException("No notification found with ID " + notificationId);
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

    // Marks every unread notification of the user as read. Returns how many were changed.
    public int markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE Notification SET isRead = TRUE WHERE userId = ? AND isRead = FALSE";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            return ps.executeUpdate();

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
