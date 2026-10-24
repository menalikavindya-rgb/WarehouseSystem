package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import dao.BatchDAO;
import dao.DashboardDAO;
import dao.NotificationDAO;
import dao.StockItemDAO;
import dao.ViewDAO;
import model.User;
import model.WarehouseManager;
import model.WarehouseOperator;

// The dashboard. Every role gets the same screen, filled with the figures that matter to that role:
//   operator      - stock by status, curing countdown, what is waiting to ship
//   sales officer - sellable stock by product, orders, money owed, payments
//   manager       - stock, orders, money, curing countdown and recent activity
// Showing this screen also brings the stock up to date (curing finished, batches depleted).
public class YardOverviewPanel extends ScreenPanel {

    private User user;
    private boolean isManager;
    private boolean isOperator;

    private StockItemDAO stockDAO = new StockItemDAO();
    private ViewDAO viewDAO = new ViewDAO();
    private DashboardDAO dashboardDAO = new DashboardDAO();

    // the figure cards (which ones are shown depends on the role)
    private JLabel totalValue = new JLabel("-");
    private JLabel curingValue = new JLabel("-");
    private JLabel shipValue = new JLabel("-");
    private JLabel openOrdersValue = new JLabel("-");
    private JLabel paymentsValue = new JLabel("-");
    private JLabel owedValue = new JLabel("-");

    // charts and tables
    private BarChartPanel stockChart = new BarChartPanel("Units by status");
    private BarChartPanel ordersChart = new BarChartPanel("Orders by status");
    private JTable productTable = createTable();
    private JTable curingTable = createTable();
    private JTable owedTable = createTable();
    private JTable activityTable = createTable();

    public YardOverviewPanel(User user) {
        super("Dashboard");
        this.user = user;
        this.isManager = user instanceof WarehouseManager;
        this.isOperator = user instanceof WarehouseOperator;

        JPanel content = new ScrollContent();
        content.setOpaque(false);
        int row = 0;   // (kept only to number the blocks)

        // ---- row 1: the figure cards ----
        JPanel tiles;
        if (isOperator) {
            tiles = new JPanel(new GridLayout(1, 3, 16, 0));
            tiles.add(makeTile("Total physical stock", "units in the yard", totalValue, null));
            tiles.add(makeTile("Stock in curing", "still curing", curingValue, Theme.statusColor("Curing")));
            tiles.add(makeTile("Waiting to ship", "reservations to ship", shipValue, Theme.statusColor("Confirmed")));
        } else if (isManager) {
            tiles = new JPanel(new GridLayout(1, 4, 16, 0));
            tiles.add(makeTile("Stock in curing", "still curing", curingValue, Theme.statusColor("Curing")));
            tiles.add(makeTile("Open orders", "not shipped yet", openOrdersValue, Theme.statusColor("Confirmed")));
            tiles.add(makeTile("Payments this month", "money received", paymentsValue, null));
            tiles.add(makeTile("Money owed", "unpaid on orders", owedValue, Theme.statusColor("Curing")));
        } else {
            tiles = new JPanel(new GridLayout(1, 3, 16, 0));
            tiles.add(makeTile("Open orders", "not shipped yet", openOrdersValue, Theme.statusColor("Confirmed")));
            tiles.add(makeTile("Money owed", "unpaid on orders", owedValue, Theme.statusColor("Curing")));
            tiles.add(makeTile("Payments this month", "money received", paymentsValue, null));
        }
        tiles.setOpaque(false);
        place(content, tiles, row++);

        // ---- the rest of the screen, by role ----
        if (isOperator) {
            place(content, pair(stockChart, tableCard("Curing finishing soon", curingTable)), row++);
        } else if (isManager) {
            place(content, tableCard("Customers who still owe money", owedTable), row++);
            place(content, tableCard("Recent activity", activityTable), row++);
        } else {
            place(content, pair(tableCard("Sellable stock by product", productTable), ordersChart), row++);
            place(content, tableCard("Customers who still owe money", owedTable), row++);
        }

        content.add(Box.createVerticalGlue());   // push everything to the top

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // The scrolling area is always exactly as wide as the window (it only scrolls up and down).
    private static class ScrollContent extends JPanel implements Scrollable {
        ScrollContent() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 80;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // building blocks
    // ------------------------------------------------------------------

    // Puts one component on its own row, as wide as the screen and exactly as tall as it needs.
    private void place(JPanel content, JComponent component, int row) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        wrapper.setAlignmentX(0f);
        wrapper.add(component, BorderLayout.CENTER);
        content.add(wrapper);
    }

    // Two cards side by side, equal width.
    private JPanel pair(JComponent left, JComponent right) {
        JPanel p = new JPanel(new GridLayout(1, 2, 16, 0));
        p.setOpaque(false);
        p.add(left);
        p.add(right);
        return p;
    }

    // One card: a caption, a big number in the given colour, and a short explanation.
    private JPanel makeTile(String caption, String hint, JLabel valueLabel, Color numberColor) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 26f));
        if (numberColor != null) {
            valueLabel.setForeground(numberColor);
        }
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD, 13f));

        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(captionLabel, BorderLayout.NORTH);
        text.add(valueLabel, BorderLayout.CENTER);
        text.add(new Theme.MutedLabel(hint), BorderLayout.SOUTH);

        Theme.Card tile = new Theme.Card(new BorderLayout());
        tile.add(text, BorderLayout.CENTER);
        return tile;
    }

    // A card with a title and a small table (five rows tall).
    private JPanel tableCard(String title, JTable table) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        table.setPreferredScrollableViewportSize(new Dimension(100, 36 * 5));
        table.setFocusable(false);                 // these small tables are for reading only
        table.setRowSelectionAllowed(false);
        JScrollPane scroll = new JScrollPane(table);

        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // data
    // ------------------------------------------------------------------

    // Brings the stock up to date. Used by this screen and once when someone logs in.
    // Returns the number of stock rows whose curing just finished.
    public static int runUpdates(User user) throws SQLException {
        BatchDAO batchDAO = new BatchDAO();
        int finished = batchDAO.completeCuring(user.getUserId(), LocalDate.now());
        batchDAO.markDepletedBatches();

        // tell the operators when stock has just finished curing
        if (finished > 0) {
            new NotificationDAO().notifyRole("WarehouseOperator",
                    "Curing has finished for " + finished + " stock row(s). The stock is now Available.");
        }
        return finished;
    }

    private static String units(int number) {
        return String.format("%,d", number);
    }

    // Rs. 950,000 stays as it is; Rs. 2,310,400 becomes Rs. 2.31M so it fits on a card.
    private static String rupees(double amount) {
        if (amount >= 1000000) {
            return String.format("Rs. %.2fM", amount / 1000000.0);
        }
        return "Rs. " + String.format("%,.0f", amount);
    }

    @Override
    public void refresh() {
        try {
            runUpdates(user);

            // stock figures
            if (isOperator) {
                totalValue.setText(units(stockDAO.getTotalPhysicalStock()));
            }
            if (isOperator || isManager) {
                curingValue.setText(units(stockDAO.getCuringStock()));
            }
            if (isOperator) {
                shipValue.setText(units(viewDAO.getShippableRows().size()));
            }

            // order and money figures
            if (!isOperator) {
                ArrayList<Object[]> orders = viewDAO.getOrderRows(null);
                int open = 0;
                double owed = 0;
                ArrayList<Object[]> owing = new ArrayList<Object[]>();
                for (Object[] o : orders) {
                    String status = String.valueOf(o[3]);
                    double balance = ((Number) o[6]).doubleValue();
                    if (status.equals("Pending") || status.equals("Confirmed") || status.equals("PartiallyShipped")) {
                        open++;
                    }
                    if (balance > 0.004 && !status.equals("Cancelled")) {
                        owed += balance;
                        owing.add(new Object[] { o[0], o[1], balance });
                    }
                }
                openOrdersValue.setText(units(open));
                owedValue.setText(rupees(owed));
                paymentsValue.setText(rupees(dashboardDAO.getPaymentsThisMonth()));

                // biggest debts first, five rows
                Collections.sort(owing, new Comparator<Object[]>() {
                    @Override
                    public int compare(Object[] a, Object[] b) {
                        return Double.compare((Double) b[2], (Double) a[2]);
                    }
                });
                ArrayList<Object[]> topOwing = new ArrayList<Object[]>(owing.subList(0, Math.min(5, owing.size())));
                formatMoney(topOwing, 2);
                fillTable(owedTable, new String[] { "Order", "Customer", "Balance (Rs.)" }, topOwing);

                if (!isManager) {
                    fillOrdersChart();
                }
            }

            // charts and tables
            if (isOperator) {
                fillStockChart();
                fillTable(curingTable, new String[] { "Product", "Units", "Ready on" },
                        dashboardDAO.getCuringSoon(8));
            }
            if (isManager) {
                fillTable(activityTable, new String[] { "When", "Who", "Action", "Units", "Product" },
                        dashboardDAO.getRecentActivity(8));
            }
            if (!isOperator && !isManager) {
                fillTable(productTable, new String[] { "Product", "Category", "Units" },
                        dashboardDAO.getSellableByProduct());
            }

        } catch (Exception e) {
            handleError(e);
        }
    }

    private void fillStockChart() throws SQLException {
        ArrayList<String> labels = new ArrayList<String>();
        ArrayList<Double> values = new ArrayList<Double>();
        ArrayList<Color> colors = new ArrayList<Color>();
        for (Object[] r : viewDAO.getUnitsByStatus()) {
            labels.add(String.valueOf(r[0]));
            values.add(((Number) r[2]).doubleValue());
            colors.add(Theme.statusColor(String.valueOf(r[0])));
        }
        stockChart.setData(labels, values, colors);
    }

    private void fillOrdersChart() throws SQLException {
        ArrayList<String> labels = new ArrayList<String>();
        ArrayList<Double> values = new ArrayList<Double>();
        ArrayList<Color> colors = new ArrayList<Color>();
        for (Object[] r : dashboardDAO.getOrderCountsByStatus()) {
            labels.add(BarChartPanel.spaced(String.valueOf(r[0])));
            values.add(((Number) r[1]).doubleValue());
            colors.add(Theme.statusColor(String.valueOf(r[0])));
        }
        ordersChart.setData(labels, values, colors);
    }
}
