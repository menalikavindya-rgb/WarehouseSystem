package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

// The parent class of every screen. It gives each screen the same title,
// the same way of filling a table, and the same way of showing messages.
public abstract class ScreenPanel extends JPanel {

    private JLabel titleLabel;

    public ScreenPanel(String title) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        add(titleLabel, BorderLayout.NORTH);
    }

    // Each child class decides how it reloads its data from the database.
    public abstract void refresh();

    // ---------- tables ----------

    // A table the user can look at and select rows in, but not type into.
    protected JTable createTable() {
        JTable table = new JTable();
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);   // click a column title to sort
        return table;
    }

    protected void fillTable(JTable table, String[] columns, ArrayList<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Object[] row : rows) {
            model.addRow(row);
        }
        table.setModel(model);
    }

    // Turns the numbers in the given columns into text like 12,500.00 so the table looks tidy.
    protected void formatMoney(ArrayList<Object[]> rows, int... columns) {
        for (Object[] row : rows) {
            for (int col : columns) {
                row[col] = String.format("%,.2f", ((Number) row[col]).doubleValue());
            }
        }
    }

    // The value in column 'col' of the selected row, or null if no row is selected.
    // (convertRowIndexToModel is needed because the user may have sorted the table.)
    protected Object selectedValue(JTable table, int col) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return table.getModel().getValueAt(modelRow, col);
    }

    // ---------- forms ----------

    // A form: label on the left, field on the right, one row per field.
    // It is kept at a fixed width and pinned to the left of the screen.
    protected JPanel formPanel(String[] labels, JComponent[] fields) {
        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 8));
        for (int i = 0; i < labels.length; i++) {
            grid.add(new JLabel(labels[i]));
            grid.add(fields[i]);
        }
        grid.setPreferredSize(new Dimension(520, labels.length * 36));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.add(grid);
        return wrapper;
    }

    // ---------- reading what the user typed ----------

    protected int readInt(JTextField field, String fieldName) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    protected double readDouble(JTextField field, String fieldName) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    // ---------- messages ----------

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    // One place that turns any exception into a dialog. The custom exceptions
    // carry the business rule in their message, so we simply show getMessage().
    protected void handleError(Exception e) {
        if (e instanceof SQLException) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Not allowed", JOptionPane.WARNING_MESSAGE);
        }
    }

    protected String money(double amount) {
        return String.format("Rs. %,.2f", amount);
    }
}
