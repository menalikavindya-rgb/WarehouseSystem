package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import exception.PaymentRequiredException;
import exception.ReservationNotFoundException;
import exception.StockNotAvailableException;

public class ShippingDAO {

    // Ships the stock of one reservation
    public void shipReservation(int reservationId, int userId)
            throws SQLException, ReservationNotFoundException,
                   PaymentRequiredException, StockNotAvailableException {

        String sqlReservation = "SELECT orderId, itemId, reservedQuantity, status "
                + "FROM Reservation WHERE reservationId = ? FOR UPDATE";
        String sqlItem = "SELECT quantity, availabilityStatus "
                + "FROM StockItem WHERE itemId = ? FOR UPDATE";
        String sqlShipItem = "UPDATE StockItem SET availabilityStatus = 'Shipped' "
                + "WHERE itemId = ?";
        String sqlFulfil = "UPDATE Reservation SET status = 'Fulfilled' "
                + "WHERE reservationId = ?";
        String sqlCountActive = "SELECT COUNT(*) AS remaining FROM Reservation "
                + "WHERE orderId = ? AND status = 'Active'";
        String sqlOrderStatus = "UPDATE CustomerOrder SET status = ? WHERE orderId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean success = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start the transaction

            // Step 1: read and lock the reservation
            ps = con.prepareStatement(sqlReservation);
            ps.setInt(1, reservationId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ReservationNotFoundException("No reservation found with ID " + reservationId);
            }
            int orderId = rs.getInt("orderId");
            int itemId = rs.getInt("itemId");
            int reservedQty = rs.getInt("reservedQuantity");
            String reservationStatus = rs.getString("status");
            rs.close();
            ps.close();

            if (!reservationStatus.equals("Active")) {
                throw new ReservationNotFoundException("Reservation " + reservationId
                        + " is " + reservationStatus + " and cannot be shipped.");
            }

            // Step 2: the order needs at least one payment
            PaymentDAO paymentDAO = new PaymentDAO();
            if (paymentDAO.getAmountPaid(orderId) <= 0) {
                throw new PaymentRequiredException("Order " + orderId
                        + " has no payment yet, so its stock cannot be shipped.");
            }

            // Step 3: read and lock the stock item, and check it is ready to ship
            ps = con.prepareStatement(sqlItem);
            ps.setInt(1, itemId);
            rs = ps.executeQuery();
            rs.next();
            int itemQty = rs.getInt("quantity");
            String itemStatus = rs.getString("availabilityStatus");
            rs.close();
            ps.close();

            if (!itemStatus.equals("Reserved")) {
                throw new StockNotAvailableException("Stock item " + itemId + " is "
                        + itemStatus + ", but it must be Reserved to be shipped.");
            }
            if (itemQty != reservedQty) {
                throw new StockNotAvailableException("Stock item " + itemId + " holds "
                        + itemQty + " units, but this reservation is for " + reservedQty + ".");
            }

            // Step 4: mark the item Shipped and record it in the ledger
            ps = con.prepareStatement(sqlShipItem);
            ps.setInt(1, itemId);
            ps.executeUpdate();
            ps.close();

            LedgerDAO ledgerDAO = new LedgerDAO();
            ledgerDAO.addEntry(con, itemId, userId, "Shipped", -reservedQty,
                    "Shipped for order " + orderId);

            // Step 5: mark the reservation Fulfilled
            ps = con.prepareStatement(sqlFulfil);
            ps.setInt(1, reservationId);
            ps.executeUpdate();
            ps.close();

            // Step 6: update the order status
            ps = con.prepareStatement(sqlCountActive);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            rs.next();
            int remaining = rs.getInt("remaining");
            rs.close();
            ps.close();

            String newOrderStatus;
            if (remaining == 0) {
                newOrderStatus = "Shipped";
            } else {
                newOrderStatus = "PartiallyShipped";
            }

            ps = con.prepareStatement(sqlOrderStatus);
            ps.setString(1, newOrderStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();

            con.commit();
            success = true;

        } finally {
            try {
                if (con != null && !success) {
                    con.rollback();
                }
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error during cleanup: " + e.getMessage());
            }
        }
    }
}