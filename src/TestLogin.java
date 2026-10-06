import java.sql.SQLException;
import dao.UserDAO;
import exception.InvalidLoginException;
import model.User;

public class TestLogin {

    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();

        tryLogin(userDAO, "upul.jayasuriya@yard.lk", "manager123");
        tryLogin(userDAO, "pradeep.perera.op4@yard.lk", "operator123");
        tryLogin(userDAO, "nuwan.de.silva.sales16@yard.lk", "sales123");
        tryLogin(userDAO, "upul.jayasuriya@yard.lk", "wrongpassword");
        tryLogin(userDAO, "nobody@yard.lk", "manager123");
        tryLogin(userDAO, "ravi.kulatunga@yard.lk", "abc123");
    }

    private static void tryLogin(UserDAO userDAO, String email, String password) {
        try {
            User user = userDAO.login(email, password);
            System.out.println("Login OK -> " + user.getDetails());
        } catch (InvalidLoginException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}