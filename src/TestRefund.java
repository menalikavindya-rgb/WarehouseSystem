import java.sql.SQLException;
import java.util.ArrayList;
import dao.OrderDAO;
import dao.PaymentDAO;
import dao.RefundDAO;
import dao.ReservationDAO;
import dao.ReturnDAO;
import dao.ShippingDAO;
import dao.StockItemDAO;
import exception.OrderNotFoundException;
import exception.RefundNotAllowedException;
import model.StockItem;

public class TestRefund {

    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        ReservationDAO reservationDAO = new ReservationDAO();
        PaymentDAO paymentDAO = new PaymentDAO();
        ShippingDAO shippingDAO = new ShippingDAO();
        ReturnDAO returnDAO = new ReturnDAO();
        RefundDAO refundDAO = new RefundDAO();
        StockItemDAO stockDAO = new StockItemDAO();

        try {
            ArrayList<StockItem> available = stockDAO.getItemsByStatus("Available");
            StockItem a = available.get(0);
            StockItem b = available.get(1);

            // ---------- Part 1: a cancelled order ----------
            int order1 = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            reservationDAO.reserveStock(order1, a.getItemId(), a.getQuantity(), 16);
            paymentDAO.addPayment(order1, 500.00);
            orderDAO.cancelOrder(order1, 16);
            System.out.println("Order " + order1 + " paid 500.00 and then cancelled");

            // Test 1: refund 200 for the cancelled order
            refundDAO.issueRefund(order1, 200.00, "OrderCancelled", 1);
            System.out.println("Test 1 OK: refunded 200.00");

            // Test 2: refund 400 when only 300 is left, which must be refused
            try {
                refundDAO.issueRefund(order1, 400.00, "OrderCancelled", 1);
                System.out.println("Test 2: this line should not print.");
            } catch (RefundNotAllowedException e) {
                System.out.println("Test 2 refused as expected: " + e.getMessage());
            }

            // Test 3: refund the remaining 300
            refundDAO.issueRefund(order1, 300.00, "OrderCancelled", 1);
            System.out.println("Test 3 OK: refunded 300.00, amount paid now "
                    + paymentDAO.getAmountPaid(order1));

            // Test 4: a goods-returned refund on an order with no returns
            try {
                refundDAO.issueRefund(order1, 1.00, "GoodsReturned", 1);
                System.out.println("Test 4: this line should not print.");
            } catch (RefundNotAllowedException e) {
                System.out.println("Test 4 refused as expected: " + e.getMessage());
            }

            // ---------- Part 2: returned goods ----------
            int order2 = orderDAO.createOrder(1, "No. 10, Temple Road, Kandy");
            int res2 = reservationDAO.reserveStock(order2, b.getItemId(), b.getQuantity(), 16);
            double total = paymentDAO.getOrderTotal(order2);
            paymentDAO.addPayment(order2, total);
            shippingDAO.shipReservation(res2, 4);
            returnDAO.returnGoods(res2, 1, "Available", 4);

            double newTotal = paymentDAO.getOrderTotal(order2);
            double surplus = Math.round((paymentDAO.getAmountPaid(order2) - newTotal) * 100.0) / 100.0;
            System.out.println("Order " + order2 + ": paid " + total + ", after returning 1 unit "
                    + "the order is worth " + newTotal + ", surplus " + surplus);

            // Test 5: refund more than the surplus, which must be refused
            try {
                refundDAO.issueRefund(order2, surplus + 1.00, "GoodsReturned", 1);
                System.out.println("Test 5: this line should not print.");
            } catch (RefundNotAllowedException e) {
                System.out.println("Test 5 refused as expected: " + e.getMessage());
            }

            // Test 6: refund exactly the surplus
            refundDAO.issueRefund(order2, surplus, "GoodsReturned", 1);
            System.out.println("Test 6 OK: refunded " + surplus + ", balance now "
                    + paymentDAO.getOutstandingBalance(order2));

            // Test 7: an OrderCancelled refund on an order that is not cancelled
            try {
                refundDAO.issueRefund(order2, 1.00, "OrderCancelled", 1);
                System.out.println("Test 7: this line should not print.");
            } catch (RefundNotAllowedException e) {
                System.out.println("Test 7 refused as expected: " + e.getMessage());
            }

            // Test 8: an order that does not exist
            try {
                refundDAO.issueRefund(999999, 1.00, "OrderCancelled", 1);
                System.out.println("Test 8: this line should not print.");
            } catch (OrderNotFoundException e) {
                System.out.println("Test 8 refused as expected: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected problem: " + e.getMessage());
        }
    }
}