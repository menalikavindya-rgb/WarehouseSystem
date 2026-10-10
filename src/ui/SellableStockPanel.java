package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import dao.StockItemDAO;
import dao.ViewDAO;

// The sales officer's dashboard: only stock that is ready and not yet reserved.
public class SellableStockPanel extends ScreenPanel {

    private StockItemDAO stockDAO = new StockItemDAO();
    private ViewDAO viewDAO = new ViewDAO();
    private JLabel sellableLabel = new JLabel(" ");
    private JTable table = createTable();

    public SellableStockPanel() {
        super("Sellable Stock");

        sellableLabel.setFont(sellableLabel.getFont().deriveFont(Font.BOLD, 16f));
        JButton refreshButton = new JButton("Refresh");

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.add(sellableLabel, BorderLayout.CENTER);
        top.add(refreshButton, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(top, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    @Override
    public void refresh() {
        try {
            sellableLabel.setText(String.format("True sellable stock: %,d units (ready and not reserved)",
                    stockDAO.getSellableStock()));
            String[] columns = { "Item ID", "Product", "Category", "Location", "Units", "Unit price (Rs.)" };
            fillTable(table, columns, viewDAO.getAvailableRows());
        } catch (Exception e) {
            handleError(e);
        }
    }
}
