package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import exception.InsufficientStockException;
import exception.StockNotAvailableException;

public class DamageDAO {

    // Logs 'quantity' units of a stock item as damaged.
    // Returns the itemId of the stock row that is now Damaged.
    public int logDamage(int itemId, int quantity, String reason, int userId)
            throws SQLException, StockNotAvailableException, InsufficientStockException {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Damaged quantity must be greater than 0.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason for the damage is required.");
        }

        String sqlItem = "SELECT batchId, location, quantity, availabilityStatus "
                + "FROM StockItem WHERE itemId = ? FOR UPDATE";
        String sqlReservation = "SELECT reservationId, reservedQuantity FROM Reservation "
                + "WHERE itemId = ? AND status = 'Active' FOR UPDATE";
        String sqlWholeDamaged = "UPDATE StockItem SET availabilityStatus = 'Damaged' "
                + "WHERE itemId = ?";
        String sqlReduce = "UPDATE StockItem SET quantity = quantity - ? WHERE itemId = ?";
        String sqlNewItem = "INSERT INTO StockItem (batchId, location, quantity, availabilityStatus) "
                + "VALUES (?, ?, ?, 'Damaged')";
        String sqlCancelReservation = "UPDATE Reservation SET status = 'Cancelled' "
                + "WHERE reservationId = ?";
        String sqlShrinkReservation = "UPDATE Reservation "
                + "SET reservedQuantity = reservedQuantity - ? WHERE reservationId = ?";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean success = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start the transaction

            // Step 1: read and lock the stock item
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

            // Step 2: check the rules
            if (!itemStatus.equals("Curing") && !itemStatus.equals("Available")
                    && !itemStatus.equals("Reserved")) {
                throw new StockNotAvailableException("Stock item " + itemId + " is "
                        + itemStatus + ", so damage cannot be logged on it.");
            }
            if (quantity > itemQty) {
                throw new InsufficientStockException("Stock item " + itemId + " has only "
                        + itemQty + " units, but " + quantity + " were reported damaged.");
            }

            // Step 3: for Reserved stock, find the one reservation that is affected
            int reservationId = 0;
            if (itemStatus.equals("Reserved")) {
                ps = con.prepareStatement(sqlReservation);
                ps.setInt(1, itemId);
                rs = ps.executeQuery();

                boolean found = rs.next();
                int reservedQty = 0;
                boolean onlyOne = true;
                if (found) {
                    reservationId = rs.getInt("reservationId");
                    reservedQty = rs.getInt("reservedQuantity");
                    onlyOne = !rs.next();
                }
                rs.close();
                ps.close();

                if (!found || !onlyOne || reservedQty != itemQty) {
                    throw new StockNotAvailableException("The reservations on stock item " + itemId
                            + " do not match its quantity, so the damage cannot be applied automatically.");
                }
            }

            // Step 4: mark the damaged units
            LedgerDAO ledgerDAO = new LedgerDAO();
            int damagedItemId;

            if (quantity == itemQty) {
                // every unit is damaged, so the item itself becomes Damaged
                ps = con.prepareStatement(sqlWholeDamaged);
                ps.setInt(1, itemId);
                ps.executeUpdate();
                ps.close();

                damagedItemId = itemId;
                ledgerDAO.addEntry(con, itemId, userId, "Damaged", -quantity, reason.trim());

            } else {
                // only some units are damaged: reduce the original, create a Damaged row
                ps = con.prepareStatement(sqlReduce);
                ps.setInt(1, quantity);
                ps.setInt(2, itemId);
                ps.executeUpdate();
                ps.close();

                ps = con.prepareStatement(sqlNewItem, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, batchId);
                ps.setString(2, location);
                ps.setInt(3, quantity);
                ps.executeUpdate();
                rs = ps.getGeneratedKeys();
                rs.next();
                damagedItemId = rs.getInt(1);
                rs.close();
                ps.close();

                ledgerDAO.addEntry(con, itemId, userId, "Damaged", -quantity,
                        quantity + " units damaged, moved to item " + damagedItemId
                        + ": " + reason.trim());
                ledgerDAO.addEntry(con, damagedItemId, userId, "Adjusted", 0,
                        "Damaged units split from item " + itemId);
            }

            // Step 5: for Reserved stock, shrink or cancel the reservation
            if (itemStatus.equals("Reserved")) {
                if (quantity == itemQty) {
                    ps = con.prepareStatement(sqlCancelReservation);
                    ps.setInt(1, reservationId);
                } else {
                    ps = con.prepareStatement(sqlShrinkReservation);
                    ps.setInt(1, quantity);
                    ps.setInt(2, reservationId);
                }
                ps.executeUpdate();
            }

            con.commit();
            success = true;
            return damagedItemId;

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