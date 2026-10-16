package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import exception.AccessDeniedException;

// A small helper for the DAOs that must check WHO is asking.
public class AccessChecker {

    // Returns the role of an ACTIVE user. Throws if the user does not exist or is inactive.
    public static String getActiveRole(Connection con, int userId)
            throws SQLException, AccessDeniedException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT role, isActive FROM User WHERE userId = ?");
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (!rs.next() || !rs.getBoolean("isActive")) {
                throw new AccessDeniedException("This account does not exist or is not active.");
            }
            return rs.getString("role");
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
    }

    // Stops the calling method unless the user is an active warehouse manager.
    public static void requireManager(Connection con, int userId)
            throws SQLException, AccessDeniedException {
        String role = getActiveRole(con, userId);
        if (!role.equals("WarehouseManager")) {
            throw new AccessDeniedException("Only a warehouse manager can do this.");
        }
    }
}
