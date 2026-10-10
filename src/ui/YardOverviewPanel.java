package ui;

import java.awt.BorderLayout;
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
    private JLabel capacityLabel = new JLabel(" ");
    private JTable statusTable = createTable();
    private JLabel noteLabel = new JLabel(" ");

    public YardOverviewPanel(User user) {
        super("Yard Overview");
        this.user = user;

        // top: three figures and the capacity bar
        JPanel tiles = new JPanel(new GridLayout(1, 3, 15, 0));
        tiles.add(makeTile("Total physical stock (units)", totalValue));
        tiles.add(makeTile("Stock in curing (units)", curingValue));
        tiles.add(makeTile("True sellable stock (units)", sellableValue));

        capacityBar.setStringPainted(true);
        JPanel capacityPanel = new JPanel(new BorderLayout(0, 5));
        capacityPanel.add(capacityLabel, BorderLayout.NORTH);
        capacityPanel.add(capacityBar, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.add(tiles, BorderLayout.NORTH);
        top.add(capacityPanel, BorderLayout.CENTER);

        // middle: units by status
        JPanel middle = new JPanel(new BorderLayout(0, 5));
        middle.add(new JLabel("Stock by status"), BorderLayout.NORTH);
        middle.add(new JScrollPane(statusTable), BorderLayout.CENTER);

        // bottom: refresh button and a note
        JButton refreshButton = new JButton("Refresh");
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.add(refreshButton, BorderLayout.WEST);
        bottom.add(noteLabel, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 15));
        body.add(top, BorderLayout.NORTH);
        body.add(middle, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    private JPanel makeTile(String caption, JLabel valueLabel) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 28f));
        JPanel tile = new JPanel(new BorderLayout(0, 5));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(java.awt.Color.GRAY),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        tile.add(new JLabel(caption), BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    // Brings the stock up to date. Used by this screen and once when someone logs in.
    // Returns the number of stock rows whose curing just finished.
    public static int runUpdates(User user) throws SQLException {
        BatchDAO batchDAO = new BatchDAO();
        int finished = batchDAO.completeCuring(user.getUserId(), LocalDate.now());
        batchDAO.markDepletedBatches();
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
                capacityLabel.setText("Yard capacity has not been set.");
                capacityBar.setValue(0);
                capacityBar.setString("");
            } else {
                int max = capacity[0];
                int percent = (int) Math.round(total * 100.0 / max);
                capacityLabel.setText(String.format("Yard occupancy: %,d of %,d units used   |   free space: %,d units",
                        total, max, max - total));
                capacityBar.setMaximum(max);
                capacityBar.setValue(Math.min(total, max));
                capacityBar.setString(percent + "%");
            }

            noteLabel.setText("Updated at " + LocalTime.now().withNano(0)
                    + "   |   stock rows that finished curing just now: " + finished);

        } catch (Exception e) {
            handleError(e);
        }
    }
}
