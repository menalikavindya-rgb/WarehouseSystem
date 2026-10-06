import java.sql.SQLException;
import java.time.LocalDate;
import dao.BatchDAO;
import dao.ProductDAO;
import exception.ProductNotFoundException;
import model.Batch;
import model.Product;

public class TestBatch {

    public static void main(String[] args) {
        BatchDAO batchDAO = new BatchDAO();
        ProductDAO productDAO = new ProductDAO();

        // Test 1: a normal batch (user 4 is a warehouse operator)
        try {
            Product p = productDAO.getProductById(1);
            LocalDate today = LocalDate.now();
            LocalDate ready = today.plusDays(p.getDefaultCuringDurationDays());

            Batch b = new Batch(0, p.getProductId(), today, ready, 100, "Curing");
            int newId = batchDAO.registerBatch(b, "Zone-A-South", 4);
            System.out.println("Test 1 OK: batch " + newId + " registered, curing ends " + ready);

        } catch (ProductNotFoundException e) {
            System.out.println("Test 1 failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Test 1 database error: " + e.getMessage());
        }

        // Test 2: a product that does not exist, so the transaction must roll back
        try {
            LocalDate today = LocalDate.now();
            Batch bad = new Batch(0, 9999, today, today.plusDays(7), 50, "Curing");
            batchDAO.registerBatch(bad, "Zone-A-South", 4);
            System.out.println("Test 2: this line should not print.");

        } catch (SQLException e) {
            System.out.println("Test 2 rolled back as expected: " + e.getMessage());
        }
    }
}
