import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatDarkLaf;
import ui.LoginFrame;

public class Main {

    public static void main(String[] args) {
        FlatDarkLaf.setup();   // the dark theme for every window

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}