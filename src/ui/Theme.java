package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.RowSorter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

// All the colours, fonts and small helpers that make the whole app look the same.
// Change a colour here and every screen changes with it.
public class Theme {

    // The sidebar is dark in both the light and the dark theme.
    public static final Color SIDEBAR_BG = new Color(0x1B222B);
    public static final Color SIDEBAR_TEXT = new Color(0xC3CBD5);
    public static final Color SIDEBAR_HOVER = new Color(0x27313D);
    public static final Color SIDEBAR_MUTED = new Color(0x7D8896);

    // Status colours: the same word always has the same colour, on every screen.
    private static final Map<String, Color> STATUS_COLORS = new HashMap<String, Color>();
    static {
        Color amber = new Color(0xD97706);
        Color green = new Color(0x16A34A);
        Color blue = new Color(0x2563EB);
        Color violet = new Color(0x7C3AED);
        Color slate = new Color(0x64748B);
        Color teal = new Color(0x0D9488);
        Color red = new Color(0xDC2626);

        STATUS_COLORS.put("Curing", amber);
        STATUS_COLORS.put("Pending", amber);
        STATUS_COLORS.put("Available", green);
        STATUS_COLORS.put("Ready", green);
        STATUS_COLORS.put("Fulfilled", green);
        STATUS_COLORS.put("Active", green);
        STATUS_COLORS.put("Reserved", blue);
        STATUS_COLORS.put("Confirmed", blue);
        STATUS_COLORS.put("PartiallyShipped", violet);
        STATUS_COLORS.put("Shipped", teal);
        STATUS_COLORS.put("Depleted", slate);
        STATUS_COLORS.put("Damaged", red);
        STATUS_COLORS.put("Cancelled", red);
        STATUS_COLORS.put("UNREAD", amber);
        STATUS_COLORS.put("Read", slate);
    }

    // Buttons with these captions are the main action of a screen and get the accent colour.
    private static final Set<String> PRIMARY_CAPTIONS = new HashSet<String>();
    static {
        String[] names = { "Log in", "Register batch", "Add customer", "New order", "Reserve stock",
                "Confirm order", "Record payment", "Issue refund", "Ship selected reservation",
                "Log damage", "Process return", "Generate", "Add user", "Add product",
                "Save curing days", "Send", "Load", "Search", "Show" };
        for (String n : names) {
            PRIMARY_CAPTIONS.add(n);
        }
    }

    private static boolean dark = true;

    public static boolean isDark() {
        return dark;
    }

    public static Color accent() {
        return dark ? new Color(0x0891B2) : new Color(0x0E7490);
    }

    public static Color statusColor(String text) {
        return STATUS_COLORS.get(text);
    }

    // ---------------------------------------------------------------
    // Switches the whole application to the light or the dark theme.
    // ---------------------------------------------------------------
    public static void install(boolean useDark) {
        dark = useDark;

        Map<String, String> extra = new HashMap<String, String>();
        extra.put("@accentColor", dark ? "#0891B2" : "#0E7490");
        extra.put("@background", dark ? "#171A1F" : "#F3F5F7");
        FlatLaf.setGlobalExtraDefaults(extra);

        // These are set before the look and feel starts, so they apply to every component.
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("TabbedPane.tabType", "underlined");
        UIManager.put("TabbedPane.tabHeight", 40);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TableHeader.height", 38);
        UIManager.put("ScrollPane.arc", 16);   // soft, rounded corners around every table
        UIManager.put("TableHeader.separatorColor", new Color(0, 0, 0, 0));
        UIManager.put("Table.selectionBackground", dark ? new Color(0x0E4F63) : new Color(0xD3EEF5));
        UIManager.put("Table.selectionForeground", dark ? Color.WHITE : new Color(0x0B3A47));
        UIManager.put("Table.selectionInactiveBackground", dark ? new Color(0x23424C) : new Color(0xE3F3F8));
        UIManager.put("Table.selectionInactiveForeground", dark ? Color.WHITE : new Color(0x0B3A47));

        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    // ---------------------------------------------------------------
    // Styles the buttons found anywhere inside a container (used once per screen).
    // ---------------------------------------------------------------
    public static void applyTo(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton && !(c instanceof SidebarButton)) {
                styleButton((JButton) c);
            }
            if (c instanceof Container) {
                applyTo((Container) c);
            }
        }
    }

    public static void styleButton(AbstractButton b) {
        String text = b.getText();
        if (PRIMARY_CAPTIONS.contains(text)) {
            stylePrimary(b);
        } else if (text.equals("Cancel order")) {
            b.putClientProperty(FlatClientProperties.STYLE,
                    "foreground:#E5484D; borderColor:#E5484D; focusedBorderColor:#E5484D; hoverBorderColor:#E5484D");
        }
    }

    public static void stylePrimary(AbstractButton b) {
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:@accentColor; foreground:#FFFFFF; borderWidth:0; focusWidth:0; "
                + "default.background:@accentColor; default.foreground:#FFFFFF; default.borderWidth:0; default.focusedBackground:@accentColor; "
                + "hoverBackground:lighten(@accentColor,6%); pressedBackground:darken(@accentColor,6%)");
        b.setFont(b.getFont().deriveFont(Font.BOLD));
    }

    // "WarehouseManager" -> "Warehouse manager"
    public static String prettyRole(String role) {
        String spaced = role.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.substring(0, 1) + spaced.substring(1).toLowerCase();
    }

    // ---------------------------------------------------------------
    // Tables: a renderer that draws status words as coloured pills and
    // right-aligns numbers, and a helper that sizes the columns to fit.
    // ---------------------------------------------------------------
    public static void styleTable(JTable table) {
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
        table.setShowVerticalLines(false);
    }

    // Widest a column may grow to when it is sized to its content.
    private static final int MAX_COLUMN_WIDTH = 650;

    public static void fitColumns(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        rememberUserWidths(table);

        // widths the user dragged earlier (kept across refreshes)
        @SuppressWarnings("unchecked")
        Map<String, Integer> saved = (Map<String, Integer>) table.getClientProperty("userWidths");

        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);

            // the title sits on the same side as the values below it
            boolean numeric = table.getRowCount() > 0 && looksNumeric(table.getValueAt(0, col));
            column.setHeaderRenderer(new HeaderRenderer(numeric));

            String name = String.valueOf(column.getHeaderValue());
            if (saved != null && saved.containsKey(name)) {
                column.setPreferredWidth(saved.get(name));
                continue;
            }

            Component header = column.getHeaderRenderer().getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, -1, col);
            int width = header.getPreferredSize().width;

            int rows = Math.min(table.getRowCount(), 120);
            for (int row = 0; row < rows; row++) {
                Component cell = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                width = Math.max(width, cell.getPreferredSize().width);
            }
            column.setPreferredWidth(Math.max(80, Math.min(width + 24, MAX_COLUMN_WIDTH)));
        }
        useNumberAwareSorting(table);
    }

    // When the user drags a column edge, remember that width for the next refresh.
    private static void rememberUserWidths(final JTable table) {
        if (table.getClientProperty("widthListener") != null) {
            return;
        }
        table.putClientProperty("widthListener", Boolean.TRUE);
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                TableColumn dragged = table.getTableHeader().getResizingColumn();
                if (dragged == null) {
                    return;   // the table resized itself, not the user
                }
                @SuppressWarnings("unchecked")
                Map<String, Integer> saved = (Map<String, Integer>) table.getClientProperty("userWidths");
                if (saved == null) {
                    saved = new HashMap<String, Integer>();
                    table.putClientProperty("userWidths", saved);
                }
                saved.put(String.valueOf(dragged.getHeaderValue()), dragged.getWidth());
            }
            public void columnAdded(TableColumnModelEvent e) { }
            public void columnRemoved(TableColumnModelEvent e) { }
            public void columnMoved(TableColumnModelEvent e) { }
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) { }
        });
    }

    // ---------------------------------------------------------------
    // Sorting: numbers sort as numbers (2 before 10), everything else as text.
    // ---------------------------------------------------------------
    private static final Comparator<Object> NUMBER_AWARE = new Comparator<Object>() {
        public int compare(Object a, Object b) {
            Double x = toNumber(a);
            Double y = toNumber(b);
            if (x != null && y != null) {
                return Double.compare(x, y);
            }
            if (x != null) {
                return -1;      // numbers before words
            }
            if (y != null) {
                return 1;
            }
            String s1 = (a == null) ? "" : a.toString();
            String s2 = (b == null) ? "" : b.toString();
            return s1.compareToIgnoreCase(s2);
        }
    };

    private static Double toNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = value.toString().trim().replace(",", "");   // "16,960.00" -> "16960.00"
        if (!text.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        return Double.valueOf(text);
    }

    @SuppressWarnings("unchecked")
    public static void useNumberAwareSorting(JTable table) {
        if (table.getRowSorter() instanceof TableRowSorter) {
            TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
            for (int col = 0; col < table.getColumnCount(); col++) {
                sorter.setComparator(col, NUMBER_AWARE);
            }
        }
    }

    // ---------------------------------------------------------------
    // A table that scrolls sideways when its columns are wider than the window,
    // and shows the full text of a cell in a tooltip when the cell is cut off.
    // ---------------------------------------------------------------
    public static class SmartTable extends JTable {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() instanceof JViewport) {
                return getPreferredSize().width < getParent().getWidth();
            }
            return super.getScrollableTracksViewportWidth();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            Point p = event.getPoint();
            int row = rowAtPoint(p);
            int col = columnAtPoint(p);
            if (row < 0 || col < 0) {
                return null;
            }
            Object value = getValueAt(row, col);
            if (value == null) {
                return null;
            }
            Component cell = prepareRenderer(getCellRenderer(row, col), row, col);
            Rectangle area = getCellRect(row, col, false);
            if (cell.getPreferredSize().width > area.width) {
                return value.toString();   // only when the text is cut off
            }
            return null;
        }
    }

    private static boolean looksNumeric(Object value) {
        if (value == null) {
            return false;
        }
        return (value instanceof Number) || value.toString().matches("-?[\\d,]+(\\.\\d+)?");
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        private boolean numeric;

        HeaderRenderer(boolean numeric) {
            this.numeric = numeric;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setFont(getFont().deriveFont(Font.BOLD, 12.5f));
            setOpaque(true);
            Color bg = UIManager.getColor("TableHeader.background");
            Color fg = UIManager.getColor("TableHeader.foreground");
            Color line = UIManager.getColor("TableHeader.bottomSeparatorColor");
            if (bg != null) {
                setBackground(bg);
            }
            if (fg != null) {
                setForeground(fg);
            }
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, line != null ? line : Color.GRAY),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    private static class CellRenderer extends DefaultTableCellRenderer {
        private Color pill = null;
        private String pillText = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            String text = (value == null) ? "" : value.toString();
            String header = table.getColumnName(column).toLowerCase();
            if ((value instanceof Integer || value instanceof Long)
                    && (header.contains("units") || header.equals("stock rows") || header.equals("change"))) {
                text = String.format("%,d", ((Number) value).longValue());   // 16960 -> 16,960
            }
            super.getTableCellRendererComponent(table, text, isSelected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

            pill = STATUS_COLORS.get(text);
            pillText = text;

            boolean numeric = (value instanceof Number) || text.matches("-?[\\d,]+(\\.\\d+)?");
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            if (pill != null) {
                d.width += 20;
            }
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (pill == null) {
                super.paintComponent(g);
                return;
            }
            // draw the cell background, then a rounded pill with the status word inside it
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            Font font = getFont().deriveFont(Font.BOLD, 12f);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(pillText);
            int pillWidth = textWidth + 20;
            int pillHeight = fm.getHeight() + 6;
            int x = 12;
            int y = (getHeight() - pillHeight) / 2;

            g2.setColor(new Color(pill.getRed(), pill.getGreen(), pill.getBlue(), dark ? 60 : 36));
            g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight);
            g2.setColor(dark ? pill.brighter() : pill.darker());
            g2.drawString(pillText, x + 10, y + 3 + fm.getAscent());
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------
    // Small building blocks
    // ---------------------------------------------------------------

    // A label in the muted colour of the current theme (for captions and subtitles).
    public static class MutedLabel extends JLabel {
        public MutedLabel(String text) {
            super(text);
        }

        @Override
        public Color getForeground() {
            Color c = UIManager.getColor("Label.disabledForeground");
            return (c != null) ? c : Color.GRAY;
        }
    }

    // A rounded square or circle with a letter in it (the brand mark and the user's avatar).
    public static class Badge extends JComponent {
        private String letter;
        private Color color;
        private boolean circle;
        private int size;

        public Badge(String letter, Color color, int size, boolean circle) {
            this.letter = letter;
            this.color = color;
            this.size = size;
            this.circle = circle;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(color);
            int arc = circle ? size : size / 3;
            g2.fillRoundRect(0, 0, size, size, arc, arc);
            g2.setColor(Color.WHITE);
            Font base = (getFont() != null) ? getFont() : UIManager.getFont("Label.font");
            g2.setFont(base.deriveFont(Font.BOLD, size * 0.45f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(letter, (size - fm.stringWidth(letter)) / 2,
                    (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // A rounded panel that stands out a little from the page (used for the overview cards).
    public static class Card extends JPanel {
        public Card(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dark ? new Color(0x1F242B) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(dark ? new Color(0x2C343D) : new Color(0xE2E6EA));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
