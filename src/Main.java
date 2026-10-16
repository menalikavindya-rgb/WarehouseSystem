import javax.swing.SwingUtilities;
import ui.LoginFrame;
import ui.Theme;

public class Main {

    public static void main(String[] args) {
        Theme.install(true);   // true = dark theme, false = light theme

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}
