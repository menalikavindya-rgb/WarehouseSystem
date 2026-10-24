package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import com.formdev.flatlaf.FlatLaf;
import model.SalesOfficer;
import model.User;
import model.WarehouseManager;
import model.WarehouseOperator;

public class MainFrame extends JFrame {

    private User user;
    private JPanel screenArea;                 // the right side, shows one screen at a time
    private SidebarButton notificationsButton;   // shows the unread count
    private javax.swing.Timer badgeTimer;
    private CardLayout cards = new CardLayout();
    private JPanel menuPanel;                  // the sidebar buttons live here

    public MainFrame(User user) {
        this.user = user;

        setTitle("Dilani concrete works");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // a large window, but never bigger than the screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1280, screen.width - 40), Math.min(800, screen.height - 60));
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        screenArea = new JPanel(cards);
        add(screenArea, BorderLayout.CENTER);

        // Step 1: which menu items does this role get? (the rules from section 2 of the ERD document)
        String[] menuItems = getMenuItemsFor(user);

        // Step 2: one button in the sidebar and one screen for every menu item
        addMenuButtons(menuPanel, menuItems);

        // Step 2b: bring the stock up to date once at login (finished curing, empty batches)
        try {
            YardOverviewPanel.runUpdates(user);
        } catch (Exception e) {
            System.out.println("Start-up update failed: " + e.getMessage());
        }

        // Step 3: show the first screen
        showScreen(menuItems[0]);

        // Step 4: keep the unread count on the Notifications button up to date
        updateUnreadBadge();
        badgeTimer = new javax.swing.Timer(20000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUnreadBadge();
            }
        });
        badgeTimer.start();
    }

    // Asks the database how many unread notifications this user has and shows it on the sidebar.
    private void updateUnreadBadge() {
        if (notificationsButton == null) {
            return;
        }
        try {
            notificationsButton.setBadge(new dao.NotificationDAO().countUnread(user.getUserId()));
        } catch (Exception e) {
            System.out.println("Could not read the unread count: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        if (badgeTimer != null) {
            badgeTimer.stop();   // no more checks once this window is closed (log out)
        }
        super.dispose();
    }

    // ---------------------------------------------------------------
    // Which screens each role may open. This is how roles are enforced in the app.
    // ---------------------------------------------------------------
    private String[] getMenuItemsFor(User user) {
        if (user instanceof WarehouseOperator) {
            return new String[] { "Dashboard", "Register Batch", "Stock", "Shipments", "Damage and Returns",
                    "Notifications" };
        } else if (user instanceof SalesOfficer) {
            return new String[] { "Dashboard", "Sellable Stock", "Customers", "Orders", "Payments", "Notifications" };
        } else if (user instanceof WarehouseManager) {
            return new String[] { "Dashboard", "Register Batch", "Stock", "Customers", "Orders", "Payments",
                    "Shipments", "Damage and Returns", "Products", "Ledger", "Reports", "Users", "Notifications" };
        }
        return new String[] { "Dashboard" };
    }

    // ---------------------------------------------------------------
    // The left sidebar: brand, who is logged in, the menu, theme button, log out
    // ---------------------------------------------------------------
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setBackground(Theme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(248, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 14, 18, 14));

        // top: brand mark and name, then the logged-in user
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.add(new Theme.Badge("D", Theme.accent(), 36, false), BorderLayout.WEST);
        JLabel appName = new JLabel("Warehouse");
        appName.setForeground(Color.WHITE);
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 17f));
        brand.add(appName, BorderLayout.CENTER);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        header.add(brand);
        header.add(Box.createVerticalStrut(22));

        JPanel who = new JPanel(new BorderLayout(10, 0));
        who.setOpaque(false);
        String initial = user.getName().substring(0, 1).toUpperCase();
        who.add(new Theme.Badge(initial, new Color(0x3B4756), 34, true), BorderLayout.WEST);
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel userName = new JLabel(user.getName());
        userName.setForeground(Color.WHITE);
        userName.setFont(userName.getFont().deriveFont(Font.BOLD, 13f));
        JLabel roleName = new JLabel(Theme.prettyRole(user.getRole()));
        roleName.setForeground(Theme.SIDEBAR_MUTED);
        roleName.setFont(roleName.getFont().deriveFont(Font.PLAIN, 12f));
        names.add(userName);
        names.add(roleName);
        who.add(names, BorderLayout.CENTER);
        who.setAlignmentX(LEFT_ALIGNMENT);
        header.add(who);
        sidebar.add(header, BorderLayout.NORTH);

        // middle: the menu buttons (in a scroll pane in case the window is short)
        menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        JScrollPane menuScroll = new JScrollPane(menuPanel);
        menuScroll.setBorder(null);
        menuScroll.setOpaque(false);
        menuScroll.getViewport().setOpaque(false);
        menuScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        menuScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(menuScroll, BorderLayout.CENTER);

        // bottom: theme toggle and log out
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        final SidebarButton themeButton = new SidebarButton(Theme.isDark() ? "Light mode" : "Dark mode");
        final SidebarButton logoutButton = new SidebarButton("Log out");
        bottom.add(themeButton);
        bottom.add(Box.createVerticalStrut(2));
        bottom.add(logoutButton);
        sidebar.add(bottom, BorderLayout.SOUTH);

        themeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Theme.install(!Theme.isDark());   // switch the whole application
                themeButton.setText(Theme.isDark() ? "Light mode" : "Dark mode");
                themeButton.setSelected(false);
                FlatLaf.updateUI();               // repaint every open window with the new theme
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

            SidebarButton button = new SidebarButton(name);
            if (name.equals("Notifications")) {
                notificationsButton = button;
            }
            group.add(button);
            menuPanel.add(button);
            menuPanel.add(Box.createVerticalStrut(2));

            // every screen is created once and kept in the CardLayout under its name
            ScreenPanel screen = createScreen(name);
            screen.setSubtitle(subtitleFor(name));
            Theme.applyTo(screen);   // style the buttons of this screen
            screenArea.add(screen, name);

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

    // One plain sentence under each screen title.
    private String subtitleFor(String name) {
        if (name.equals("Dashboard")) {
            return "What needs your attention today.";
        } else if (name.equals("Sellable Stock")) {
            return "Stock that is ready and not reserved.";
        } else if (name.equals("Register Batch")) {
            return "Record a new production run. It starts as curing stock.";
        } else if (name.equals("Stock")) {
            return "Every stock row and its status.";
        } else if (name.equals("Customers")) {
            return "Find a customer or add a new one.";
        } else if (name.equals("Orders")) {
            return "Create orders, reserve stock, then confirm or cancel.";
        } else if (name.equals("Payments")) {
            return "Payments and refunds for one order.";
        } else if (name.equals("Shipments")) {
            return "Reserved stock that is ready to leave the yard.";
        } else if (name.equals("Damage and Returns")) {
            return "Log damaged stock or take goods back from a customer.";
        } else if (name.equals("Products")) {
            return "Curing time and price of each product.";
        } else if (name.equals("Ledger")) {
            return "The full history of every stock change.";
        } else if (name.equals("Reports")) {
            return "Stock, sales and staff activity for a date range.";
        } else if (name.equals("Users")) {
            return "Add staff, reset passwords, and deactivate accounts.";
        } else if (name.equals("Notifications")) {
            return "Messages for you, and messages you send.";
        }
        return " ";
    }

    // ---------------------------------------------------------------
    // Creates the screen that belongs to a menu name. Every screen is a ScreenPanel.
    // ---------------------------------------------------------------
    private ScreenPanel createScreen(String name) {
        if (name.equals("Dashboard")) {
            return new YardOverviewPanel(user);
        } else if (name.equals("Sellable Stock")) {
            return new SellableStockPanel();
        } else if (name.equals("Register Batch")) {
            return new RegisterBatchPanel(user);
        } else if (name.equals("Stock")) {
            return new StockPanel();
        } else if (name.equals("Customers")) {
            return new CustomersPanel();
        } else if (name.equals("Orders")) {
            return new OrdersPanel(user);
        } else if (name.equals("Payments")) {
            return new PaymentsPanel(user);
        } else if (name.equals("Shipments")) {
            return new ShipmentsPanel(user);
        } else if (name.equals("Damage and Returns")) {
            return new DamageReturnsPanel(user);
        } else if (name.equals("Ledger")) {
            return new LedgerPanel();
        } else if (name.equals("Reports")) {
            return new ReportsPanel(user);
        } else if (name.equals("Users")) {
            return new UsersPanel(user);
        } else if (name.equals("Products")) {
            return new ProductsPanel(user);
        } else if (name.equals("Notifications")) {
            return new NotificationsPanel(user);
        }
        return new PlaceholderPanel(name);
    }

    private void showScreen(String name) {
        cards.show(screenArea, name);
        updateUnreadBadge();
        // ask the screen that is now visible to reload its data
        for (java.awt.Component c : screenArea.getComponents()) {
            if (c.isVisible() && c instanceof ScreenPanel) {
                ((ScreenPanel) c).refresh();
            }
        }
    }
}
