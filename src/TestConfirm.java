import java.sql.SQLException;
import java.util.ArrayList;
import dao.OrderDAO;
import dao.ReservationDAO;
import dao.StockItemDAO;
import exception.OrderNotConfirmableException;
import exception.OrderNotFoundException;
import model.StockItem;

public class TestConfirm {

    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            StockItem a = available.get(0);

            int orderId = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            System.out.println("Order " + orderId + " created, status: " + orderDAO.getOrderStatus(orderId));

            // Test 1: no reserved stock yet, so confirming must be refused
            try {
                orderDAO.confirmOrder(orderId);
                System.out.println("Test 1: this line should not print.");
            } catch (OrderNotConfirmableException e) {
                System.out.println("Test 1 refused as expected: " + e.getMessage());
            }

            // Test 2: reserve stock, then confirm
            reservationDAO.reserveStock(orderId, a.getItemId(), a.getQuantity(), 16);
            orderDAO.confirmOrder(orderId);
            System.out.println("Test 2 OK: status is now " + orderDAO.getOrderStatus(orderId));

            // Test 3: confirming again must be refused
            try {
                orderDAO.confirmOrder(orderId);
                System.out.println("Test 3: this line should not print.");
            } catch (OrderNotConfirmableException e) {
                System.out.println("Test 3 refused as expected: " + e.getMessage());
            }

            // Test 4: an order that does not exist
            try {
                orderDAO.confirmOrder(999999);
                System.out.println("Test 4: this line should not print.");
            } catch (OrderNotFoundException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // Clean up: a Confirmed order can still be cancelled, which releases the stock
            orderDAO.cancelOrder(orderId, 16);
            System.out.println("Cleaned up: status is now " + orderDAO.getOrderStatus(orderId));

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}