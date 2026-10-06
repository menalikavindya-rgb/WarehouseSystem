package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import model.Batch;
import java.time.LocalDate;
import java.util.ArrayList;

public class BatchDAO {

    // Saves a batch, its stock item and a ledger entry as ONE transaction.
    // Returns the new batchId.
    public int registerBatch(Batch batch, String location, int userId) throws SQLException {
        String sqlBatch = "INSERT INTO Batch (productId, productionDate, curingEndDate, quantity, curingStatus) "
                + "VALUES (?, ?, ?, ?, 'Curing')";
        String sqlStock = "INSERT INTO StockItem (batchId, location, quantity, availabilityStatus) "
                + "VALUES (?, ?, ?, 'Curing')";

        Connection con = null;
        PreparedStatement psBatch = null;
        PreparedStatement psStock = null;
        ResultSet keys = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start the transaction

            // Step 1: insert the batch
            psBatch = con.prepareStatement(sqlBatch, Statement.RETURN_GENERATED_KEYS);
            psBatch.setInt(1, batch.getProductId());
            psBatch.setDate(2, Date.valueOf(batch.getProductionDate()));
            psBatch.setDate(3, Date.valueOf(batch.getCuringEndDate()));
            psBatch.setInt(4, batch.getQuantity());
            psBatch.executeUpdate();

            keys = psBatch.getGeneratedKeys();
            keys.next();
            int batchId = keys.getInt(1);
            keys.close();

            // Step 2: insert the stock item for this batch
            psStock = con.prepareStatement(sqlStock, Statement.RETURN_GENERATED_KEYS);
            psStock.setInt(1, batchId);
            psStock.setString(2, location);
            psStock.setInt(3, batch.getQuantity());
            psStock.executeUpdate();

            keys = psStock.getGeneratedKeys();
            keys.next();
            int itemId = keys.getInt(1);

            // Step 3: write the ledger entry
            LedgerDAO ledgerDAO = new LedgerDAO();
            ledgerDAO.addEntry(con, itemId, userId, "Produced", batch.getQuantity(),
                    "New batch registered");

            con.commit();               // all three worked, so save them
            return batchId;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();     // something failed, so undo everything
                } catch (SQLException ex) {
                    System.out.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e;                    // let the caller know it failed

        } finally {
            try {
                if (keys != null) {
                    keys.close();
                }
                if (psBatch != null) {
                    psBatch.close();
                }
                if (psStock != null) {
                    psStock.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
        // Moves finished-curing batches to Ready and their stock items to Available.
    // Returns the number of batches updated.
    public int completeCuring(int userId, LocalDate today) throws SQLException {
        String sqlFindItems = "SELECT s.itemId FROM StockItem s "
                + "JOIN Batch b ON s.batchId = b.batchId "
                + "WHERE b.curingStatus = 'Curing' AND b.curingEndDate <= ? "
                + "AND s.availabilityStatus = 'Curing'";
        String sqlUpdateItem = "UPDATE StockItem SET availabilityStatus = 'Available' "
                + "WHERE itemId = ?";
        String sqlUpdateBatches = "UPDATE Batch SET curingStatus = 'Ready' "
                + "WHERE curingStatus = 'Curing' AND curingEndDate <= ?";

        Connection con = null;
        PreparedStatement psFind = null;
        PreparedStatement psItem = null;
        PreparedStatement psBatch = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);   // start the transaction

            // Step 1: find the stock items whose batch has finished curing
            psFind = con.prepareStatement(sqlFindItems);
            psFind.setDate(1, Date.valueOf(today));
            rs = psFind.executeQuery();

            ArrayList<Integer> itemIds = new ArrayList<Integer>();
            while (rs.next()) {
                itemIds.add(rs.getInt("itemId"));
            }
            rs.close();

            // Step 2: make each item Available and record it in the ledger
            psItem = con.prepareStatement(sqlUpdateItem);
            LedgerDAO ledgerDAO = new LedgerDAO();

            for (int i = 0; i < itemIds.size(); i++) {
                int itemId = itemIds.get(i);
                psItem.setInt(1, itemId);
                psItem.executeUpdate();
                ledgerDAO.addEntry(con, itemId, userId, "CuringComplete", 0,
                        "Curing complete (auto-update)");
            }

            // Step 3: mark the batches themselves as Ready
            psBatch = con.prepareStatement(sqlUpdateBatches);
            psBatch.setDate(1, Date.valueOf(today));
            int batchesUpdated = psBatch.executeUpdate();

            con.commit();
            return batchesUpdated;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e;

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (psFind != null) {
                    psFind.close();
                }
                if (psItem != null) {
                    psItem.close();
                }
                if (psBatch != null) {
                    psBatch.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
        // Marks Ready batches as Depleted when none of their stock is left in the yard.
    // Returns the number of batches updated.
    public int markDepletedBatches() throws SQLException {
        String sql = "UPDATE Batch SET curingStatus = 'Depleted' "
                + "WHERE curingStatus = 'Ready' "
                + "AND EXISTS (SELECT 1 FROM StockItem s WHERE s.batchId = Batch.batchId) "
                + "AND NOT EXISTS (SELECT 1 FROM StockItem s WHERE s.batchId = Batch.batchId "
                + "AND s.availabilityStatus IN ('Curing', 'Available', 'Reserved'))";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            return ps.executeUpdate();

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}