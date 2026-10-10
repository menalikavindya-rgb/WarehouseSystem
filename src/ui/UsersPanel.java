package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import dao.UserManagementDAO;
import model.SalesOfficer;
import model.User;
import model.WarehouseManager;
import model.WarehouseOperator;

// The manager adds users, edits them, resets passwords, and deactivates or re-activates accounts.
public class UsersPanel extends ScreenPanel {

    private User manager;
    private UserManagementDAO userDAO = new UserManagementDAO();
    private java.util.List<User> users = new ArrayList<User>();
    private JTable table = createTable();

    public UsersPanel(User manager) {
        super("Users");
        this.manager = manager;

        JButton addButton = new JButton("Add user");
        JButton editButton = new JButton("Edit name / email");
        JButton passwordButton = new JButton("Reset password");
        JButton activeButton = new JButton("Deactivate / activate");
        JButton refreshButton = new JButton("Refresh");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(addButton);
        bar.add(editButton);
        bar.add(passwordButton);
        bar.add(activeButton);
        bar.add(refreshButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(bar, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUser();
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editUser();
            }
        });
        passwordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetPassword();
            }
        });
        activeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleActive();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    // Finds the User object that belongs to the selected table row (null if none is selected).
    private User selectedUser() {
        Object id = selectedValue(table, 0);
        if (id == null) {
            showWarning("Please select a user in the table first.");
            return null;
        }
        int userId = ((Number) id).intValue();
        for (User u : users) {
            if (u.getUserId() == userId) {
                return u;
            }
        }
        return null;
    }

    private void addUser() {
        try {
            JTextField nameField = new JTextField(25);
            JTextField emailField = new JTextField(25);
            JComboBox<String> roleBox = new JComboBox<String>(
                    new String[] { "WarehouseOperator", "SalesOfficer", "WarehouseManager" });
            JPasswordField passwordField = new JPasswordField(25);
            JTextField extraField = new JTextField(25);

            Object[] fields = { "Name", nameField, "Email", emailField, "Role", roleBox,
                    "Password (at least 6 characters)", passwordField,
                    "Yard zone (operator) or monthly target in Rs. (sales officer)", extraField };
            int answer = JOptionPane.showConfirmDialog(this, fields, "Add user",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }

            // build the right kind of User object for the chosen role (inheritance)
            String role = (String) roleBox.getSelectedItem();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String extra = extraField.getText().trim();

            User newUser;
            if (role.equals("WarehouseOperator")) {
                newUser = new WarehouseOperator(0, name, email, "", 1, extra);
            } else if (role.equals("SalesOfficer")) {
                double target = 0;
                if (!extra.isEmpty()) {
                    target = readDouble(extraField, "Monthly target");
                }
                newUser = new SalesOfficer(0, name, email, "", 2, target);
            } else {
                newUser = new WarehouseManager(0, name, email, "", 3);
            }

            int newId = userDAO.addUser(manager.getUserId(), newUser, new String(passwordField.getPassword()));
            refresh();
            showInfo("User " + newId + " (" + name + ") added as " + role + ".");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void editUser() {
        User selected = selectedUser();
        if (selected == null) {
            return;
        }
        try {
            JTextField nameField = new JTextField(selected.getName(), 25);
            JTextField emailField = new JTextField(selected.getEmail(), 25);
            Object[] fields = { "Name", nameField, "Email", emailField };
            int answer = JOptionPane.showConfirmDialog(this, fields, "Edit user " + selected.getUserId(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            userDAO.updateUserDetails(manager.getUserId(), selected.getUserId(),
                    nameField.getText(), emailField.getText());
            refresh();
            showInfo("User " + selected.getUserId() + " updated.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void resetPassword() {
        User selected = selectedUser();
        if (selected == null) {
            return;
        }
        try {
            JPasswordField passwordField = new JPasswordField(20);
            Object[] fields = { "New password for " + selected.getName() + " (at least 6 characters)", passwordField };
            int answer = JOptionPane.showConfirmDialog(this, fields, "Reset password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            userDAO.resetPassword(manager.getUserId(), selected.getUserId(),
                    new String(passwordField.getPassword()));
            showInfo("Password of " + selected.getName() + " has been reset.");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void toggleActive() {
        User selected = selectedUser();
        if (selected == null) {
            return;
        }
        boolean makeActive = !selected.isActive();
        String verb = makeActive ? "Activate" : "Deactivate";
        int answer = JOptionPane.showConfirmDialog(this, verb + " " + selected.getName() + "?",
                verb + " user", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            userDAO.setActive(manager.getUserId(), selected.getUserId(), makeActive);
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        try {
            users = userDAO.getAllUsers(manager.getUserId());

            ArrayList<Object[]> rows = new ArrayList<Object[]>();
            for (User u : users) {
                // the extra column depends on the kind of user (polymorphism with instanceof)
                String extra = "";
                if (u instanceof WarehouseOperator) {
                    extra = "Zone: " + ((WarehouseOperator) u).getAssignedYardZone();
                } else if (u instanceof SalesOfficer) {
                    extra = String.format("Target: Rs. %,.2f", ((SalesOfficer) u).getSalesTarget());
                }
                rows.add(new Object[] { u.getUserId(), u.getName(), u.getEmail(), u.getRole(),
                        u.isActive() ? "Yes" : "No", extra });
            }
            String[] columns = { "ID", "Name", "Email", "Role", "Active", "Details" };
            fillTable(table, columns, rows);
        } catch (Exception e) {
            handleError(e);
        }
    }
}
