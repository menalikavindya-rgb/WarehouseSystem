package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;
import exception.InvalidLoginException;
import model.User;
import model.WarehouseOperator;
import model.SalesOfficer;
import model.WarehouseManager;

public class UserDAO {

    public User login(String email, String plainPassword) throws SQLException, InvalidLoginException {
        String sql = "SELECT u.userId, u.name, u.email, u.passwordHash, u.role, u.accessLevel, "
                + "o.assignedYardZone, s.salesTarget "
                + "FROM User u "
                + "LEFT JOIN WarehouseOperator o ON u.userId = o.userId "
                + "LEFT JOIN SalesOfficer s ON u.userId = s.userId "
                + "WHERE u.email = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();

            // Step 1: is there a user with this email?
            if (!rs.next()) {
                throw new InvalidLoginException("Invalid email or password.");
            }

            // Step 2: does the typed password match the stored hash?
            String storedHash = rs.getString("passwordHash");
            boolean passwordMatches;
            try {
                passwordMatches = BCrypt.checkpw(plainPassword, storedHash);
            } catch (IllegalArgumentException e) {
                // the stored hash is not a valid bcrypt hash (placeholder data)
                passwordMatches = false;
            }

            if (!passwordMatches) {
                throw new InvalidLoginException("Invalid email or password.");
            }

            // Step 3: build the correct kind of user object
            int userId = rs.getInt("userId");
            String name = rs.getString("name");
            String userEmail = rs.getString("email");
            int accessLevel = rs.getInt("accessLevel");
            String role = rs.getString("role");

            if (role.equals("WarehouseOperator")) {
                return new WarehouseOperator(userId, name, userEmail, storedHash,
                        accessLevel, rs.getString("assignedYardZone"));
            } else if (role.equals("SalesOfficer")) {
                return new SalesOfficer(userId, name, userEmail, storedHash,
                        accessLevel, rs.getDouble("salesTarget"));
            } else {
                return new WarehouseManager(userId, name, userEmail, storedHash, accessLevel);
            }

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