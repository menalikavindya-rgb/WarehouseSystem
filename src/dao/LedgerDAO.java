package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LedgerDAO {

    // Uses the connection passed in, so it can join a transaction started elsewhere
    public void addEntry(Connection con, int itemId, int userId, String actionType,
                         int quantityChange, String remarks) throws SQLException {
        String sql = "INSERT INTO LedgerEntry (itemId, userId, actionType, quantityChange, remarks) "
                + "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            ps.setString(3, actionType);
            ps.setInt(4, quantityChange);
            ps.setString(5, remarks);
            ps.executeUpdate();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }
}