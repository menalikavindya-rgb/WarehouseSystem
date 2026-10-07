package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import exception.InsufficientStockException;
import exception.OrderNotFoundException;
import exception.StockNotAvailableException;

public class ReservationDAO {

    // Reserves 'quantity' units of a stock item for an order.
    // Returns the new reservationId.
    public int reserveStock(int orderId, int itemId, int quantity, int userId)
            throws SQLException, OrderNotFoundException,
                   StockNotAvailableException, InsufficientStockException {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Reserved quantity must be greater than 0.");
        }

        String sqlOrder = "SELECT status FROM CustomerOrder WHERE orderId = ?";
        String sqlItem = "SELECT batchId, location, quantity, availabilityStatus "
                + "FROM StockItem WHERE itemId = ? FOR UPDATE";
        String sqlMarkReserved = "UPDATE StockItem SET availabilityStatus = 'Reserved' "
                + "WHERE itemId = ?";
        String sqlReduce = "UPDATE StockItem SET quantity = quantity - ? WHERE itemId = ?";
        String sqlNewItem = "INSERT INTO StockItem (batchId, location, quantity, availabilityStatus) "
                + "VALUES (?, ?, ?, 'Reserved')";
        String sqlReservation = "INSERT INTO Reservation (orderId, itemId, reservedQuantity, status) "
                + "VALUES (?, ?, ?, 'Active')";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean success = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start the transaction

            // Step 1: check the order exists and is still open
            ps = con.prepareStatement(sqlOrder);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new OrderNotFoundException("No order found with ID " + orderId);
            }
            String orderStatus = rs.getString("status");
            rs.close();
            ps.close();

            if (orderStatus.equals("Cancelled") || orderStatus.equals("Shipped")) {
                throw new OrderNotFoundException("Order " + orderId + " is " + orderStatus
                        + " and cannot take new reservations.");
            }

            // Step 2: read and lock the stock item
            ps = con.prepareStatement(sqlItem);
            ps.setInt(1, itemId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new StockNotAvailableException("Stock item " + itemId + " does not exist.");
            }
            int batchId = rs.getInt("batchId");
            String location = rs.getString("location");
            int itemQty = rs.getInt("quantity");
            String itemStatus = rs.getString("availabilityStatus");
            rs.close();
            ps.close();

            // Step 3: apply the rules
            if (!itemStatus.equals("Available")) {
                throw new StockNotAvailableException("Stock item " + itemId + " is "
                        + itemStatus + " and cannot be reserved.");
            }
            if (quantity > itemQty) {
                throw new InsufficientStockException("Stock item " + itemId + " has only "
                        + itemQty + " units, but " + quantity + " were requested.");
            }

            // Step 4: reserve the whole item, or split it
            LedgerDAO ledgerDAO = new LedgerDAO();
            int reservedItemId;

            if (quantity == itemQty) {
                // the order takes every unit, so the item itself becomes Reserved
                ps = con.prepareStatement(sqlMarkReserved);
                ps.setInt(1, itemId);
                ps.executeUpdate();
                ps.close();

                reservedItemId = itemId;
                ledgerDAO.addEntry(con, itemId, userId, "Reserved", 0,
                        "Reserved for order " + orderId);

            } else {
                // the order takes only some units: reduce the original item...
                ps = con.prepareStatement(sqlReduce);
                ps.setInt(1, quantity);
                ps.setInt(2, itemId);
                ps.executeUpdate();
                ps.close();

                // ...and create a new Reserved item for the reserved units
                ps = con.prepareStatement(sqlNewItem, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, batchId);
                ps.setString(2, location);
                ps.setInt(3, quantity);
                ps.executeUpdate();
                rs = ps.getGeneratedKeys();
                rs.next();
                reservedItemId = rs.getInt(1);
                rs.close();
                ps.close();

                ledgerDAO.addEntry(con, itemId, userId, "Adjusted", -quantity,
                        "Split for order " + orderId + ": " + quantity
                        + " units moved to item " + reservedItemId);
                ledgerDAO.addEntry(con, reservedItemId, userId, "Reserved", quantity,
                        "Reserved for order " + orderId + " (split from item " + itemId + ")");
            }

            // Step 5: save the reservation itself
            ps = con.prepareStatement(sqlReservation, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, orderId);
            ps.setInt(2, reservedItemId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            int reservationId = rs.getInt(1);

            con.commit();
            success = true;
            return reservationId;

        } finally {
            try {
                if (con != null && !success) {
                    con.rollback();     // anything went wrong, so undo every step
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