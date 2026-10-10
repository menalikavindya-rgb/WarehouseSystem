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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import dao.DamageDAO;
import dao.ReturnDAO;
import dao.ViewDAO;
import model.User;

// Two jobs of the operator: log damaged stock, and take goods back from a customer.
public class DamageReturnsPanel extends ScreenPanel {

    private User user;
    private ViewDAO viewDAO = new ViewDAO();
    private DamageDAO damageDAO = new DamageDAO();
    private ReturnDAO returnDAO = new ReturnDAO();

    private JTable damageTable = createTable();
    private JTextField damageUnitsField = new JTextField(6);
    private JTextField damageReasonField = new JTextField(25);

    private JTable returnTable = createTable();
    private JTextField returnUnitsField = new JTextField(6);
    private JComboBox<String> conditionBox = new JComboBox<String>(new String[] { "Available", "Damaged" });

    public DamageReturnsPanel(User user) {
        super("Damage and Returns");
        this.user = user;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log damage", buildDamageTab());
        tabs.addTab("Return goods", buildReturnTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildDamageTab() {
        JButton damageButton = new JButton("Log damage");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        form.add(new JLabel("Units damaged:"));
        form.add(damageUnitsField);
        form.add(new JLabel("Reason:"));
        form.add(damageReasonField);
        form.add(damageButton);

        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tab.add(new JLabel("Select the stock row that was damaged (curing, available or reserved stock):"),
                BorderLayout.NORTH);
        tab.add(new JScrollPane(damageTable), BorderLayout.CENTER);
        tab.add(form, BorderLayout.SOUTH);

        damageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logDamage();
            }
        });
        return tab;
    }

    private JPanel buildReturnTab() {
        JButton returnButton = new JButton("Process return");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        form.add(new JLabel("Units returned:"));
        form.add(returnUnitsField);
        form.add(new JLabel("Condition:"));
        form.add(conditionBox);
        form.add(returnButton);

        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tab.add(new JLabel("Select the shipped reservation the goods came from:"), BorderLayout.NORTH);
        tab.add(new JScrollPane(returnTable), BorderLayout.CENTER);
        tab.add(form, BorderLayout.SOUTH);

        returnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processReturn();
            }
        });
        return tab;
    }

    private void logDamage() {
        try {
            Object id = selectedValue(damageTable, 0);
            if (id == null) {
                showWarning("Please select a stock row in the table first.");
                return;
            }
            int units = readInt(damageUnitsField, "Units damaged");
            String reason = damageReasonField.getText().trim();
            if (reason.isEmpty()) {
                showWarning("Please enter a reason for the damage.");
                return;
            }

            int damagedItemId = damageDAO.logDamage(((Number) id).intValue(), units, reason, user.getUserId());
            showInfo(units + " units logged as damaged (stock row " + damagedItemId + ").");
            damageUnitsField.setText("");
            damageReasonField.setText("");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void processReturn() {
        try {
            Object id = selectedValue(returnTable, 0);
            if (id == null) {
                showWarning("Please select a reservation in the table first.");
                return;
            }
            int units = readInt(returnUnitsField, "Units returned");
            String condition = (String) conditionBox.getSelectedItem();

            int newItemId = returnDAO.returnGoods(((Number) id).intValue(), units, condition, user.getUserId());
            showInfo(units + " units returned as " + condition + " (new stock row " + newItemId + ").");
            returnUnitsField.setText("");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        try {
            String[] damageColumns = { "Item ID", "Batch", "Product", "Location", "Units", "Status", "Curing ends" };
            fillTable(damageTable, damageColumns, viewDAO.getDamageableRows());

            String[] returnColumns = { "Reservation", "Order", "Customer", "Product", "Shipped units", "Already returned" };
            fillTable(returnTable, returnColumns, viewDAO.getReturnableRows());
        } catch (Exception e) {
            handleError(e);
        }
    }
}
