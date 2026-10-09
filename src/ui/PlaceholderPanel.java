package ui;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

// A temporary screen. We replace it with the real screen, one at a time.
public class PlaceholderPanel extends ScreenPanel {

    public PlaceholderPanel(String title) {
        super(title);
        add(new JLabel("The \"" + title + "\" screen will be built here.", SwingConstants.CENTER),
                BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        // nothing to reload yet
    }
}