package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

// A menu entry in the dark sidebar. It paints its own rounded background:
// the accent colour when it is the open screen, a soft grey when the mouse is over it.
public class SidebarButton extends JToggleButton {

    public SidebarButton(String text) {
        super(text);
        setFocusable(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        setFont(getFont().deriveFont(Font.PLAIN, 13.5f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(100, 36));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected()) {
            g2.setColor(Theme.accent());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            setForeground(Color.WHITE);
        } else if (getModel().isRollover()) {
            g2.setColor(Theme.SIDEBAR_HOVER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            setForeground(Color.WHITE);
        } else {
            setForeground(Theme.SIDEBAR_TEXT);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
