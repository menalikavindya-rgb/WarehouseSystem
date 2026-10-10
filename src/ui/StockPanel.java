package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import dao.ViewDAO;

// Every stock row, with a filter on its status.
public class StockPanel extends ScreenPanel {

    private ViewDAO viewDAO = new ViewDAO();
    private JComboBox<String> statusBox = new JComboBox<String>(
            new String[] { "All", "Curing", "Available", "Reserved", "Shipped", "Damaged" });
    private JTable table = createTable();
    private JLabel countLabel = new JLabel(" ");

    public StockPanel() {
        super("Stock");

        JButton refreshButton = new JButton("Refresh");
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.add(new JLabel("Status:"));
        filterBar.add(statusBox);
        filterBar.add(refreshButton);
        filterBar.add(countLabel);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(filterBar, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        ActionListener reload = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
        statusBox.addActionListener(reload);
        refreshButton.addActionListener(reload);
    }

    @Override
    public void refresh() {
        try {
            String choice = (String) statusBox.getSelectedItem();
            String status = choice.equals("All") ? null : choice;

            String[] columns = { "Item ID", "Batch", "Product", "Location", "Units", "Status", "Curing ends" };
            java.util.ArrayList<Object[]> rows = viewDAO.getStockRows(status);
            fillTable(table, columns, rows);
            countLabel.setText(rows.size() + " rows (newest first, at most 1000)");
        } catch (Exception e) {
            handleError(e);
        }
    }
}
