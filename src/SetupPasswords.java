import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;
import dao.DBConnection;

public class SetupPasswords {

    public static void main(String[] args) {
        setPassword(1, "manager123");
        setPassword(4, "operator123");
        setPassword(16, "sales123");
    }

    private static void setPassword(int userId, String plainPassword) {
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        String sql = "UPDATE User SET passwordHash = ? WHERE userId = ?";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            System.out.println("User " + userId + ": " + rows + " row(s) updated.");
        } catch (SQLException e) {
            System.out.println("Database error for user " + userId + ": " + e.getMessage());
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