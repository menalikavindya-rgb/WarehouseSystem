package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

// A card with a title and a list of horizontal bars. Drawn by hand, so no extra library is needed.
public class BarChartPanel extends Theme.Card {

    private static final int ROW_HEIGHT = 30;

    private Bars bars = new Bars();

    public BarChartPanel(String title) {
        super(new BorderLayout(0, 12));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel, BorderLayout.NORTH);
        add(bars, BorderLayout.CENTER);
    }

    // labels, values and colours must have the same length. A null colour means "use the accent".
    public void setData(List<String> labels, List<Double> values, List<Color> colors) {
        bars.labels = new ArrayList<String>(labels);
        bars.values = new ArrayList<Double>(values);
        bars.colors = new ArrayList<Color>(colors);
        bars.setPreferredSize(new Dimension(100, Math.max(1, labels.size()) * ROW_HEIGHT));
        bars.revalidate();
        bars.repaint();
    }

    // "PartiallyShipped" -> "Partially Shipped"
    public static String spaced(String text) {
        return text.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private static class Bars extends JPanel {
        List<String> labels = new ArrayList<String>();
        List<Double> values = new ArrayList<Double>();
        List<Color> colors = new ArrayList<Color>();

        Bars() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, ROW_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font font = UIManager.getFont("Label.font");
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            Color text = UIManager.getColor("Label.foreground");
            Color muted = UIManager.getColor("Label.disabledForeground");
            if (text == null) {
                text = Color.GRAY;
            }
            if (muted == null) {
                muted = Color.GRAY;
            }

            if (labels.isEmpty()) {
                g2.setColor(muted);
                g2.drawString("Nothing to show yet.", 0, ROW_HEIGHT / 2 + fm.getAscent() / 2);
                g2.dispose();
                return;
            }

            double max = 1;
            int labelWidth = 0;
            int valueWidth = 0;
            for (int i = 0; i < labels.size(); i++) {
                max = Math.max(max, values.get(i));
                labelWidth = Math.max(labelWidth, fm.stringWidth(labels.get(i)));
                valueWidth = Math.max(valueWidth, fm.stringWidth(String.format("%,.0f", values.get(i))));
            }
            labelWidth = Math.min(labelWidth, 170);

            int barLeft = labelWidth + 14;
            int barRight = getWidth() - valueWidth - 14;
            int barWidth = Math.max(20, barRight - barLeft);
            int barHeight = 10;
            Color track = new Color(muted.getRed(), muted.getGreen(), muted.getBlue(), 40);

            for (int i = 0; i < labels.size(); i++) {
                int top = i * ROW_HEIGHT;
                int centre = top + ROW_HEIGHT / 2;
                int textY = centre + fm.getAscent() / 2 - 1;

                g2.setColor(text);
                g2.drawString(labels.get(i), 0, textY);

                g2.setColor(track);
                g2.fillRoundRect(barLeft, centre - barHeight / 2, barWidth, barHeight, barHeight, barHeight);

                int filled = (int) Math.round(barWidth * (values.get(i) / max));
                if (values.get(i) > 0) {
                    filled = Math.max(filled, barHeight);   // a tiny value is still visible
                }
                Color colour = colors.get(i) == null ? Theme.accent() : colors.get(i);
                g2.setColor(colour);
                g2.fillRoundRect(barLeft, centre - barHeight / 2, filled, barHeight, barHeight, barHeight);

                String value = String.format("%,.0f", values.get(i));
                g2.setColor(text);
                g2.drawString(value, getWidth() - fm.stringWidth(value), textY);
            }
            g2.dispose();
        }
    }
}
