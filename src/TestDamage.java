import java.sql.SQLException;
import java.util.ArrayList;
import dao.DamageDAO;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ReservationDAO;
import dao.StockItemDAO;
import exception.InsufficientStockException;
import exception.StockNotAvailableException;
import model.StockItem;

public class TestDamage {

    public static void main(String[] args) {
        DamageDAO damageDAO = new DamageDAO();
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        PaymentDAO paymentDAO = new PaymentDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            // Collect Available items with at least 3 units (we need four of them)
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            ArrayList<StockItem> big = new ArrayList<StockItem>();
            for (int i = 0; i < available.size(); i++) {
                if (available.get(i).getQuantity() >= 3) {
                    big.add(available.get(i));
                }
            }
            if (big.size() < 4) {
                System.out.println("Not enough Available items with 3+ units.");
                return;
            }
            StockItem a = big.get(0);
            StockItem b = big.get(1);
            StockItem c = big.get(2);
            StockItem d = big.get(3);

            // Test 1: damage 1 unit of an Available item (partial)
            int physicalBefore = stockDAO.getTotalPhysicalStock();
            int damaged1 = damageDAO.logDamage(a.getItemId(), 1, "Cracked during loading", 4);
            System.out.println("Test 1 OK: new Damaged item " + damaged1 + ". Physical stock "
                    + physicalBefore + " -> " + stockDAO.getTotalPhysicalStock());

            // Test 2: damage 1 unit of a Curing item (partial)
            ArrayList<StockItem> curing = stockDAO.getItemsByStatus("Curing");
            StockItem curingItem = null;
            for (int i = 0; i < curing.size(); i++) {
                if (curingItem == null && curing.get(i).getQuantity() >= 2) {
                    curingItem = curing.get(i);
                }
            }
            if (curingItem != null) {
                int curingBefore = stockDAO.getCuringStock();
                damageDAO.logDamage(curingItem.getItemId(), 1, "Cracked while curing", 4);
                System.out.println("Test 2 OK: curing stock " + curingBefore + " -> "
                        + stockDAO.getCuringStock());
            }

            // Test 3: damage 1 unit of a Reserved item, then allocate replacement stock
            int order1 = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            reservationDAO.reserveStock(order1, b.getItemId(), b.getQuantity(), 16);
            double totalBefore = paymentDAO.getOrderTotal(order1);
            damageDAO.logDamage(b.getItemId(), 1, "Dropped by forklift", 4);
            double totalAfter = paymentDAO.getOrderTotal(order1);
            System.out.println("Test 3 OK: order " + order1 + " total " + totalBefore + " -> " + totalAfter);

            reservationDAO.reserveStock(order1, c.getItemId(), 1, 16);
            System.out.println("   after the sales officer reserved 1 replacement unit: total "
                    + paymentDAO.getOrderTotal(order1));

            // Test 4: damage every unit of a Reserved item, so its reservation is cancelled
            int order2 = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            reservationDAO.reserveStock(order2, d.getItemId(), d.getQuantity(), 16);
            damageDAO.logDamage(d.getItemId(), d.getQuantity(), "Flooded storage area", 4);
            System.out.println("Test 4 OK: order " + order2 + " total is now "
                    + paymentDAO.getOrderTotal(order2) + " (reservation cancelled)");

            // Test 5: more units than the item holds
            try {
                damageDAO.logDamage(c.getItemId(), 100000, "Too many", 4);
                System.out.println("Test 5: this line should not print.");
            } catch (InsufficientStockException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

            // Test 6: stock that is already Damaged
            try {
                damageDAO.logDamage(damaged1, 1, "Again", 4);
                System.out.println("Test 6: this line should not print.");
            } catch (StockNotAvailableException e) {
                System.out.println("Test 6 refused as expected: " + e.getMessage());
            }

            // Test 7: no reason given
            try {
                damageDAO.logDamage(c.getItemId(), 1, "  ", 4);
                System.out.println("Test 7: this line should not print.");
            } catch (IllegalArgumentException e) {
                System.out.println("Test 7 refused as expected: " + e.getMessage());
            }

            // Test 8: a stock item that does not exist
            try {
                damageDAO.logDamage(999999, 1, "Missing", 4);
                System.out.println("Test 8: this line should not print.");
            } catch (StockNotAvailableException e) {
                System.out.println("Test 8 refused as expected: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}