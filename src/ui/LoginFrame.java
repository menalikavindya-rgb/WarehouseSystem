package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import com.formdev.flatlaf.FlatClientProperties;
import dao.UserDAO;
import exception.InvalidLoginException;
import model.User;

// The sign-in window: a dark brand panel on the left, the form on the right.
public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginButton;
    private UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("Warehouse Handling System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 540);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel content = new JPanel(new GridLayout(1, 2));
        content.add(new BrandPanel());
        content.add(buildForm());
        setContentPane(content);

        getRootPane().setDefaultButton(loginButton);   // pressing Enter clicks "Log in"

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(0, 64, 0, 64));

        JLabel title = new JLabel("Sign in");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel hint = new Theme.MutedLabel(" ");
        hint.setAlignmentX(LEFT_ALIGNMENT);

        JLabel emailLabel = new JLabel("Email");
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        emailField = new JTextField();
        emailField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "name@yard.lk");
        emailField.putClientProperty(FlatClientProperties.STYLE, "margin:8,12,8,12");
        emailField.setAlignmentX(LEFT_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setAlignmentX(LEFT_ALIGNMENT);
        passwordField = new JPasswordField();
        passwordField.putClientProperty(FlatClientProperties.STYLE, "margin:8,12,8,12; showRevealButton:true");
        passwordField.setAlignmentX(LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(0xE5484D));
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        loginButton = new JButton("Log in");
        Theme.stylePrimary(loginButton);
        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setPreferredSize(new Dimension(100, 42));

        form.add(Box.createVerticalGlue());
        form.add(title);
        form.add(Box.createVerticalStrut(4));
        form.add(hint);
        form.add(Box.createVerticalStrut(28));
        form.add(emailLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(emailField);
        form.add(Box.createVerticalStrut(16));
        form.add(passwordLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(10));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(10));
        form.add(loginButton);
        form.add(Box.createVerticalGlue());
        return form;
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        errorLabel.setText(" ");

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Enter your email and password.");
            return;
        }

        try {
            User user = userDAO.login(email, password);
            new MainFrame(user).setVisible(true);
            dispose();   // close the login window

        } catch (InvalidLoginException e) {
            errorLabel.setText(e.getMessage());
            passwordField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not reach the database:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // The left side: the name of the system and the stages stock moves through,
    // drawn in the same colours the status pills use inside the app.
    private static class BrandPanel extends JPanel {

        private static final String[] STAGES = { "Curing", "Available", "Reserved", "Shipped" };

        BrandPanel() {
            setBackground(Theme.SIDEBAR_BG);
            setPreferredSize(new Dimension(450, 540));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int left = 56;
            int width = getWidth() - 2 * left;

            // brand mark and name
            Theme.Badge mark = new Theme.Badge("D", new Color(0x0891B2), 60, false);
            mark.setSize(60, 60);
            g2.translate(left, 56);
            mark.paint(g2);
            g2.translate(-left, -56);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 30f));
            g2.drawString("Welcome!", left, 170);
            g2.drawString("Dilani Concrete Works", left, 208);

            g2.setColor(Theme.SIDEBAR_TEXT);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Warehouse Handling System", left, 246);
            

            // the four stages as a coloured strip
            int stripY = getHeight() - 120;
            int gap = 6;
            int segment = (width - gap * (STAGES.length - 1)) / STAGES.length;
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < STAGES.length; i++) {
                int x = left + i * (segment + gap);
                g2.setColor(Theme.statusColor(STAGES[i]));
                g2.fillRoundRect(x, stripY, segment, 8, 8, 8);
                g2.setColor(Theme.SIDEBAR_MUTED);
                ;
            }
            g2.dispose();
        }
    }
}
