package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import dao.NotificationDAO;
import model.Notification;
import model.SalesOfficer;
import model.User;
import model.WarehouseManager;

// The user's own notifications. Sales officers and managers can also send one.
public class NotificationsPanel extends ScreenPanel {

    private User user;
    private NotificationDAO notificationDAO = new NotificationDAO();
    private JTable table = createTable();
    private JLabel unreadLabel = new JLabel(" ");

    private JComboBox<String> recipientBox;
    private JTextField messageField = new JTextField();

    public NotificationsPanel(User user) {
        super("Notifications");
        this.user = user;

        // top: buttons and the unread count
        JButton markButton = new JButton("Mark selected as read");
        JButton markAllButton = new JButton("Mark all as read");
        JButton refreshButton = new JButton("Refresh");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.add(markButton);
        buttons.add(markAllButton);
        buttons.add(refreshButton);
        buttons.add(unreadLabel);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(buttons, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);

        // bottom: the "send" part, only for sales officers and managers
        if (user instanceof SalesOfficer || user instanceof WarehouseManager) {
            if (user instanceof WarehouseManager) {
                recipientBox = new JComboBox<String>(new String[] {
                        "Warehouse operators", "Sales officers", "Warehouse managers" });
            } else {
                recipientBox = new JComboBox<String>(new String[] { "Warehouse operators" });
            }
            JButton sendButton = new JButton("Send");

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.add(recipientBox, BorderLayout.WEST);
            row.add(messageField, BorderLayout.CENTER);
            row.add(sendButton, BorderLayout.EAST);

            JPanel sendPanel = new JPanel(new BorderLayout(0, 6));
            sendPanel.add(new JLabel("Send a notification (for example, about an upcoming shipment)"),
                    BorderLayout.NORTH);
            sendPanel.add(row, BorderLayout.CENTER);
            body.add(sendPanel, BorderLayout.SOUTH);

            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    send();
                }
            });
        }
        add(body, BorderLayout.CENTER);

        markButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markSelected();
            }
        });
        markAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markAll();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    private String roleFor(String label) {
        if (label.equals("Warehouse operators")) {
            return "WarehouseOperator";
        } else if (label.equals("Sales officers")) {
            return "SalesOfficer";
        }
        return "WarehouseManager";
    }

    private void send() {
        try {
            String role = roleFor((String) recipientBox.getSelectedItem());
            int count = notificationDAO.sendManual(user.getUserId(), role, messageField.getText());
            showInfo("Notification sent to " + count + " user(s).");
            messageField.setText("");
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void markSelected() {
        Object id = selectedValue(table, 0);
        if (id == null) {
            showWarning("Please select a notification in the table first.");
            return;
        }
        try {
            notificationDAO.markAsRead(((Number) id).intValue(), user.getUserId());
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void markAll() {
        try {
            notificationDAO.markAllAsRead(user.getUserId());
            refresh();
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void refresh() {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            ArrayList<Notification> list = notificationDAO.getNotificationsForUser(user.getUserId());

            ArrayList<Object[]> rows = new ArrayList<Object[]>();
            for (Notification n : list) {
                String status;
                if (n.isRead()) {
                    status = "Read";
                } else {
                    status = "UNREAD";
                }
                rows.add(new Object[] { n.getNotificationId(), status,
                        n.getTimestamp().format(format), n.getMessage() });
            }
            String[] columns = { "ID", "Status", "Time", "Message" };
            fillTable(table, columns, rows);

            unreadLabel.setText("Unread: " + notificationDAO.countUnread(user.getUserId()));
        } catch (Exception e) {
            handleError(e);
        }
    }
}
