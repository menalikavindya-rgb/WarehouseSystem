package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

// The parent class of every screen. It gives each screen the same title bar,
// and every screen must say what it does when it is shown or refreshed.
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
}
