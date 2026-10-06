import java.sql.SQLException;
import java.util.ArrayList;
import dao.StockItemDAO;
import model.StockItem;

public class TestStockDAO {

    public static void main(String[] args) {
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            ArrayList<StockItem> all = stockDAO.getAllStock();
            System.out.println("All stock items: " + all.size());

            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            System.out.println("Available items: " + available.size());

            System.out.println("Total physical stock: " + stockDAO.getTotalPhysicalStock());
            System.out.println("Stock in curing:      " + stockDAO.getCuringStock());
            System.out.println("True sellable stock:  " + stockDAO.getSellableStock());

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}