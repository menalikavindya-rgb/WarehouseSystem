import java.sql.SQLException;
import java.util.ArrayList;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ReservationDAO;
import dao.ShippingDAO;
import dao.StockItemDAO;
import exception.PaymentRequiredException;
import exception.ReservationNotFoundException;
import model.StockItem;

public class TestShipping {

    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        PaymentDAO paymentDAO = new PaymentDAO();
        ShippingDAO shippingDAO = new ShippingDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            StockItem a = available.get(0);
            StockItem b = available.get(1);
            System.out.println("Physical stock before: " + stockDAO.getTotalPhysicalStock());

            // Set up: one order that reserves two whole stock items
            int orderId = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            int res1 = reservationDAO.reserveStock(orderId, a.getItemId(), a.getQuantity(), 16);
            int res2 = reservationDAO.reserveStock(orderId, b.getItemId(), b.getQuantity(), 16);
            System.out.println("Order " + orderId + " has reservations " + res1 + " and " + res2);

            // Test 1: no payment yet, so shipping must be refused
            try {
                shippingDAO.shipReservation(res1, 4);
                System.out.println("Test 1: this line should not print.");
            } catch (PaymentRequiredException e) {
                System.out.println("Test 1 refused as expected: " + e.getMessage());
            }

            // Test 2: make a small payment, then ship the first reservation
            paymentDAO.addPayment(orderId, 1.00);
            shippingDAO.shipReservation(res1, 4);
            System.out.println("Test 2 OK: reservation " + res1 + " shipped, order status: "
                    + orderDAO.getOrderStatus(orderId));

            // Test 3: ship the second reservation, so the whole order is shipped
            shippingDAO.shipReservation(res2, 4);
            System.out.println("Test 3 OK: reservation " + res2 + " shipped, order status: "
                    + orderDAO.getOrderStatus(orderId));

            // Test 4: shipping the first reservation again must be refused
            try {
                shippingDAO.shipReservation(res1, 4);
                System.out.println("Test 4: this line should not print.");
            } catch (ReservationNotFoundException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // Test 5: a reservation that does not exist
            try {
                shippingDAO.shipReservation(999999, 4);
                System.out.println("Test 5: this line should not print.");
            } catch (ReservationNotFoundException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

            System.out.println("Physical stock after:  " + stockDAO.getTotalPhysicalStock());
            System.out.println("Units shipped: " + (a.getQuantity() + b.getQuantity()));

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            // any other problem (a business-rule exception we did not expect)
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}