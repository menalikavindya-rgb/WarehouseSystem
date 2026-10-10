package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

// Read-only lists for the screens' tables. Nothing here changes the database.
// Each method returns rows (one Object[] per table row); the screen supplies the column titles.
public class ViewDAO {

    // The part of the order-total calculation that is reused by getOrderRows.
    private static final String ITEM_JOINS =
            "JOIN StockItem s ON r.itemId = s.itemId "
            + "JOIN Batch b ON s.batchId = b.batchId "
            + "JOIN Product p ON b.productId = p.productId ";

    // Runs a query with the given parameters and returns every row as an Object[].
    // This private helper keeps the public methods short.
    private ArrayList<Object[]> runQuery(String sql, Object[] params, int columnCount) throws SQLException {
        ArrayList<Object[]> rows = new ArrayList<Object[]>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            rs = ps.executeQuery();

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int c = 0; c < columnCount; c++) {
                    row[c] = rs.getObject(c + 1);
                }
                rows.add(row);
            }
            return rows;

        } finally {
            try {
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
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Stock
    // ---------------------------------------------------------------

    // Columns: Item ID, Batch, Product, Location, Units, Status, Curing ends
    // status = null means all statuses
    public ArrayList<Object[]> getStockRows(String status) throws SQLException {
        String sql = "SELECT s.itemId, s.batchId, p.name, s.location, s.quantity, "
                + "s.availabilityStatus, b.curingEndDate "
                + "FROM StockItem s JOIN Batch b ON s.batchId = b.batchId "
                + "JOIN Product p ON b.productId = p.productId ";
        Object[] params;
        if (status == null) {
            params = new Object[] {};
        } else {
            sql += "WHERE s.availabilityStatus = ? ";
            params = new Object[] { status };
        }
        sql += "ORDER BY s.itemId DESC LIMIT 1000";
        return runQuery(sql, params, 7);
    }

    // Stock that can still be marked damaged (not shipped, not already damaged).
    // Columns: same as getStockRows
    public ArrayList<Object[]> getDamageableRows() throws SQLException {
        String sql = "SELECT s.itemId, s.batchId, p.name, s.location, s.quantity, "
                + "s.availabilityStatus, b.curingEndDate "
                + "FROM StockItem s JOIN Batch b ON s.batchId = b.batchId "
                + "JOIN Product p ON b.productId = p.productId "
                + "WHERE s.availabilityStatus IN ('Curing', 'Available', 'Reserved') AND s.quantity > 0 "
                + "ORDER BY s.itemId DESC LIMIT 1000";
        return runQuery(sql, new Object[] {}, 7);
    }

    // Stock that a sales officer may allocate.
    // Columns: Item ID, Product, Category, Location, Units, Unit price (Rs.)
    public ArrayList<Object[]> getAvailableRows() throws SQLException {
        String sql = "SELECT s.itemId, p.name, p.category, s.location, s.quantity, p.unitPrice "
                + "FROM StockItem s JOIN Batch b ON s.batchId = b.batchId "
                + "JOIN Product p ON b.productId = p.productId "
                + "WHERE s.availabilityStatus = 'Available' AND s.quantity > 0 "
                + "ORDER BY p.name, s.itemId LIMIT 1000";
        return runQuery(sql, new Object[] {}, 6);
    }

    // Columns: Status, Stock rows, Units
    public ArrayList<Object[]> getUnitsByStatus() throws SQLException {
        String sql = "SELECT availabilityStatus, COUNT(*), COALESCE(SUM(quantity), 0) "
                + "FROM StockItem GROUP BY availabilityStatus ORDER BY availabilityStatus";
        return runQuery(sql, new Object[] {}, 3);
    }

    // The newest warehouse settings: {maxCapacity, alertThreshold}, or null if there are none.
    public int[] getLatestCapacity() throws SQLException {
        String sql = "SELECT maxCapacity, alertThreshold FROM WarehouseConfiguration "
                + "ORDER BY configId DESC LIMIT 1";
        ArrayList<Object[]> rows = runQuery(sql, new Object[] {}, 2);
        if (rows.isEmpty()) {
            return null;
        }
        return new int[] { ((Number) rows.get(0)[0]).intValue(), ((Number) rows.get(0)[1]).intValue() };
    }

    // ---------------------------------------------------------------
    // Orders, reservations, payments
    // ---------------------------------------------------------------

    // Columns: Order, Customer, Date, Status, Total, Paid, Balance
    // status = null means all orders. Money columns are Double.
    public ArrayList<Object[]> getOrderRows(String status) throws SQLException {
        String sql = "SELECT o.orderId, c.name, o.orderDate, o.status, "
                + "COALESCE((SELECT SUM((r.reservedQuantity - r.returnedQuantity) * p.unitPrice) "
                + "          FROM Reservation r " + ITEM_JOINS
                + "          WHERE r.orderId = o.orderId AND r.status <> 'Cancelled'), 0) AS total, "
                + "COALESCE((SELECT SUM(amount) FROM Payment WHERE orderId = o.orderId), 0) "
                + "- COALESCE((SELECT SUM(amount) FROM Refund WHERE orderId = o.orderId), 0) AS paid "
                + "FROM CustomerOrder o JOIN Customer c ON o.customerId = c.customerId ";
        Object[] params;
        if (status == null) {
            params = new Object[] {};
        } else {
            sql += "WHERE o.status = ? ";
            params = new Object[] { status };
        }
        sql += "ORDER BY o.orderId DESC LIMIT 300";

        ArrayList<Object[]> raw = runQuery(sql, params, 6);
        ArrayList<Object[]> rows = new ArrayList<Object[]>();
        for (Object[] r : raw) {
            double total = ((Number) r[4]).doubleValue();
            double paid = ((Number) r[5]).doubleValue();
            double balance = Math.round((total - paid) * 100.0) / 100.0;
            rows.add(new Object[] { r[0], r[1], String.valueOf(r[2]), r[3], total, paid, balance });
        }
        return rows;
    }

    // Columns: Reservation, Item, Product, Units, Returned, Unit price, Status
    public ArrayList<Object[]> getReservationRows(int orderId) throws SQLException {
        String sql = "SELECT r.reservationId, r.itemId, p.name, r.reservedQuantity, r.returnedQuantity, "
                + "p.unitPrice, r.status FROM Reservation r " + ITEM_JOINS
                + "WHERE r.orderId = ? ORDER BY r.reservationId";
        return runQuery(sql, new Object[] { orderId }, 7);
    }

    // Reservations that are ready to ship: Active, on a Confirmed or PartiallyShipped order.
    // Columns: Reservation, Order, Customer, Product, Units, Location, Order status, Paid anything?
    public ArrayList<Object[]> getShippableRows() throws SQLException {
        String sql = "SELECT r.reservationId, r.orderId, c.name, p.name, r.reservedQuantity, s.location, "
                + "o.status, (SELECT COUNT(*) FROM Payment WHERE orderId = o.orderId) "
                + "FROM Reservation r "
                + "JOIN CustomerOrder o ON r.orderId = o.orderId "
                + "JOIN Customer c ON o.customerId = c.customerId " + ITEM_JOINS
                + "WHERE r.status = 'Active' AND o.status IN ('Confirmed', 'PartiallyShipped') "
                + "ORDER BY r.reservationId LIMIT 500";
        ArrayList<Object[]> raw = runQuery(sql, new Object[] {}, 8);
        for (Object[] r : raw) {
            int payments = ((Number) r[7]).intValue();
            r[7] = (payments > 0) ? "Yes" : "No";
        }
        return raw;
    }

    // Reservations that were shipped and still have units that could be returned.
    // Columns: Reservation, Order, Customer, Product, Shipped units, Already returned
    public ArrayList<Object[]> getReturnableRows() throws SQLException {
        String sql = "SELECT r.reservationId, r.orderId, c.name, p.name, r.reservedQuantity, r.returnedQuantity "
                + "FROM Reservation r "
                + "JOIN CustomerOrder o ON r.orderId = o.orderId "
                + "JOIN Customer c ON o.customerId = c.customerId " + ITEM_JOINS
                + "WHERE r.status = 'Fulfilled' AND r.returnedQuantity < r.reservedQuantity "
                + "ORDER BY r.reservationId DESC LIMIT 500";
        return runQuery(sql, new Object[] {}, 6);
    }

    // Columns: Payment, Date, Amount
    public ArrayList<Object[]> getPaymentRows(int orderId) throws SQLException {
        String sql = "SELECT paymentId, paymentDate, amount FROM Payment WHERE orderId = ? ORDER BY paymentId";
        return runQuery(sql, new Object[] { orderId }, 3);
    }

    // Columns: Refund, Date, Amount, Reason, Processed by
    public ArrayList<Object[]> getRefundRows(int orderId) throws SQLException {
        String sql = "SELECT f.refundId, f.refundDate, f.amount, f.reason, u.name "
                + "FROM Refund f JOIN User u ON f.processedBy = u.userId "
                + "WHERE f.orderId = ? ORDER BY f.refundId";
        return runQuery(sql, new Object[] { orderId }, 5);
    }

    // ---------------------------------------------------------------
    // Ledger
    // ---------------------------------------------------------------

    // Columns: Entry, Time, Item, Action, Change, User, Remarks
    // actionType = null means all actions; itemId = 0 means all items. Newest first, at most 300.
    public ArrayList<Object[]> getLedgerRows(String actionType, int itemId) throws SQLException {
        String sql = "SELECT l.entryId, l.timestamp, l.itemId, l.actionType, l.quantityChange, u.name, l.remarks "
                + "FROM LedgerEntry l JOIN User u ON l.userId = u.userId WHERE 1 = 1 ";
        ArrayList<Object> params = new ArrayList<Object>();
        if (actionType != null) {
            sql += "AND l.actionType = ? ";
            params.add(actionType);
        }
        if (itemId > 0) {
            sql += "AND l.itemId = ? ";
            params.add(itemId);
        }
        sql += "ORDER BY l.entryId DESC LIMIT 300";
        return runQuery(sql, params.toArray(), 7);
    }
}
