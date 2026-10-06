import java.sql.SQLException;
import java.time.LocalDate;
import dao.BatchDAO;
import dao.StockItemDAO;

public class TestCuring {

    public static void main(String[] args) {
        BatchDAO batchDAO = new BatchDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            System.out.println("Before -> curing: " + stockDAO.getCuringStock()
                    + ", sellable: " + stockDAO.getSellableStock());

            int updated = batchDAO.completeCuring(4, LocalDate.now());
            System.out.println("Batches moved to Ready: " + updated);

            System.out.println("After  -> curing: " + stockDAO.getCuringStock()
                    + ", sellable: " + stockDAO.getSellableStock());

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}