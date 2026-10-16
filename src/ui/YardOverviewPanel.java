package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import dao.BatchDAO;
import dao.NotificationDAO;
import dao.StockItemDAO;
import dao.ViewDAO;
import model.User;

// The three stock figures, the yard occupancy and the units by status.
// Showing this screen also brings the stock up to date (curing finished, batches depleted).
public class YardOverviewPanel extends ScreenPanel {

    private User user;
    private StockItemDAO stockDAO = new StockItemDAO();
    private ViewDAO viewDAO = new ViewDAO();

    private JLabel totalValue = new JLabel("-");
    private JLabel curingValue = new JLabel("-");
    private JLabel sellableValue = new JLabel("-");
    private JProgressBar capacityBar = new JProgressBar();
    private JLabel occupancyPercent = new JLabel(" ");
    private JLabel capacityLabel = new Theme.MutedLabel(" ");
    private JTable statusTable = createTable();
    private JLabel noteLabel = new Theme.MutedLabel(" ");

    public YardOverviewPanel(User user) {
        super("Yard Overview");
        this.user = user;

        // row 1: the three figures, each in its status colour
        JPanel tiles = new JPanel(new GridLayout(1, 3, 16, 0));
        tiles.setOpaque(false);
        tiles.add(makeTile("Total physical stock", "units in the yard", totalValue, null));
        tiles.add(makeTile("Stock in curing", "not ready for sale yet", curingValue, Theme.statusColor("Curing")));
        tiles.add(makeTile("True sellable stock", "ready and not reserved", sellableValue,
                Theme.statusColor("Available")));

        // row 2: how full the yard is
        occupancyPercent.setFont(occupancyPercent.getFont().deriveFont(Font.BOLD, 15f));
        JLabel occupancyTitle = new JLabel("Yard occupancy");
        occupancyTitle.setFont(occupancyTitle.getFont().deriveFont(Font.BOLD, 14f));
        JPanel occupancyHeader = new JPanel(new BorderLayout());
        occupancyHeader.setOpaque(false);
        occupancyHeader.add(occupancyTitle, BorderLayout.WEST);
        occupancyHeader.add(occupancyPercent, BorderLayout.EAST);

        capacityBar.setStringPainted(false);
        capacityBar.setPreferredSize(new java.awt.Dimension(100, 10));

        Theme.Card capacityCard = new Theme.Card(new BorderLayout(0, 10));
        capacityCard.add(occupancyHeader, BorderLayout.NORTH);
        capacityCard.add(capacityBar, BorderLayout.CENTER);
        capacityCard.add(capacityLabel, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(tiles, BorderLayout.NORTH);
        top.add(capacityCard, BorderLayout.CENTER);

        // row 3: units by status
        JLabel statusTitle = new JLabel("Stock by status");
        statusTitle.setFont(statusTitle.getFont().deriveFont(Font.BOLD, 14f));
        JButton refreshButton = new JButton("Refresh");
        JPanel statusHeader = new JPanel(new BorderLayout(12, 0));
        statusHeader.setOpaque(false);
        statusHeader.add(statusTitle, BorderLayout.WEST);
        statusHeader.add(noteLabel, BorderLayout.CENTER);
        statusHeader.add(refreshButton, BorderLayout.EAST);

        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.setOpaque(false);
        middle.add(statusHeader, BorderLayout.NORTH);
        middle.add(new JScrollPane(statusTable), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(middle, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    // One card: a caption, a big number in the given colour, and a short explanation.
    private JPanel makeTile(String caption, String hint, JLabel valueLabel, Color numberColor) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 34f));
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

    @Override
    public void refresh() {
        try {
            int finished = runUpdates(user);

            int total = stockDAO.getTotalPhysicalStock();
            totalValue.setText(String.format("%,d", total));
            curingValue.setText(String.format("%,d", stockDAO.getCuringStock()));
            sellableValue.setText(String.format("%,d", stockDAO.getSellableStock()));

            String[] columns = { "Status", "Stock rows", "Units" };
            fillTable(statusTable, columns, viewDAO.getUnitsByStatus());

            int[] capacity = viewDAO.getLatestCapacity();
            if (capacity == null) {
                capacityLabel.setText("The yard capacity has not been set yet.");
                occupancyPercent.setText(" ");
                capacityBar.setValue(0);
            } else {
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

            noteLabel.setText("Updated at " + LocalTime.now().withNano(0)
                    + ". Curing finished just now for " + finished + " stock row(s).");

        } catch (Exception e) {
            handleError(e);
        }
    }
}
