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
import javax.swing.JTextField;
import dao.ViewDAO;

// The full stock history (the ledger). It can only be read here, never edited.
public class LedgerPanel extends ScreenPanel {

    private ViewDAO viewDAO = new ViewDAO();
    private JComboBox<String> actionBox = new JComboBox<String>(new String[] { "All", "Produced", "Curing",
            "CuringComplete", "Reserved", "Shipped", "Damaged", "Returned", "Adjusted" });
    private JTextField itemField = new JTextField(8);
    private JTable table = createTable();
    private JLabel countLabel = new JLabel(" ");

    public LedgerPanel() {
        super("Ledger");

        JButton showButton = new JButton("Show");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(new JLabel("Action:"));
        bar.add(actionBox);
        bar.add(new JLabel("Item ID (blank = all):"));
        bar.add(itemField);
        bar.add(showButton);
        bar.add(countLabel);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(bar, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        showButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    @Override
    public void refresh() {
        try {
            String choice = (String) actionBox.getSelectedItem();
            String action = choice.equals("All") ? null : choice;

            int itemId = 0;
            if (!itemField.getText().trim().isEmpty()) {
                itemId = readInt(itemField, "Item ID");
            }

            String[] columns = { "Entry", "Time", "Item", "Action", "Change", "User", "Remarks" };
            java.util.ArrayList<Object[]> rows = viewDAO.getLedgerRows(action, itemId);
            for (Object[] row : rows) {
                row[1] = String.valueOf(row[1]);   // show the timestamp as plain text
            }
            fillTable(table, columns, rows);
            countLabel.setText(rows.size() + " entries (newest first, at most 300)");
        } catch (Exception e) {
            handleError(e);
        }
    }
}
