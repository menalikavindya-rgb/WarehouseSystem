package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import dao.UserDAO;
import exception.InvalidLoginException;
import model.User;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("Warehouse Handling System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 330);
        setLocationRelativeTo(null);   // centre the window on the screen
        setResizable(false);

        // The main panel, with space around the edges
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Top: the title
        JLabel title = new JLabel("Warehouse Handling System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        content.add(title, BorderLayout.NORTH);

        // Middle: labels and fields stacked in one column
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Email"));
        emailField = new JTextField();
        form.add(emailField);
        form.add(new JLabel("Password"));
        passwordField = new JPasswordField();
        form.add(passwordField);
        content.add(form, BorderLayout.CENTER);

        // Bottom: the button
        loginButton = new JButton("Log in");
        content.add(loginButton, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(loginButton);   // pressing Enter clicks "Log in"

        // What happens when the button is clicked
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your email and password.",
                    "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDAO.login(email, password);
            new MainFrame(user).setVisible(true);
            dispose();   // close the login window

        } catch (InvalidLoginException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Login failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not reach the database:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }
}