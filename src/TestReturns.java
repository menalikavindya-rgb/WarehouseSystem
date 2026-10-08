import java.sql.SQLException;
import java.util.ArrayList;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.ReservationDAO;
import dao.ReturnDAO;
import dao.ShippingDAO;
import dao.StockItemDAO;
import exception.ReservationNotFoundException;
import exception.ReturnNotAllowedException;
import model.StockItem;

public class TestReturns {

    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        PaymentDAO paymentDAO = new PaymentDAO();
        ShippingDAO shippingDAO = new ShippingDAO();
        ReturnDAO returnDAO = new ReturnDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            // Pick two different Available items; the first must have at least 4 units
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            StockItem chosen = null;
            StockItem other = null;
            for (int i = 0; i < available.size(); i++) {
                if (chosen == null && available.get(i).getQuantity() >= 4) {
                    chosen = available.get(i);
                } else if (other == null) {
                    other = available.get(i);
                }
            }
            if (chosen == null || other == null) {
                System.out.println("Could not find suitable Available items.");
                return;
            }

            // Set up: an order, one reservation that is paid and shipped,
            // and a second reservation that stays Active (not shipped)
            int orderId = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            int shippedRes = reservationDAO.reserveStock(orderId, chosen.getItemId(), chosen.getQuantity(), 16);
            int activeRes = reservationDAO.reserveStock(orderId, other.getItemId(), other.getQuantity(), 16);
            paymentDAO.addPayment(orderId, 1.00);
            shippingDAO.shipReservation(shippedRes, 4);
            System.out.println("Shipped reservation " + shippedRes + " (" + chosen.getQuantity()
                    + " units); reservation " + activeRes + " is still Active");
            System.out.println("Physical stock after shipping: " + stockDAO.getTotalPhysicalStock());

            // Test 1: return 1 unit in good condition, so physical stock should go up by 1
            int item1 = returnDAO.returnGoods(shippedRes, 1, "Available", 4);
            System.out.println("Test 1 OK: new stock item " + item1 + " (Available). Physical stock: "
                    + stockDAO.getTotalPhysicalStock());

            // Test 2: return 1 unit in damaged condition, so physical stock should not change
            int item2 = returnDAO.returnGoods(shippedRes, 1, "Damaged", 4);
            System.out.println("Test 2 OK: new stock item " + item2 + " (Damaged). Physical stock: "
                    + stockDAO.getTotalPhysicalStock());

            // Test 3: return more units than can still be returned
            try {
                returnDAO.returnGoods(shippedRes, chosen.getQuantity(), "Available", 4);
                System.out.println("Test 3: this line should not print.");
            } catch (ReturnNotAllowedException e) {
                System.out.println("Test 3 refused as expected: " + e.getMessage());
            }

            // Test 4: a reservation that has not been shipped
            try {
                returnDAO.returnGoods(activeRes, 1, "Available", 4);
                System.out.println("Test 4: this line should not print.");
            } catch (ReturnNotAllowedException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // Test 5: an invalid condition
            try {
                returnDAO.returnGoods(shippedRes, 1, "Broken", 4);
                System.out.println("Test 5: this line should not print.");
            } catch (IllegalArgumentException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

            // Test 6: a reservation that does not exist
            try {
                returnDAO.returnGoods(999999, 1, "Available", 4);
                System.out.println("Test 6: this line should not print.");
            } catch (ReservationNotFoundException e) {
                System.out.println("Test 6 refused as expected: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}