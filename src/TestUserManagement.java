import dao.UserDAO;
import dao.UserManagementDAO;
import exception.AccessDeniedException;
import exception.DuplicateUserException;
import exception.InvalidLoginException;
import model.SalesOfficer;
import model.User;
import model.WarehouseOperator;

public class TestUserManagement {
    public static void main(String[] args) {
        UserManagementDAO dao = new UserManagementDAO();
        UserDAO loginDAO = new UserDAO();

        int managerId = 1;    // upul.jayasuriya@yard.lk (the real manager)
        int operatorId = 4;   // pradeep (an operator, must be refused)

        // a timestamp makes the test emails unique, so the test can be re-run
        String stamp = String.valueOf(System.currentTimeMillis());
        String email = "test.officer." + stamp + "@yard.lk";

        try {
            // Test 1: manager adds a sales officer
            User newOfficer = new SalesOfficer(0, "Test Officer", email, "", 2, 500000.00);
            int newId = dao.addUser(managerId, newOfficer, "secret123");
            System.out.println("Test 1: added user with ID " + newId);

            // Test 2: the new user can log in
            User loggedIn = loginDAO.login(email, "secret123");
            System.out.println("Test 2: logged in as " + loggedIn.getDetails());

            // Test 3: the same email again is refused
            try {
                dao.addUser(managerId, new SalesOfficer(0, "Copy", email, "", 2, 0), "secret123");
                System.out.println("Test 3: this line should not print.");
            } catch (DuplicateUserException e) {
                System.out.println("Test 3 refused as expected: " + e.getMessage());
            }

            // Test 4: an operator may not add users
            try {
                dao.addUser(operatorId,
                        new WarehouseOperator(0, "Sneaky", "sneaky." + stamp + "@yard.lk", "", 1, "Zone A"),
                        "secret123");
                System.out.println("Test 4: this line should not print.");
            } catch (AccessDeniedException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // Test 5: a short password is refused
            try {
                dao.addUser(managerId,
                        new WarehouseOperator(0, "Short", "short." + stamp + "@yard.lk", "", 1, "Zone A"),
                        "123");
                System.out.println("Test 5: this line should not print.");
            } catch (IllegalArgumentException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

            // Test 6: update details and sales target
            dao.updateUserDetails(managerId, newId, "Test Officer Renamed", email);
            dao.updateSalesTarget(managerId, newId, 750000.00);
            System.out.println("Test 6: name and target updated.");

            // Test 7: reset the password; the old one stops working, the new one works
            dao.resetPassword(managerId, newId, "newsecret456");
            try {
                loginDAO.login(email, "secret123");
                System.out.println("Test 7: this line should not print.");
            } catch (InvalidLoginException e) {
                System.out.println("Test 7a old password refused: " + e.getMessage());
            }
            loginDAO.login(email, "newsecret456");
            System.out.println("Test 7b new password works.");

            // Test 8: deactivate, login is refused
            dao.setActive(managerId, newId, false);
            try {
                loginDAO.login(email, "newsecret456");
                System.out.println("Test 8: this line should not print.");
            } catch (InvalidLoginException e) {
                System.out.println("Test 8 refused as expected: " + e.getMessage());
            }

            // Test 9: re-activate, login works again
            dao.setActive(managerId, newId, true);
            loginDAO.login(email, "newsecret456");
            System.out.println("Test 9: account active again, login works.");

            // Test 10: a manager cannot deactivate themself
            try {
                dao.setActive(managerId, managerId, false);
                System.out.println("Test 10: this line should not print.");
            } catch (AccessDeniedException e) {
                System.out.println("Test 10 refused as expected: " + e.getMessage());
            }

            // Test 11: list all users
            java.util.List<User> all = dao.getAllUsers(managerId);
            System.out.println("Test 11: " + all.size() + " users. Last one: "
                    + all.get(all.size() - 1).getDetails());

            // tidy up: deactivate the test account (users are never deleted)
            dao.setActive(managerId, newId, false);
            System.out.println("Test account deactivated.");

        } catch (java.sql.SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}