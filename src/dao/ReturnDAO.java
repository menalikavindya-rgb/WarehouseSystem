package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import exception.ReservationNotFoundException;
import exception.ReturnNotAllowedException;

public class ReturnDAO {

    // Records a return of 'quantity' units from a shipped reservation.
    // condition must be "Available" or "Damaged". Returns the new stock itemId.
    public int returnGoods(int reservationId, int quantity, String condition, int userId)
            throws SQLException, ReservationNotFoundException, ReturnNotAllowedException {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Returned quantity must be greater than 0.");
        }
        if (!condition.equals("Available") && !condition.equals("Damaged")) {
            throw new IllegalArgumentException("Condition must be Available or Damaged.");
        }

        String sqlReservation = "SELECT orderId, itemId, reservedQuantity, returnedQuantity, status "
                + "FROM Reservation WHERE reservationId = ? FOR UPDATE";
        String sqlOldItem = "SELECT batchId, location FROM StockItem WHERE itemId = ?";
        String sqlNewItem = "INSERT INTO StockItem (batchId, location, quantity, availabilityStatus) "
                + "VALUES (?, ?, ?, ?)";
        String sqlUpdateReservation = "UPDATE Reservation "
                + "SET returnedQuantity = returnedQuantity + ? WHERE reservationId = ?";

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
            int oldItemId = rs.getInt("itemId");
            int reservedQty = rs.getInt("reservedQuantity");
            int returnedQty = rs.getInt("returnedQuantity");
            String reservationStatus = rs.getString("status");
            rs.close();
            ps.close();

            // Step 2: check the return is allowed
            if (!reservationStatus.equals("Fulfilled")) {
                throw new ReturnNotAllowedException("Reservation " + reservationId + " is "
                        + reservationStatus + ", so nothing has been shipped to return.");
            }
            int stillReturnable = reservedQty - returnedQty;
            if (quantity > stillReturnable) {
                throw new ReturnNotAllowedException("Only " + stillReturnable
                        + " units can still be returned from reservation " + reservationId
                        + ", but " + quantity + " were requested.");
            }

            // Step 3: find the batch and location of the shipped stock
            ps = con.prepareStatement(sqlOldItem);
            ps.setInt(1, oldItemId);
            rs = ps.executeQuery();
            rs.next();
            int batchId = rs.getInt("batchId");
            String location = rs.getString("location");
            rs.close();
            ps.close();

            // Step 4: create a new stock item for the returned units
            ps = con.prepareStatement(sqlNewItem, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, batchId);
            ps.setString(2, location);
            ps.setInt(3, quantity);
            ps.setString(4, condition);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            int newItemId = rs.getInt(1);
            rs.close();
            ps.close();

            // Step 5: record it in the ledger
            LedgerDAO ledgerDAO = new LedgerDAO();
            ledgerDAO.addEntry(con, newItemId, userId, "Returned", quantity,
                    "Returned from order " + orderId + " (reservation " + reservationId + ")");
            if (condition.equals("Damaged")) {
                ledgerDAO.addEntry(con, newItemId, userId, "Damaged", -quantity,
                        "Returned in damaged condition, written off");
            }

            // Step 6: remember how many units have now been returned
            ps = con.prepareStatement(sqlUpdateReservation);
            ps.setInt(1, quantity);
            ps.setInt(2, reservationId);
            ps.executeUpdate();

            con.commit();
            success = true;
            return newItemId;

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