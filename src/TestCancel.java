import java.sql.SQLException;
import java.util.ArrayList;
import dao.OrderDAO;
import dao.ReservationDAO;
import dao.StockItemDAO;
import exception.OrderNotCancellableException;
import exception.OrderNotFoundException;
import model.StockItem;

public class TestCancel {

    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            StockItem a = available.get(0);
            StockItem b = available.get(1);

            int sellableStart = stockDAO.getSellableStock();
            System.out.println("Sellable stock at start: " + sellableStart);

            // Set up: an order that reserves two whole stock items
            int orderId = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            reservationDAO.reserveStock(orderId, a.getItemId(), a.getQuantity(), 16);
            reservationDAO.reserveStock(orderId, b.getItemId(), b.getQuantity(), 16);
            System.out.println("After reserving (order " + orderId + "): " + stockDAO.getSellableStock());

            // Test 1: cancel the order, so the stock must come back
            orderDAO.cancelOrder(orderId, 16);
            System.out.println("Test 1 OK: order status is " + orderDAO.getOrderStatus(orderId));
            System.out.println("After cancelling: " + stockDAO.getSellableStock()
                    + " (should equal " + sellableStart + ")");

            // Test 2: cancelling the same order again must be refused
            try {
                orderDAO.cancelOrder(orderId, 16);
                System.out.println("Test 2: this line should not print.");
            } catch (OrderNotCancellableException e) {
                System.out.println("Test 2 refused as expected: " + e.getMessage());
            }

            // Test 3: order 2 is a Shipped sample order, so it must be refused
            try {
                orderDAO.cancelOrder(2, 16);
                System.out.println("Test 3: this line should not print.");
            } catch (OrderNotCancellableException e) {
                System.out.println("Test 3 refused as expected: " + e.getMessage());
            }

            // Test 4: an order that does not exist
            try {
                orderDAO.cancelOrder(999999, 16);
                System.out.println("Test 4: this line should not print.");
            } catch (OrderNotFoundException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}