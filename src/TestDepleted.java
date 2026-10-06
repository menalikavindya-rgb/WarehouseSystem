import java.sql.SQLException;
import dao.BatchDAO;

public class TestDepleted {

    public static void main(String[] args) {
        BatchDAO batchDAO = new BatchDAO();

        try {
            int updated = batchDAO.markDepletedBatches();
            System.out.println("Batches marked Depleted: " + updated);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}