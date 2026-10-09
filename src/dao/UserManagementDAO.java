package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import exception.AccessDeniedException;
import exception.DuplicateUserException;
import exception.UserNotFoundException;
import model.SalesOfficer;
import model.User;
import model.WarehouseManager;
import model.WarehouseOperator;

public class UserManagementDAO {

    // ---------------------------------------------------------------
    // Helper: only an ACTIVE warehouse manager may use this class.
    // The connection is passed in so the check runs inside the caller's transaction.
    // ---------------------------------------------------------------
    private void requireManager(Connection con, int actingUserId)
            throws SQLException, AccessDeniedException {
        String sql = "SELECT role, isActive FROM User WHERE userId = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, actingUserId);
            rs = ps.executeQuery();
            if (!rs.next() || !rs.getString("role").equals("WarehouseManager")
                    || !rs.getBoolean("isActive")) {
                throw new AccessDeniedException("Only an active Warehouse Manager can manage user accounts.");
            }
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Helper: simple input checks (name, email, password)
    // ---------------------------------------------------------------
    private void checkName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must not be empty.");
        }
    }

    private void checkEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email must contain an @ sign.");
        }
    }

    private void checkPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    // ---------------------------------------------------------------
    // Helper: is this email used by a user OTHER than excludeUserId?
    // (use excludeUserId = 0 when adding a new user)
    // ---------------------------------------------------------------
    private boolean emailInUse(Connection con, String email, int excludeUserId) throws SQLException {
        String sql = "SELECT userId FROM User WHERE email = ? AND userId <> ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setInt(2, excludeUserId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // 1. Add a user: one transaction over User + the matching child table.
    //    Returns the new userId.
    // ---------------------------------------------------------------
    public int addUser(int managerId, User user, String plainPassword)
            throws SQLException, AccessDeniedException, DuplicateUserException {

        checkName(user.getName());
        checkEmail(user.getEmail());
        checkPassword(plainPassword);

        Connection con = null;
        PreparedStatement psUser = null;
        PreparedStatement psChild = null;
        ResultSet keys = null;
        boolean success = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // Step 1: only an active manager may do this
            requireManager(con, managerId);

            // Step 2: the email must be new
            if (emailInUse(con, user.getEmail(), 0)) {
                throw new DuplicateUserException("A user with email " + user.getEmail() + " already exists.");
            }

            // Step 3: role and access level come from the kind of object we were given
            String role;
            int accessLevel;
            if (user instanceof WarehouseOperator) {
                role = "WarehouseOperator";
                accessLevel = 1;
            } else if (user instanceof SalesOfficer) {
                role = "SalesOfficer";
                accessLevel = 2;
            } else {
                role = "WarehouseManager";
                accessLevel = 3;
            }

            // Step 4: hash the password (cost 12, same as the rest of the project)
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

            // Step 5: insert the User row and read back the new id
            String sqlUser = "INSERT INTO User (name, email, passwordHash, role, accessLevel) "
                    + "VALUES (?, ?, ?, ?, ?)";
            psUser = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, user.getName().trim());
            psUser.setString(2, user.getEmail().trim());
            psUser.setString(3, hash);
            psUser.setString(4, role);
            psUser.setInt(5, accessLevel);
            psUser.executeUpdate();

            keys = psUser.getGeneratedKeys();
            keys.next();
            int newUserId = keys.getInt(1);

            // Step 6: insert the matching child row
            if (user instanceof WarehouseOperator) {
                WarehouseOperator op = (WarehouseOperator) user;
                psChild = con.prepareStatement(
                        "INSERT INTO WarehouseOperator (userId, assignedYardZone) VALUES (?, ?)");
                psChild.setInt(1, newUserId);
                psChild.setString(2, op.getAssignedYardZone());
            } else if (user instanceof SalesOfficer) {
                SalesOfficer so = (SalesOfficer) user;
                psChild = con.prepareStatement(
                        "INSERT INTO SalesOfficer (userId, salesTarget) VALUES (?, ?)");
                psChild.setInt(1, newUserId);
                psChild.setDouble(2, so.getSalesTarget());
            } else {
                psChild = con.prepareStatement("INSERT INTO WarehouseManager (userId) VALUES (?)");
                psChild.setInt(1, newUserId);
            }
            psChild.executeUpdate();

            con.commit();
            success = true;
            return newUserId;

        } finally {
            try {
                if (!success && con != null) {
                    con.rollback();
                }
                if (keys != null) {
                    keys.close();
                }
                if (psUser != null) {
                    psUser.close();
                }
                if (psChild != null) {
                    psChild.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // 2. Change a user's name and email
    // ---------------------------------------------------------------
    public void updateUserDetails(int managerId, int userId, String newName, String newEmail)
            throws SQLException, AccessDeniedException, UserNotFoundException, DuplicateUserException {

        checkName(newName);
        checkEmail(newEmail);

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            requireManager(con, managerId);

            if (emailInUse(con, newEmail.trim(), userId)) {
                throw new DuplicateUserException("A user with email " + newEmail + " already exists.");
            }

            ps = con.prepareStatement("UPDATE User SET name = ?, email = ? WHERE userId = ?");
            ps.setString(1, newName.trim());
            ps.setString(2, newEmail.trim());
            ps.setInt(3, userId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new UserNotFoundException("No user with ID " + userId + ".");
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

    // ---------------------------------------------------------------
    // 3. Reset a password (the new one is hashed before it is stored)
    // ---------------------------------------------------------------
    public void resetPassword(int managerId, int userId, String newPlainPassword)
            throws SQLException, AccessDeniedException, UserNotFoundException {

        checkPassword(newPlainPassword);

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            requireManager(con, managerId);

            String hash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(12));
            ps = con.prepareStatement("UPDATE User SET passwordHash = ? WHERE userId = ?");
            ps.setString(1, hash);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new UserNotFoundException("No user with ID " + userId + ".");
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

    // ---------------------------------------------------------------
    // 4. Deactivate or re-activate an account (nothing is ever deleted,
    //    so the ledger history keeps pointing at a real user)
    // ---------------------------------------------------------------
    public void setActive(int managerId, int userId, boolean active)
            throws SQLException, AccessDeniedException, UserNotFoundException {

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            requireManager(con, managerId);

            // a manager cannot deactivate their own account,
            // which also guarantees at least one active manager always remains
            if (!active && managerId == userId) {
                throw new AccessDeniedException("You cannot deactivate your own account.");
            }

            ps = con.prepareStatement("UPDATE User SET isActive = ? WHERE userId = ?");
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new UserNotFoundException("No user with ID " + userId + ".");
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

    // ---------------------------------------------------------------
    // 5. Change a sales officer's monthly target
    // ---------------------------------------------------------------
    public void updateSalesTarget(int managerId, int userId, double newTarget)
            throws SQLException, AccessDeniedException, UserNotFoundException {

        if (newTarget < 0) {
            throw new IllegalArgumentException("Sales target cannot be negative.");
        }

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            requireManager(con, managerId);

            ps = con.prepareStatement("UPDATE SalesOfficer SET salesTarget = ? WHERE userId = ?");
            ps.setDouble(1, newTarget);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new UserNotFoundException("User " + userId + " is not a sales officer.");
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

    // ---------------------------------------------------------------
    // 6. List all users (for the Users table on the manager screen)
    // ---------------------------------------------------------------
    public List<User> getAllUsers(int managerId) throws SQLException, AccessDeniedException {
        String sql = "SELECT u.userId, u.name, u.email, u.passwordHash, u.role, u.accessLevel, u.isActive, "
                + "o.assignedYardZone, s.salesTarget "
                + "FROM User u "
                + "LEFT JOIN WarehouseOperator o ON u.userId = o.userId "
                + "LEFT JOIN SalesOfficer s ON u.userId = s.userId "
                + "ORDER BY u.userId";

        List<User> users = new ArrayList<User>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            requireManager(con, managerId);

            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("userId");
                String name = rs.getString("name");
                String email = rs.getString("email");
                String hash = rs.getString("passwordHash");
                int level = rs.getInt("accessLevel");
                String role = rs.getString("role");

                User u;
                if (role.equals("WarehouseOperator")) {
                    u = new WarehouseOperator(id, name, email, hash, level, rs.getString("assignedYardZone"));
                } else if (role.equals("SalesOfficer")) {
                    u = new SalesOfficer(id, name, email, hash, level, rs.getDouble("salesTarget"));
                } else {
                    u = new WarehouseManager(id, name, email, hash, level);
                }
                u.setActive(rs.getBoolean("isActive"));
                users.add(u);
            }
            return users;

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