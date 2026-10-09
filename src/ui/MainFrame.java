package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import model.SalesOfficer;
import model.User;
import model.WarehouseManager;
import model.WarehouseOperator;

public class MainFrame extends JFrame {

    private User user;
    private JPanel screenArea;                 // the right side, shows one screen at a time
    private CardLayout cards = new CardLayout();
    private JPanel menuPanel;                  // the sidebar buttons live here
    private boolean darkMode = false;

    public MainFrame(User user) {
        this.user = user;

        setTitle("Warehouse Handling System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        screenArea = new JPanel(cards);
        add(screenArea, BorderLayout.CENTER);

        // Step 1: which menu items does this role get? (the rules from section 2 of the ERD document)
        String[] menuItems = getMenuItemsFor(user);

        // Step 2: one button in the sidebar and one screen for every menu item
        addMenuButtons(menuPanel, menuItems);

        // Step 3: show the first screen
        showScreen(menuItems[0]);
    }

    // ---------------------------------------------------------------
    // Which screens each role may open. This is how roles are enforced in the app.
    // ---------------------------------------------------------------
    private String[] getMenuItemsFor(User user) {
        if (user instanceof WarehouseOperator) {
            return new String[] { "Yard Overview", "Register Batch", "Stock", "Damage and Returns" };
        } else if (user instanceof SalesOfficer) {
            return new String[] { "Sellable Stock", "Customers", "Orders", "Payments" };
        } else if (user instanceof WarehouseManager) {
            return new String[] { "Yard Overview", "Stock", "Orders", "Ledger", "Reports", "Users" };
        }
        return new String[] { "Yard Overview" };
    }

    // ---------------------------------------------------------------
    // The left sidebar: title, menu (filled later), theme button, log out
    // ---------------------------------------------------------------
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));

        // top: app name, user name, role
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        JLabel appName = new JLabel("Warehouse System");
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 16f));
        header.add(appName);
        header.add(new JLabel(user.getName()));
        header.add(new JLabel(user.getRole()));
        sidebar.add(header, BorderLayout.NORTH);

        // middle: the menu buttons go here
        menuPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        JPanel menuHolder = new JPanel(new BorderLayout());   // keeps the buttons at the top
        menuHolder.add(menuPanel, BorderLayout.NORTH);
        sidebar.add(menuHolder, BorderLayout.CENTER);

        // bottom: theme toggle and log out
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 6));
        final JButton themeButton = new JButton("Dark mode");
        JButton logoutButton = new JButton("Log out");
        bottom.add(themeButton);
        bottom.add(logoutButton);
        sidebar.add(bottom, BorderLayout.SOUTH);

        themeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                darkMode = !darkMode;
                if (darkMode) {
                    FlatDarkLaf.setup();
                    themeButton.setText("Light mode");
                } else {
                    FlatLightLaf.setup();
                    themeButton.setText("Dark mode");
                }
                FlatLaf.updateUI();   // repaint every open window with the new theme
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        return sidebar;
    }

    private void addMenuButtons(JPanel menuPanel, String[] menuItems) {
        ButtonGroup group = new ButtonGroup();   // only one button can be selected at a time

        for (int i = 0; i < menuItems.length; i++) {
            final String name = menuItems[i];

            JToggleButton button = new JToggleButton(name);
            button.setFocusable(false);
            group.add(button);
            menuPanel.add(button);

            // every screen is created once and kept in the CardLayout under its name
            screenArea.add(createScreen(name), name);

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showScreen(name);
                }
            });

            if (i == 0) {
                button.setSelected(true);
            }
        }
    }

    // ---------------------------------------------------------------
    // Creates the screen for a menu name. Right now every screen is a placeholder;
    // in the next steps each "case" will return a real screen class.
    // ---------------------------------------------------------------
    private ScreenPanel createScreen(String name) {
        return new PlaceholderPanel(name);
    }

    private void showScreen(String name) {
        cards.show(screenArea, name);
        // ask the screen that is now visible to reload its data
        for (java.awt.Component c : screenArea.getComponents()) {
            if (c.isVisible() && c instanceof ScreenPanel) {
                ((ScreenPanel) c).refresh();
            }
        }
    }
}