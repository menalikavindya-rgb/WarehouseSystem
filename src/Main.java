import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;
import ui.LoginFrame;

public class Main {

    public static void main(String[] args) {
        FlatLightLaf.setup();   // gives every window FlatLaf's modern look

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}