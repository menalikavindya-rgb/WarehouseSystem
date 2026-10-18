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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import dao.BatchDAO;
import dao.DashboardDAO;
import dao.NotificationDAO;
import dao.StockItemDAO;
import dao.ViewDAO;
import model.Notification;
import model.User;
import model.WarehouseManager;
import model.WarehouseOperator;

// The dashboard. Every role gets the same screen, filled with the figures that matter to that role:
//   operator      - stock, curing, what is waiting to ship, yard occupancy, recent activity
//   sales officer - sellable stock, orders, money owed, payments
//   manager       - all of it
// Showing this screen also brings the stock up to date (curing finished, batches depleted).
public class YardOverviewPanel extends ScreenPanel {

    private User user;
    private boolean isManager;
    private boolean isOperator;

    private StockItemDAO stockDAO = new StockItemDAO();
    private ViewDAO viewDAO = new ViewDAO();
    private DashboardDAO dashboardDAO = new DashboardDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();

    // the figure cards (which ones are shown depends on the role)
    private JLabel totalValue = new JLabel("-");
    private JLabel curingValue = new JLabel("-");
    private JLabel sellableValue = new JLabel("-");
    private JLabel shipValue = new JLabel("-");
    private JLabel openOrdersValue = new JLabel("-");
    private JLabel paymentsValue = new JLabel("-");
    private JLabel owedValue = new JLabel("-");

    // charts and tables
    private BarChartPanel stockChart = new BarChartPanel("Units by status");
    private BarChartPanel ordersChart = new BarChartPanel("Orders by status");
    private BarChartPanel productChart = new BarChartPanel("Sellable stock by product");
    private JTable curingTable = createTable();
    private JTable owedTable = createTable();
    private JTable activityTable = createTable();

    // notifications card
    private JPanel notificationList = new JPanel();
    private JLabel notificationCount = new Theme.MutedLabel(" ");

    // yard occupancy card
    private JProgressBar capacityBar = new JProgressBar();
    private JLabel occupancyPercent = new JLabel(" ");
    private JLabel capacityLabel = new Theme.MutedLabel(" ");

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
            tiles = new JPanel(new GridLayout(1, 4, 16, 0));
            tiles.add(makeTile("Total physical stock", "units in the yard", totalValue, null));
            tiles.add(makeTile("Stock in curing", "still curing", curingValue, Theme.statusColor("Curing")));
            tiles.add(makeTile("Sellable stock", "ready, not reserved", sellableValue,
                    Theme.statusColor("Available")));
            tiles.add(makeTile("Waiting to ship", "reservations to ship", shipValue,
                    Theme.statusColor("Confirmed")));
        } else if (isManager) {
            tiles = new JPanel(new GridLayout(1, 5, 16, 0));
            tiles.add(makeTile("Sellable stock", "ready, not reserved", sellableValue,
                    Theme.statusColor("Available")));
            tiles.add(makeTile("Stock in curing", "still curing", curingValue, Theme.statusColor("Curing")));
            tiles.add(makeTile("Open orders", "not shipped yet", openOrdersValue,
                    Theme.statusColor("Confirmed")));
            tiles.add(makeTile("Payments this month", "money received", paymentsValue, null));
            tiles.add(makeTile("Money owed", "unpaid on orders", owedValue, Theme.statusColor("Curing")));
        } else {
            tiles = new JPanel(new GridLayout(1, 4, 16, 0));
            tiles.add(makeTile("Sellable stock", "ready, not reserved", sellableValue,
                    Theme.statusColor("Available")));
            tiles.add(makeTile("Open orders", "not shipped yet", openOrdersValue,
                    Theme.statusColor("Confirmed")));
            tiles.add(makeTile("Money owed", "unpaid on orders", owedValue, Theme.statusColor("Curing")));
            tiles.add(makeTile("Payments this month", "money received", paymentsValue, null));
        }
        tiles.setOpaque(false);
        place(content, tiles, row++);

        // ---- row 2: charts ----
        if (isOperator) {
            place(content, pair(stockChart, tableCard("Curing finishing soon", curingTable)), row++);
        } else if (isManager) {
            place(content, pair(stockChart, ordersChart), row++);
        } else {
            place(content, pair(productChart, ordersChart), row++);
        }

        // ---- row 3 ----
        if (isOperator) {
            place(content, pair(notificationsCard(), occupancyCard()), row++);
            place(content, tableCard("Recent activity", activityTable), row++);
        } else if (isManager) {
            place(content, pair(tableCard("Curing finishing soon", curingTable), notificationsCard()), row++);
            place(content, occupancyCard(), row++);
            place(content, tableCard("Recent activity", activityTable), row++);
        } else {
            place(content, pair(notificationsCard(), tableCard("Customers who still owe money", owedTable)), row++);
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

    private JPanel notificationsCard() {
        JLabel titleLabel = new JLabel("Notifications");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(notificationCount, BorderLayout.EAST);

        notificationList.setOpaque(false);
        notificationList.setLayout(new BoxLayout(notificationList, BoxLayout.Y_AXIS));

        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.add(header, BorderLayout.NORTH);
        card.add(notificationList, BorderLayout.CENTER);
        return card;
    }

    private JPanel occupancyCard() {
        occupancyPercent.setFont(occupancyPercent.getFont().deriveFont(Font.BOLD, 15f));
        JLabel title = new JLabel("Yard occupancy");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(occupancyPercent, BorderLayout.EAST);

        capacityBar.setStringPainted(false);
        capacityBar.setPreferredSize(new Dimension(100, 10));

        JPanel barBox = new JPanel(new BorderLayout());   // keeps the bar thin however tall the card is
        barBox.setOpaque(false);
        barBox.add(capacityBar, BorderLayout.NORTH);

        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));
        card.add(header, BorderLayout.NORTH);
        card.add(barBox, BorderLayout.CENTER);
        card.add(capacityLabel, BorderLayout.SOUTH);
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
            int total = stockDAO.getTotalPhysicalStock();
            totalValue.setText(units(total));
            curingValue.setText(units(stockDAO.getCuringStock()));
            sellableValue.setText(units(stockDAO.getSellableStock()));

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

                fillOrdersChart();
            }

            // charts and tables
            if (isOperator || isManager) {
                fillStockChart();
                fillTable(curingTable, new String[] { "Product", "Units", "Ready on" },
                        dashboardDAO.getCuringSoon(8));
                fillTable(activityTable, new String[] { "When", "Who", "Action", "Units", "Product" },
                        dashboardDAO.getRecentActivity(8));
                fillOccupancy(total);
            }
            if (!isOperator && !isManager) {
                fillProductChart();
            }

            fillNotifications();

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

    private void fillProductChart() throws SQLException {
        ArrayList<String> labels = new ArrayList<String>();
        ArrayList<Double> values = new ArrayList<Double>();
        ArrayList<Color> colors = new ArrayList<Color>();
        for (Object[] r : dashboardDAO.getSellableByProduct(6)) {
            labels.add(String.valueOf(r[0]));
            values.add(((Number) r[1]).doubleValue());
            colors.add(Theme.statusColor("Available"));
        }
        productChart.setData(labels, values, colors);
    }

    // Shows the newest unread notifications of the logged-in user.
    private void fillNotifications() throws SQLException {
        ArrayList<Notification> all = notificationDAO.getNotificationsForUser(user.getUserId());
        ArrayList<Notification> unread = new ArrayList<Notification>();
        for (Notification n : all) {
            if (!n.isRead()) {
                unread.add(n);
            }
        }

        notificationList.removeAll();
        if (unread.isEmpty()) {
            notificationCount.setText("0 unread");
            notificationList.add(new Theme.MutedLabel("You are all caught up."));
        } else {
            notificationCount.setText(unread.size() + " unread");
            for (int i = 0; i < Math.min(5, unread.size()); i++) {
                String message = unread.get(i).getMessage();
                String shortText = message.length() > 80 ? message.substring(0, 77) + "..." : message;
                JLabel line = new JLabel("•  " + shortText);
                line.setToolTipText(message);
                line.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
                notificationList.add(line);
            }
            if (unread.size() > 5) {
                notificationList.add(new Theme.MutedLabel("and " + (unread.size() - 5)
                        + " more in the Notifications screen"));
            }
        }
        notificationList.revalidate();
        notificationList.repaint();
    }

    private void fillOccupancy(int total) throws SQLException {
        int[] capacity = viewDAO.getLatestCapacity();
        if (capacity == null) {
            capacityLabel.setText("The yard capacity has not been set yet.");
            occupancyPercent.setText(" ");
            capacityBar.setValue(0);
            return;
        }
        int max = capacity[0];
        int percent = (int) Math.round(total * 100.0 / max);
        occupancyPercent.setText(percent + "%");
        capacityLabel.setText(String.format("%,d of %,d units used, %,d free", total, max, max - total));
        capacityBar.setMaximum(max);
        capacityBar.setValue(Math.min(total, max));

        // calm colour while there is space, amber when it is getting full, red when it is full
        if (percent >= 100) {
            capacityBar.setForeground(Theme.statusColor("Damaged"));
        } else if (percent >= 85) {
            capacityBar.setForeground(Theme.statusColor("Curing"));
        } else {
            capacityBar.setForeground(Theme.accent());
        }
    }
}
