package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import model.User;

public class MainFrame extends JFrame {

    private User user;

    public MainFrame(User user) {
        this.user = user;

        setTitle("Warehouse Handling System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Top bar: a welcome message on the left, a Log out button on the right
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel welcome = new JLabel("Welcome, " + user.getName() + "  (" + user.getRole() + ")");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 16f));
        topBar.add(welcome, BorderLayout.WEST);

        JButton logoutButton = new JButton("Log out");
        topBar.add(logoutButton, BorderLayout.EAST);

        // Middle: a placeholder until we build the real screens
        JLabel placeholder = new JLabel("The screens for the " + user.getRole() + " will go here.",
                SwingConstants.CENTER);

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(placeholder, BorderLayout.CENTER);

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });
    }
}