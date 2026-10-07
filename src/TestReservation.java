import java.sql.SQLException;
import java.util.ArrayList;
import dao.OrderDAO;
import dao.ReservationDAO;
import dao.StockItemDAO;
import exception.CustomerNotFoundException;
import exception.InsufficientStockException;
import exception.OrderNotFoundException;
import exception.StockNotAvailableException;
import model.StockItem;

public class TestReservation {

    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            System.out.println("Sellable stock before: " + stockDAO.getSellableStock());

            // Pick two different Available items; the first must have at least 3 units
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            StockItem chosen = null;
            StockItem other = null;
            for (int i = 0; i < available.size(); i++) {
                if (chosen == null && available.get(i).getQuantity() >= 3) {
                    chosen = available.get(i);
                } else if (other == null) {
                    other = available.get(i);
                }
            }
            if (chosen == null || other == null) {
                System.out.println("Could not find suitable Available items.");
                return;
            }

            int itemId = chosen.getItemId();
            int itemQty = chosen.getQuantity();
            System.out.println("Using item " + itemId + " (" + itemQty + " units)");

            // Test 1: create an order for customer 1 (a sales officer, user 16, does the work)
            int orderId = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            System.out.println("Test 1 OK: order " + orderId + " created");

            // Test 2: reserve 1 unit, so the item is split
            int r1 = reservationDAO.reserveStock(orderId, itemId, 1, 16);
            System.out.println("Test 2 OK (split): reservation " + r1);

            // Test 3: reserve all the remaining units, so the item becomes Reserved
            int r2 = reservationDAO.reserveStock(orderId, itemId, itemQty - 1, 16);
            System.out.println("Test 3 OK (whole item): reservation " + r2);

            // Test 4: the same item is now Reserved, so it must be refused
            try {
                reservationDAO.reserveStock(orderId, itemId, 1, 16);
                System.out.println("Test 4: this line should not print.");
            } catch (StockNotAvailableException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // Test 5: a Curing item must be refused
            ArrayList<StockItem> curing = stockDAO.getItemsByStatus("Curing");
            try {
                reservationDAO.reserveStock(orderId, curing.get(0).getItemId(), 1, 16);
                System.out.println("Test 5: this line should not print.");
            } catch (StockNotAvailableException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

            // Test 6: asking for more units than the item holds
            try {
                reservationDAO.reserveStock(orderId, other.getItemId(), other.getQuantity() + 1, 16);
                System.out.println("Test 6: this line should not print.");
            } catch (InsufficientStockException e) {
                System.out.println("Test 6 refused as expected: " + e.getMessage());
            }

            // Test 7: an order that does not exist
            try {
                reservationDAO.reserveStock(999999, other.getItemId(), 1, 16);
                System.out.println("Test 7: this line should not print.");
            } catch (OrderNotFoundException e) {
                System.out.println("Test 7 refused as expected: " + e.getMessage());
            }

            System.out.println("Sellable stock after: " + stockDAO.getSellableStock());

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (CustomerNotFoundException e) {
            System.out.println("Customer problem: " + e.getMessage());
        } catch (OrderNotFoundException e) {
            System.out.println("Order problem: " + e.getMessage());
        } catch (StockNotAvailableException e) {
            System.out.println("Stock problem: " + e.getMessage());
        } catch (InsufficientStockException e) {
            System.out.println("Quantity problem: " + e.getMessage());
        }
    }
}